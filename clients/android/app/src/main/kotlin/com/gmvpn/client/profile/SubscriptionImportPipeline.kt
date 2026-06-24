package com.gmvpn.client.profile

import uniffi.gmvpn_ffi.FfiSubscriptionFormat
import uniffi.gmvpn_ffi.GmvpnException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

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
    val importStage: String = category.defaultImportStage(),
    val failureOrigin: String = category.defaultFailureOrigin(),
    val throwableKind: String = cause?.let(::throwableKindOf) ?: "subscription_import_exception",
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
    val inputDiagnostics = SubscriptionFetchDiagnostics.fromInput(normalizedUrl)
    if (normalizedUrl.isBlank()) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.EmptyInput,
            fetchDiagnostics = inputDiagnostics,
        )
    }

    val body = try {
        fetchBody(normalizedUrl)
    } catch (e: SubscriptionFetchException) {
        throw wrapSubscriptionImportFailure(
            stage = "fetch_failed",
            origin = "fetch",
            inputDiagnostics = inputDiagnostics,
            throwable = e,
        )
    } catch (e: Throwable) {
        throw wrapSubscriptionImportFailure(
            stage = "fetch_failed",
            origin = "fetch",
            inputDiagnostics = inputDiagnostics,
            throwable = e,
        )
    }

    if (body.isEmpty()) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.NoProfilesFound,
            fetchDiagnostics = inputDiagnostics,
        )
    }

    val decoded = try {
        decodeUris(body, format)
    } catch (e: GmvpnException) {
        throw classifyDecodeFailure(e, inputDiagnostics)
    } catch (e: IllegalArgumentException) {
        throw wrapSubscriptionImportFailure(
            stage = "decode_failed",
            origin = "decode",
            inputDiagnostics = inputDiagnostics,
            throwable = e,
        )
    } catch (e: Throwable) {
        throw wrapSubscriptionImportFailure(
            stage = "decode_failed",
            origin = "decode",
            inputDiagnostics = inputDiagnostics,
            throwable = e,
        )
    }

    if (decoded.uris.isEmpty()) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.NoProfilesFound,
            fetchDiagnostics = inputDiagnostics,
        )
    }

    val plan = buildProfileImportPlan(decoded.uris)
    if (plan.profiles.isEmpty()) {
        throw SubscriptionImportException(
            category = SubscriptionImportFailureCategory.NoProfilesFound,
            fetchDiagnostics = inputDiagnostics,
        )
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
    error.findSubscriptionImportException()
        ?.category
        ?: if (error.findSubscriptionFetchException() != null) {
            SubscriptionImportFailureCategory.FetchFailed
        } else {
            SubscriptionImportFailureCategory.Unknown
        }

fun subscriptionImportStage(error: Throwable): String =
    error.findSubscriptionImportException()
        ?.importStage
        ?: subscriptionImportFailureCategory(error).defaultImportStage(error)

fun subscriptionImportFailureOrigin(error: Throwable): String =
    error.findSubscriptionImportException()
        ?.failureOrigin
        ?: subscriptionImportFailureCategory(error).defaultFailureOrigin(error)

fun subscriptionImportThrowableKind(error: Throwable): String =
    error.findSubscriptionImportException()
        ?.throwableKind
        ?: throwableKindOf(error)

fun subscriptionImportHasTypedCause(error: Throwable): Boolean =
    error.causeChain().any {
        it is SubscriptionImportException || it is SubscriptionFetchException
    }

fun subscriptionImportFetchDiagnostics(error: Throwable): SubscriptionFetchDiagnostics? =
    error.findSubscriptionImportException()
        ?.fetchDiagnostics
        ?: error.findSubscriptionFetchException()
            ?.diagnostics

fun subscriptionSaveFailure(cause: Throwable): SubscriptionImportException =
    wrapSubscriptionImportFailure(
        stage = "save_failed",
        origin = "save",
        inputDiagnostics = null,
        throwable = cause,
    )

fun wrapSubscriptionImportFailure(
    stage: String,
    origin: String,
    inputDiagnostics: SubscriptionFetchDiagnostics?,
    throwable: Throwable,
): SubscriptionImportException {
    throwable.findSubscriptionImportException()?.let { return it }

    throwable.findSubscriptionFetchException()?.let { fetchError ->
        return SubscriptionImportException(
            category = SubscriptionImportFailureCategory.FetchFailed,
            importStage = "fetch_failed",
            failureOrigin = "fetch",
            throwableKind = throwableKindOf(throwable),
            fetchDiagnostics = fetchError.diagnostics
                .takeUnless { it == SubscriptionFetchDiagnostics.Unknown }
                ?: inputDiagnostics?.withException(throwable),
            cause = throwable,
        )
    }

    return when (origin) {
        "fetch" -> SubscriptionImportException(
            category = SubscriptionImportFailureCategory.FetchFailed,
            importStage = "fetch_failed",
            failureOrigin = "fetch",
            throwableKind = throwableKindOf(throwable),
            fetchDiagnostics = inputDiagnostics?.withException(throwable),
            cause = throwable,
        )
        "decode" -> classifyDecodeFailure(throwable, inputDiagnostics)
        "save" -> SubscriptionImportException(
            category = SubscriptionImportFailureCategory.SaveFailed,
            importStage = "save_failed",
            failureOrigin = "save",
            throwableKind = throwableKindOf(throwable),
            fetchDiagnostics = inputDiagnostics,
            cause = throwable,
        )
        "input" -> SubscriptionImportException(
            category = if (throwable is IllegalArgumentException) {
                SubscriptionImportFailureCategory.UnsupportedFormat
            } else {
                SubscriptionImportFailureCategory.EmptyInput
            },
            importStage = stage,
            failureOrigin = "input",
            throwableKind = throwableKindOf(throwable),
            fetchDiagnostics = inputDiagnostics,
            cause = throwable,
        )
        else -> SubscriptionImportException(
            category = SubscriptionImportFailureCategory.Unknown,
            importStage = stage,
            failureOrigin = origin,
            throwableKind = throwableKindOf(throwable),
            fetchDiagnostics = inputDiagnostics,
            cause = throwable,
        )
    }
}

fun throwableKindOf(error: Throwable): String =
    when {
        error.causeChain().any { it is UnknownHostException } -> "unknown_host_exception"
        error.causeChain().any { it is SocketTimeoutException } -> "timeout_exception"
        error.causeChain().any { it is SSLException } -> "ssl_exception"
        error.causeChain().any { it is IOException } -> "io_exception"
        error.causeChain().any { it is IllegalArgumentException } -> "illegal_argument_exception"
        error.causeChain().any { it is SecurityException } -> "security_exception"
        error.causeChain().any { it is CancellationException } -> "cancellation_exception"
        error.causeChain().any { it is SubscriptionFetchException } -> "subscription_fetch_exception"
        error.causeChain().any { it is SubscriptionImportException } -> "subscription_import_exception"
        error.causeChain().any { it is GmvpnException || it.javaClass.name.contains("uniffi", ignoreCase = true) } ->
            "uniffi_exception"
        else -> "unknown_exception"
    }

private fun classifyDecodeFailure(
    error: Throwable,
    inputDiagnostics: SubscriptionFetchDiagnostics?,
): SubscriptionImportException {
    val text = error.message.orEmpty().lowercase()
    val category = when {
        "unsupported" in text || "format" in text || "base64" in text ||
            "sip008" in text || "malformed" in text ->
            SubscriptionImportFailureCategory.UnsupportedFormat
        else -> SubscriptionImportFailureCategory.ParseFailed
    }
    return SubscriptionImportException(
        category = category,
        importStage = "decode_failed",
        failureOrigin = "decode",
        throwableKind = throwableKindOf(error),
        fetchDiagnostics = inputDiagnostics,
        cause = error,
    )
}

private fun SubscriptionImportFailureCategory.defaultImportStage(
    error: Throwable? = null,
): String =
    when (this) {
        SubscriptionImportFailureCategory.EmptyInput -> "input_normalized"
        SubscriptionImportFailureCategory.FetchFailed -> "fetch_failed"
        SubscriptionImportFailureCategory.UnsupportedFormat,
        SubscriptionImportFailureCategory.ParseFailed,
        SubscriptionImportFailureCategory.NoProfilesFound -> "decode_failed"
        SubscriptionImportFailureCategory.SaveFailed -> "save_failed"
        SubscriptionImportFailureCategory.Unknown -> if (error == null) {
            "unknown"
        } else {
            "ui_failure_catch"
        }
    }

private fun SubscriptionImportFailureCategory.defaultFailureOrigin(
    error: Throwable? = null,
): String =
    when (this) {
        SubscriptionImportFailureCategory.EmptyInput -> "input"
        SubscriptionImportFailureCategory.FetchFailed -> "fetch"
        SubscriptionImportFailureCategory.UnsupportedFormat,
        SubscriptionImportFailureCategory.ParseFailed,
        SubscriptionImportFailureCategory.NoProfilesFound -> "decode"
        SubscriptionImportFailureCategory.SaveFailed -> "save"
        SubscriptionImportFailureCategory.Unknown -> if (error == null) {
            "unknown"
        } else {
            "ui"
        }
    }

private fun Throwable.findSubscriptionImportException(): SubscriptionImportException? =
    causeChain()
        .filterIsInstance<SubscriptionImportException>()
        .firstOrNull()

private fun Throwable.findSubscriptionFetchException(): SubscriptionFetchException? =
    causeChain()
        .filterIsInstance<SubscriptionFetchException>()
        .firstOrNull()

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { it.cause }
