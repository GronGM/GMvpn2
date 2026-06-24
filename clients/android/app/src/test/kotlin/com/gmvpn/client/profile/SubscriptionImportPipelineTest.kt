package com.gmvpn.client.profile

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.gmvpn_ffi.FfiSubscriptionFormat

class SubscriptionImportPipelineTest {

    private val fakeUuid = listOf(
        "00000000",
        "0000",
        "0000",
        "0000",
        "000000000000",
    ).joinToString("-")
    private val syntheticHost = "example.invalid"
    private val syntheticUri =
        listOf("vless:/", "/$fakeUuid@", syntheticHost, ":8443?security=none#Test%20Profile")
            .joinToString("")

    @Test
    fun `synthetic uri list import produces profile preview`() = runBlocking {
        val prepared = prepareSubscriptionImport(
            url = syntheticSubscriptionUrl("list"),
            format = FfiSubscriptionFormat.URI_LIST,
            fetchBody = { syntheticUri.toByteArray(StandardCharsets.UTF_8) },
            decodeUris = { body, format ->
                assertEquals(FfiSubscriptionFormat.URI_LIST, format)
                SubscriptionDecodeOutput(
                    uris = body.toString(StandardCharsets.UTF_8).lines(),
                    warningCount = 0,
                )
            },
        )

        assertEquals(1, prepared.profiles.size)
        assertEquals("Test Profile", prepared.profiles.single().suggestedName)
        assertEquals("VLESS", prepared.profiles.single().protocolLabel)
    }

    @Test
    fun `synthetic base64 subscription import produces profile preview`() = runBlocking {
        val body = Base64.getEncoder().encodeToString(
            syntheticUri.toByteArray(StandardCharsets.UTF_8),
        )

        val prepared = prepareSubscriptionImport(
            url = syntheticSubscriptionUrl("base64"),
            format = FfiSubscriptionFormat.BASE64_URI_LIST,
            fetchBody = { body.toByteArray(StandardCharsets.UTF_8) },
            decodeUris = { encoded, format ->
                assertEquals(FfiSubscriptionFormat.BASE64_URI_LIST, format)
                val decoded = Base64.getDecoder().decode(encoded)
                    .toString(StandardCharsets.UTF_8)
                SubscriptionDecodeOutput(uris = decoded.lines(), warningCount = 0)
            },
        )

        assertEquals(1, prepared.profiles.size)
        assertEquals("Test Profile", prepared.profiles.single().suggestedName)
    }

    @Test
    fun `synthetic sip008 failure is typed and non generic`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("sip008"),
                format = FfiSubscriptionFormat.SIP008,
                fetchBody = { """{"servers": []}""".toByteArray(StandardCharsets.UTF_8) },
                decodeUris = { _, _ ->
                    throw IllegalArgumentException("unsupported SIP008 synthetic fixture")
                },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.UnsupportedFormat, error.category)
        assertEquals("decode_failed", error.importStage)
        assertEquals("decode", error.failureOrigin)
        assertEquals("illegal_argument_exception", error.throwableKind)
    }

    @Test
    fun `empty input returns typed empty input error`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = " ",
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { ByteArray(0) },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.EmptyInput, error.category)
        assertEquals("input_normalized", error.importStage)
        assertEquals("input", error.failureOrigin)
    }

    @Test
    fun `invalid input returns typed parse error without raw input`() = runBlocking {
        val rawInput = "not a profile from $syntheticHost"

        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("invalid"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { rawInput.toByteArray(StandardCharsets.UTF_8) },
                decodeUris = { _, _ -> throw RuntimeException("parser rejected input") },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.ParseFailed, error.category)
        assertEquals("decode_failed", error.importStage)
        assertEquals("decode", error.failureOrigin)
        assertFalse(error.message.orEmpty().contains(rawInput))
        assertFalse(error.message.orEmpty().contains(syntheticHost))
    }

    @Test
    fun `network fetch failure returns typed fetch error`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("network"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = {
                    throw SubscriptionFetchException("network error while fetching subscription")
                },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.FetchFailed, error.category)
        assertEquals("fetch_failed", error.importStage)
        assertEquals("fetch", error.failureOrigin)
        assertEquals("subscription_fetch_exception", error.throwableKind)
    }

    @Test
    fun `unexpected fetch io failure is wrapped with fetch boundary`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("network-io"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { throw IOException("synthetic network failure") },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.FetchFailed, error.category)
        assertEquals("fetch_failed", error.importStage)
        assertEquals("fetch", error.failureOrigin)
        assertEquals("io_exception", error.throwableKind)
        assertTrue(subscriptionImportFetchDiagnostics(error) != null)
    }

    @Test
    fun `fetch dns tls and timeout failures keep safe likely flags`() = runBlocking {
        assertFetchBoundary(
            cause = UnknownHostException("synthetic dns"),
            expectedKind = "unknown_host_exception",
            expectedDns = SubscriptionDiagnosticTriState.Yes,
        )
        assertFetchBoundary(
            cause = SSLHandshakeException("synthetic tls"),
            expectedKind = "ssl_exception",
            expectedTls = SubscriptionDiagnosticTriState.Yes,
        )
        assertFetchBoundary(
            cause = SocketTimeoutException("synthetic timeout"),
            expectedKind = "timeout_exception",
            expectedTimeout = SubscriptionDiagnosticTriState.Yes,
        )
    }

    @Test
    fun `empty decoder result returns typed no profiles error`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("empty"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { "comment only".toByteArray(StandardCharsets.UTF_8) },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.NoProfilesFound, error.category)
        assertEquals("decode_failed", error.importStage)
        assertEquals("decode", error.failureOrigin)
    }

    @Test
    fun `save failure is distinguishable internally`() {
        val error = subscriptionSaveFailure(IllegalStateException("encrypted store failed"))

        assertEquals(SubscriptionImportFailureCategory.SaveFailed, error.category)
        assertEquals("save_failed", error.importStage)
        assertEquals("save", error.failureOrigin)
        assertEquals(SubscriptionImportFailureCategory.SaveFailed, subscriptionImportFailureCategory(error))
    }

    @Test
    fun `failure messages do not expose profile secrets`() = runBlocking {
        val rawUri = syntheticUri

        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("parse"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { rawUri.toByteArray(StandardCharsets.UTF_8) },
                decodeUris = { _, _ -> throw RuntimeException("parser rejected input") },
            )
        }

        val visibleMessage = error.message.orEmpty()
        assertEquals("ParseFailed", visibleMessage)
        assertFalse(visibleMessage.contains("vless:/" + "/"))
        assertFalse(visibleMessage.contains(syntheticHost))
        assertFalse(visibleMessage.contains(fakeUuid))
        assertTrue(error.cause != null)
    }

    @Test
    fun `decode boundary wrapper keeps raw throwable out of diagnostics surface`() = runBlocking {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("decode-wrapper"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { syntheticUri.toByteArray(StandardCharsets.UTF_8) },
                decodeUris = { _, _ ->
                    throw RuntimeException("synthetic parser crash at $syntheticHost")
                },
            )
        }

        assertEquals(SubscriptionImportFailureCategory.ParseFailed, error.category)
        assertEquals("decode_failed", error.importStage)
        assertEquals("decode", error.failureOrigin)
        assertEquals("unknown_exception", error.throwableKind)
        assertFalse(error.message.orEmpty().contains(syntheticHost))
        assertTrue(error.cause != null)
    }

    private suspend fun assertFetchBoundary(
        cause: Throwable,
        expectedKind: String,
        expectedDns: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
        expectedTls: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
        expectedTimeout: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    ) {
        val error = assertImportFailure {
            prepareSubscriptionImport(
                url = syntheticSubscriptionUrl("network-kind"),
                format = FfiSubscriptionFormat.URI_LIST,
                fetchBody = { throw cause },
                decodeUris = { _, _ -> SubscriptionDecodeOutput(emptyList(), 0) },
            )
        }
        val diagnostics = subscriptionImportFetchDiagnostics(error)

        assertEquals(SubscriptionImportFailureCategory.FetchFailed, error.category)
        assertEquals("fetch_failed", error.importStage)
        assertEquals("fetch", error.failureOrigin)
        assertEquals(expectedKind, error.throwableKind)
        assertEquals(expectedDns, diagnostics?.dnsFailureLikely)
        assertEquals(expectedTls, diagnostics?.tlsFailureLikely)
        assertEquals(expectedTimeout, diagnostics?.timeoutLikely)
    }

    private fun syntheticSubscriptionUrl(path: String): String =
        listOf("https:/", "/subscription.", syntheticHost, "/", path).joinToString("")

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
