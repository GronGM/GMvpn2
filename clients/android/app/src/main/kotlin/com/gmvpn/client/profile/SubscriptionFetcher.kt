package com.gmvpn.client.profile

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches a subscription URL into a byte body. HTTPS-only by default
 * (per `docs/memory/project-context.md` -- "no silent network calls"
 * and the privacy promise). One-shot, no caching, no redirects to
 * non-HTTPS schemes.
 */
class SubscriptionFetcher(
    private val maxBodyBytes: Int = MAX_BODY_BYTES,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) {

    /**
     * Returns the body bytes. Throws [SubscriptionFetchException] on
     * any non-2xx status, network failure, or non-HTTPS URL.
     */
    suspend fun fetch(url: String, userAgent: String? = DEFAULT_UA): ByteArray =
        withContext(Dispatchers.IO) {
            val normalizedUrl = sanitizeSubscriptionUrlInput(url)
            var diagnostics = SubscriptionFetchDiagnostics.fromInput(normalizedUrl)
            val parsed = try {
                URL(normalizedUrl)
            } catch (e: Exception) {
                throw SubscriptionFetchException(
                    message = "invalid HTTPS subscription URL",
                    diagnostics = diagnostics.withException(e),
                )
            }
            if (!parsed.protocol.equals("https", ignoreCase = true)) {
                diagnostics = diagnostics.copy(
                    cleartextBlockedLikely = if (parsed.protocol.equals("http", ignoreCase = true)) {
                        SubscriptionDiagnosticTriState.Yes
                    } else {
                        SubscriptionDiagnosticTriState.No
                    },
                )
                throw SubscriptionFetchException(
                    message = "only https:// URLs are accepted",
                    diagnostics = diagnostics,
                )
            }

            val conn = parsed.openConnection() as HttpsURLConnection
            try {
                conn.connectTimeout = connectTimeoutMs
                conn.readTimeout = readTimeoutMs
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = false
                userAgent?.let { conn.setRequestProperty("User-Agent", it) }
                conn.setRequestProperty("Accept", "text/plain, application/json, */*")

                val code = conn.responseCode
                diagnostics = diagnostics.withHttpStatus(code)
                if (code in 300..399) {
                    throw SubscriptionFetchException(
                        message = "redirect refused; point at the final HTTPS URL",
                        diagnostics = diagnostics.copy(
                            redirectObserved = SubscriptionDiagnosticTriState.Yes,
                        ),
                    )
                }
                if (code !in 200..299) {
                    throw SubscriptionFetchException(
                        message = "HTTP status from subscription endpoint",
                        diagnostics = diagnostics,
                    )
                }

                val stream = conn.inputStream
                    ?: throw SubscriptionFetchException(
                        message = "empty response body",
                        diagnostics = diagnostics.copy(
                            bodyLengthBucket = SubscriptionBodyLengthBucket.Empty,
                        ),
                    )
                val out = ByteArray(maxBodyBytes + 1)
                var read = 0
                while (read < out.size) {
                    val n = stream.read(out, read, out.size - read)
                    if (n <= 0) break
                    read += n
                }
                if (read > maxBodyBytes) {
                    throw SubscriptionFetchException(
                        message = "subscription body exceeds maximum size",
                        diagnostics = diagnostics.copy(
                            bodyLengthBucket = SubscriptionBodyLengthBucket.Large,
                        ),
                    )
                }
                out.copyOfRange(0, read)
            } catch (e: SubscriptionFetchException) {
                throw e
            } catch (e: IOException) {
                throw SubscriptionFetchException(
                    message = "network error while fetching subscription",
                    diagnostics = diagnostics.withException(e),
                    cause = e,
                )
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        private const val MAX_BODY_BYTES = 2 * 1024 * 1024 // 2 MiB
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val DEFAULT_UA = "GMvpn/0.0"
    }
}

class SubscriptionFetchException(
    message: String,
    val diagnostics: SubscriptionFetchDiagnostics = SubscriptionFetchDiagnostics.Unknown,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

enum class SubscriptionUrlScheme(val safeValue: String) {
    Http("http"),
    Https("https"),
    Other("other"),
    Missing("missing"),
}

enum class SubscriptionDiagnosticTriState(val safeValue: String) {
    Yes("yes"),
    No("no"),
    Unknown("unknown"),
}

enum class SubscriptionInputLengthBucket(val safeValue: String) {
    Empty("empty"),
    Short("short"),
    Medium("medium"),
    Long("long"),
    VeryLong("veryLong"),
}

enum class SubscriptionHttpStatusClass(val safeValue: String) {
    Status2xx("2xx"),
    Status3xx("3xx"),
    Status4xx("4xx"),
    Status5xx("5xx"),
    Unknown("unknown"),
}

enum class SubscriptionBodyLengthBucket(val safeValue: String) {
    Empty("empty"),
    Small("small"),
    Medium("medium"),
    Large("large"),
    Unknown("unknown"),
}

data class SubscriptionFetchDiagnostics(
    val urlScheme: SubscriptionUrlScheme,
    val hasQuery: Boolean,
    val hasFragment: Boolean,
    val inputLengthBucket: SubscriptionInputLengthBucket,
    val networkAvailable: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val cleartextBlockedLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val httpStatusClass: SubscriptionHttpStatusClass = SubscriptionHttpStatusClass.Unknown,
    val redirectObserved: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val tlsFailureLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val timeoutLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val dnsFailureLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val bodyLengthBucket: SubscriptionBodyLengthBucket = SubscriptionBodyLengthBucket.Unknown,
) {
    fun withHttpStatus(code: Int): SubscriptionFetchDiagnostics =
        copy(
            httpStatusClass = when (code) {
                in 200..299 -> SubscriptionHttpStatusClass.Status2xx
                in 300..399 -> SubscriptionHttpStatusClass.Status3xx
                in 400..499 -> SubscriptionHttpStatusClass.Status4xx
                in 500..599 -> SubscriptionHttpStatusClass.Status5xx
                else -> SubscriptionHttpStatusClass.Unknown
            },
            redirectObserved = if (code in 300..399) {
                SubscriptionDiagnosticTriState.Yes
            } else {
                redirectObserved
            },
        )

    fun withException(error: Throwable): SubscriptionFetchDiagnostics {
        val cleartext = error.causeChainContains { throwable ->
            throwable.message.orEmpty().contains("cleartext", ignoreCase = true)
        }
        return copy(
            cleartextBlockedLikely = if (cleartext) {
                SubscriptionDiagnosticTriState.Yes
            } else {
                cleartextBlockedLikely
            },
            tlsFailureLikely = if (error.causeChainContains { it is SSLException || it is CertificateException }) {
                SubscriptionDiagnosticTriState.Yes
            } else {
                tlsFailureLikely
            },
            timeoutLikely = if (error.causeChainContains { it is SocketTimeoutException }) {
                SubscriptionDiagnosticTriState.Yes
            } else {
                timeoutLikely
            },
            dnsFailureLikely = if (error.causeChainContains { it is UnknownHostException }) {
                SubscriptionDiagnosticTriState.Yes
            } else {
                dnsFailureLikely
            },
        )
    }

    fun toSafeLogString(
        failureCategory: SubscriptionImportFailureCategory,
        safeMessageKey: String,
    ): String =
        listOf(
            "urlScheme=${urlScheme.safeValue}",
            "hasQuery=$hasQuery",
            "hasFragment=$hasFragment",
            "inputLengthBucket=${inputLengthBucket.safeValue}",
            "networkAvailable=${networkAvailable.safeValue}",
            "cleartextBlockedLikely=${cleartextBlockedLikely.safeValue}",
            "httpStatusClass=${httpStatusClass.safeValue}",
            "redirectObserved=${redirectObserved.safeValue}",
            "tlsFailureLikely=${tlsFailureLikely.safeValue}",
            "timeoutLikely=${timeoutLikely.safeValue}",
            "dnsFailureLikely=${dnsFailureLikely.safeValue}",
            "bodyLengthBucket=${bodyLengthBucket.safeValue}",
            "failureCategory=${failureCategory.name}",
            "safeMessageKey=$safeMessageKey",
        ).joinToString(separator = " ")

    companion object {
        val Unknown = SubscriptionFetchDiagnostics(
            urlScheme = SubscriptionUrlScheme.Missing,
            hasQuery = false,
            hasFragment = false,
            inputLengthBucket = SubscriptionInputLengthBucket.Empty,
        )

        fun fromInput(input: String): SubscriptionFetchDiagnostics {
            val normalized = sanitizeSubscriptionUrlInput(input)
            val scheme = runCatching { URI(normalized).scheme }
                .getOrNull()
                ?.lowercase()
                ?: normalized.substringBefore(":", missingDelimiterValue = "")
                    .takeIf { it.isNotBlank() }
                    ?.lowercase()

            return SubscriptionFetchDiagnostics(
                urlScheme = when (scheme) {
                    null -> SubscriptionUrlScheme.Missing
                    "http" -> SubscriptionUrlScheme.Http
                    "https" -> SubscriptionUrlScheme.Https
                    else -> SubscriptionUrlScheme.Other
                },
                hasQuery = normalized.contains("?"),
                hasFragment = normalized.contains("#"),
                inputLengthBucket = normalized.length.toInputLengthBucket(),
            )
        }
    }
}

internal fun sanitizeSubscriptionUrlInput(input: String): String =
    input.trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }

private fun Char.isInvisibleEdgeCharacter(): Boolean =
    this == '\u200B' ||
        this == '\u200C' ||
        this == '\u200D' ||
        this == '\u2060' ||
        this == '\uFEFF'

private fun Int.toInputLengthBucket(): SubscriptionInputLengthBucket =
    when {
        this == 0 -> SubscriptionInputLengthBucket.Empty
        this <= 128 -> SubscriptionInputLengthBucket.Short
        this <= 1024 -> SubscriptionInputLengthBucket.Medium
        this <= 4096 -> SubscriptionInputLengthBucket.Long
        else -> SubscriptionInputLengthBucket.VeryLong
    }

private fun Throwable.causeChainContains(predicate: (Throwable) -> Boolean): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (predicate(current)) return true
        current = current.cause
    }
    return false
}
