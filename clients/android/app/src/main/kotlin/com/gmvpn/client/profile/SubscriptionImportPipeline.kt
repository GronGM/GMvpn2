package com.gmvpn.client.profile

import uniffi.gmvpn_ffi.FfiSubscriptionFormat
import uniffi.gmvpn_ffi.GmvpnException

enum class SubscriptionImportFailureCategory {
    EmptyInput,
    FetchFailed,
    UnsupportedFormat,
    ParseFailed,
    NoProfilesFound,
    SaveFailed,
    Unknown,
}

class SubscriptionImportException(
    val category: SubscriptionImportFailureCategory,
    val fetchDiagnostics: SubscriptionFetchDiagnostics? = null,
    cause: Throwable? = null,
) : RuntimeException(category.name, cause)

data class SubscriptionDecodeOutput(
    val uris: List<String>,
    val warningCount: Int,
)

data class PreparedSubscriptionImport(
    val profiles: List<ProfileImportPreview>,
    val warnings: Int,
    val duplicateUris: Int,
)

suspend fun prepareSubscriptionImport(
    url: String,
    format: FfiSubscriptionFormat,
    fetchBody: suspend (String) -> ByteArray,
    decodeUris: (ByteArray, FfiSubscriptionFormat) -> SubscriptionDecodeOutput,
): PreparedSubscriptionImport {
    val normalizedUrl = sanitizeSubscriptionUrlInput(url)
    if (normalizedUrl.isBlank()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.EmptyInput)
    }

    val body = try {
        fetchBody(normalizedUrl)
    } catch (e: SubscriptionFetchException) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.FetchFailed,
            fetchDiagnostics = e.diagnostics,
            cause = e,
        )
    } catch (e: Exception) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.FetchFailed,
            fetchDiagnostics = SubscriptionFetchDiagnostics.fromInput(normalizedUrl).withException(e),
            cause = e,
        )
    }

    if (body.isEmpty()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.NoProfilesFound)
    }

    val decoded = try {
        decodeUris(body, format)
    } catch (e: GmvpnException) {
        throw classifyDecodeFailure(e)
    } catch (e: IllegalArgumentException) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.UnsupportedFormat,
            cause = e,
        )
    } catch (e: Exception) {
        throw classifyDecodeFailure(e)
    }

    if (decoded.uris.isEmpty()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.NoProfilesFound)
    }

    val plan = buildProfileImportPlan(decoded.uris)
    if (plan.profiles.isEmpty()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.NoProfilesFound)
    }

    return PreparedSubscriptionImport(
        profiles = plan.profiles,
        warnings = decoded.warningCount,
        duplicateUris = plan.duplicateUriCount,
    )
}

fun subscriptionImportFailureCategory(
    error: Throwable,
): SubscriptionImportFailureCategory =
    generateSequence(error) { it.cause }
        .filterIsInstance<SubscriptionImportException>()
        .firstOrNull()
        ?.category
        ?: SubscriptionImportFailureCategory.Unknown

fun subscriptionImportFetchDiagnostics(error: Throwable): SubscriptionFetchDiagnostics? =
    generateSequence(error) { it.cause }
        .filterIsInstance<SubscriptionImportException>()
        .firstOrNull()
        ?.fetchDiagnostics
        ?: generateSequence(error) { it.cause }
            .filterIsInstance<SubscriptionFetchException>()
            .firstOrNull()
            ?.diagnostics

fun subscriptionSaveFailure(cause: Throwable): SubscriptionImportException =
    SubscriptionImportException(
        category = SubscriptionImportFailureCategory.SaveFailed,
        cause = cause,
    )

private fun classifyDecodeFailure(error: Throwable): SubscriptionImportException {
    val text = error.message.orEmpty().lowercase()
    val category = when {
        "unsupported" in text || "format" in text || "base64" in text ||
            "sip008" in text || "malformed" in text ->
            SubscriptionImportFailureCategory.UnsupportedFormat
        else -> SubscriptionImportFailureCategory.ParseFailed
    }
    return SubscriptionImportException(
        category = category,
        cause = error,
    )
}
