package com.gmvpn.client.profile

import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches a subscription URL into a byte body. HTTPS-only by default
 * (per `docs/memory/project-context.md` — "no silent network calls"
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
            val parsed = try {
                URL(url)
            } catch (e: Exception) {
                throw SubscriptionFetchException("invalid HTTPS subscription URL")
            }
            if (!parsed.protocol.equals("https", ignoreCase = true)) {
                throw SubscriptionFetchException("only https:// URLs are accepted")
            }

            val conn = parsed.openConnection() as HttpsURLConnection
            try {
                conn.connectTimeout = connectTimeoutMs
                conn.readTimeout = readTimeoutMs
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = false
                userAgent?.let { conn.setRequestProperty("User-Agent", it) }

                val code = conn.responseCode
                if (code in 300..399) {
                    throw SubscriptionFetchException(
                        "redirect ($code) refused — point at the final HTTPS URL",
                    )
                }
                if (code !in 200..299) {
                    throw SubscriptionFetchException("HTTP $code from subscription endpoint")
                }

                val stream = conn.inputStream
                    ?: throw SubscriptionFetchException("empty response body")
                val out = ByteArray(maxBodyBytes + 1)
                var read = 0
                while (read < out.size) {
                    val n = stream.read(out, read, out.size - read)
                    if (n <= 0) break
                    read += n
                }
                if (read > maxBodyBytes) {
                    throw SubscriptionFetchException(
                        "subscription body exceeds $maxBodyBytes bytes",
                    )
                }
                out.copyOfRange(0, read)
            } catch (e: IOException) {
                throw SubscriptionFetchException("network error while fetching subscription", e)
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
    cause: Throwable? = null,
) : RuntimeException(message, cause)

