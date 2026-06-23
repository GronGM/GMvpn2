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
    if (url.isBlank()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.EmptyInput)
    }

    val body = try {
        fetchBody(url)
    } catch (e: SubscriptionFetchException) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.FetchFailed, e)
    } catch (e: Exception) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.FetchFailed, e)
    }

    if (body.isEmpty()) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.NoProfilesFound)
    }

    val decoded = try {
        decodeUris(body, format)
    } catch (e: GmvpnException) {
        throw classifyDecodeFailure(e)
    } catch (e: IllegalArgumentException) {
        throw SubscriptionImportException(SubscriptionImportFailureCategory.UnsupportedFormat, e)
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

fun subscriptionSaveFailure(cause: Throwable): SubscriptionImportException =
    SubscriptionImportException(SubscriptionImportFailureCategory.SaveFailed, cause)

private fun classifyDecodeFailure(error: Throwable): SubscriptionImportException {
    val text = error.message.orEmpty().lowercase()
    val category = when {
        "unsupported" in text || "format" in text || "base64" in text ||
            "sip008" in text || "malformed" in text ->
            SubscriptionImportFailureCategory.UnsupportedFormat
        else -> SubscriptionImportFailureCategory.ParseFailed
    }
    return SubscriptionImportException(category, error)
}
