package com.gmvpn.client.profile

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.gmvpn_ffi.FfiSubscriptionFormat
import javax.net.ssl.SSLHandshakeException

class SubscriptionFetchDiagnosticsTest {

    private val syntheticHost = "subscription.example.invalid"
    private val syntheticHttpsUrl =
        listOf("https:/", "/$syntheticHost", "/feed", "?sample=1", "#section").joinToString("")
    private val syntheticHttpUrl =
        listOf("http:/", "/$syntheticHost", "/feed", "?sample=1").joinToString("")
    private val fakeUuid = listOf(
        "00000000",
        "0000",
        "0000",
        "0000",
        "000000000000",
    ).joinToString("-")
    private val syntheticProfileUri =
        listOf("vless:/", "/$fakeUuid@", "profile.example.invalid", ":443#Synthetic").joinToString("")

    @Test
    fun `http cleartext input is rejected with safe diagnostic`() = runBlocking {
        val error = assertFetchFailure {
            SubscriptionFetcher().fetch(syntheticHttpUrl)
        }

        assertEquals(SubscriptionUrlScheme.Http, error.diagnostics.urlScheme)
        assertEquals(SubscriptionDiagnosticTriState.Yes, error.diagnostics.cleartextBlockedLikely)
        assertEquals(SubscriptionHttpStatusClass.Unknown, error.diagnostics.httpStatusClass)
        assertSafeLogDoesNotExposeInput(error.diagnostics)
    }

    @Test
    fun `http status classes and redirect flag are represented safely`() {
        val base = SubscriptionFetchDiagnostics.fromInput(syntheticHttpsUrl)

        assertEquals(SubscriptionHttpStatusClass.Status4xx, base.withHttpStatus(404).httpStatusClass)
        assertEquals(SubscriptionHttpStatusClass.Status5xx, base.withHttpStatus(500).httpStatusClass)
        assertEquals(SubscriptionHttpStatusClass.Status3xx, base.withHttpStatus(302).httpStatusClass)
        assertEquals(
            SubscriptionDiagnosticTriState.Yes,
            base.withHttpStatus(302).redirectObserved,
        )
    }

    @Test
    fun `timeout dns and tls exceptions are classified without raw exception text`() {
        val base = SubscriptionFetchDiagnostics.fromInput(syntheticHttpsUrl)

        assertEquals(
            SubscriptionDiagnosticTriState.Yes,
            base.withException(SocketTimeoutException("timeout")).timeoutLikely,
        )
        assertEquals(
            SubscriptionDiagnosticTriState.Yes,
            base.withException(UnknownHostException(syntheticHost)).dnsFailureLikely,
        )
        assertEquals(
            SubscriptionDiagnosticTriState.Yes,
            base.withException(SSLHandshakeException("certificate rejected")).tlsFailureLikely,
        )
        assertSafeLogDoesNotExposeInput(base.withException(UnknownHostException(syntheticHost)))
    }

    @Test
    fun `pipeline trims pasted edge characters and preserves query internally`() = runBlocking {
        var capturedUrl = ""
        val padded = " \u200B$syntheticHttpsUrl\n"

        val prepared = prepareSubscriptionImport(
            url = padded,
            format = FfiSubscriptionFormat.URI_LIST,
            fetchBody = {
                capturedUrl = it
                syntheticProfileUri.toByteArray(StandardCharsets.UTF_8)
            },
            decodeUris = { body, format ->
                assertEquals(FfiSubscriptionFormat.URI_LIST, format)
                SubscriptionDecodeOutput(
                    uris = body.toString(StandardCharsets.UTF_8).lines(),
                    warningCount = 0,
                )
            },
        )

        assertEquals(syntheticHttpsUrl, capturedUrl)
        assertTrue(capturedUrl.contains("?"))
        assertEquals(1, prepared.profiles.size)
    }

    @Test
    fun `fetch diagnostics propagate through import failure`() = runBlocking {
        val expected = SubscriptionFetchDiagnostics.fromInput(syntheticHttpsUrl)
            .withHttpStatus(404)
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticHttpsUrl,
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = {
                    throw SubscriptionFetchException(
                        message = "HTTP status from subscription endpoint",
                        diagnostics = expected,
                    )
                },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.FetchFailed, error.category)
        assertEquals(expected, subscriptionImportFetchDiagnostics(error))
        assertSafeLogDoesNotExposeInput(expected)
    }

    private fun assertSafeLogDoesNotExposeInput(diagnostics: SubscriptionFetchDiagnostics) {
        val safeLog = diagnostics.toSafeLogString(
            failureCategory = SubscriptionImportFailureCategory.FetchFailed,
            safeMessageKey = "subscription_error_network",
        )
        assertFalse(safeLog.contains(syntheticHttpsUrl))
        assertFalse(safeLog.contains(syntheticHttpUrl))
        assertFalse(safeLog.contains(syntheticHost))
        assertFalse(safeLog.contains("/feed"))
        assertFalse(safeLog.contains("sample=1"))
        assertFalse(safeLog.contains(fakeUuid))
    }

    private suspend fun assertFetchFailure(
        block: suspend () -> ByteArray,
    ): SubscriptionFetchException {
        try {
            block()
        } catch (error: SubscriptionFetchException) {
            return error
        }
        throw AssertionError("Expected SubscriptionFetchException")
    }

    private suspend fun assertImportFailure(
        block: suspend () -> PreparedSubscriptionImport,
    ): SubscriptionImportException {
        try {
            block()
        } catch (error: SubscriptionImportException) {
            return error
        }
        throw AssertionError("Expected SubscriptionImportException")
    }
}
