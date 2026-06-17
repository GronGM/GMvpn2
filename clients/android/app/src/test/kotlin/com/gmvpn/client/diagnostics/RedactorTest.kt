package com.gmvpn.client.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for [Redactor]. They protect us from
 * accidentally regressing the secret-leakage surface of the
 * diagnostics export — the worst possible failure mode for this
 * feature.
 */
class RedactorTest {

    private val sampleUuid = "11111111-1111-1111-1111-111111111111"

    @Test
    fun `redactProfileUri masks vless uri including endpoint`() {
        val input = "vless://$sampleUuid@host.example:443?security=reality" +
            "&pbk=ABC&sid=DEF#TestServer"
        val out = Redactor.redactProfileUri(input)
        assertEquals("vless://<redacted-profile-uri>", out)
        assertFalse("uuid must be redacted", out.contains(sampleUuid))
        assertFalse("host leaked", out.contains("host.example"))
        assertFalse("port leaked", out.contains(":443"))
        assertFalse("Reality pbk leaked", out.contains("ABC"))
        assertFalse("Reality sid leaked", out.contains("DEF"))
    }

    @Test
    fun `redactProfileUri replaces full vmess body with placeholder`() {
        val input = "vmess://aGVsbG8gd29ybGQ="
        val out = Redactor.redactProfileUri(input)
        assertEquals("vmess://<redacted-profile-uri>", out)
    }

    @Test
    fun `redactProfileUri masks trojan password and endpoint`() {
        val input = "trojan://hunter2@trojan.example:443?security=tls#main"
        val out = Redactor.redactProfileUri(input)
        assertEquals("trojan://<redacted-profile-uri>", out)
        assertFalse("password leaked", out.contains("hunter2"))
        assertFalse("host leaked", out.contains("trojan.example"))
        assertFalse("port leaked", out.contains(":443"))
    }

    @Test
    fun `redactProfileUri collapses shadowsocks userinfo and endpoint`() {
        val input = "ss://Y2hhY2hhMjA6cHc=@ss.example:8388#JP-1"
        val out = Redactor.redactProfileUri(input)
        assertEquals("ss://<redacted-profile-uri>", out)
        assertFalse("base64 userinfo leaked", out.contains("Y2hhY2hhMjA6cHc"))
        assertFalse("host leaked", out.contains("ss.example"))
        assertFalse("port leaked", out.contains(":8388"))
    }

    @Test
    fun `redactProfileUri handles legacy ss without at`() {
        val input = "ss://YWVzLTI1Ni1nY206cHdAc3MuZXhhbXBsZTo4Mzg4#legacy"
        val out = Redactor.redactProfileUri(input)
        assertEquals("ss://<redacted-profile-uri>", out)
    }

    @Test
    fun `redactText scrubs uuids in arbitrary log lines`() {
        val input = "11:22:33 D/Tunnel: connecting profile $sampleUuid to host"
        val out = Redactor.redactText(input)
        assertFalse(out.contains(sampleUuid))
        assertTrue(out.contains("<uuid>"))
    }

    @Test
    fun `redactText scrubs Authorization headers`() {
        val input = "Authorization: Bearer abcdef.0123\n"
        val out = Redactor.redactText(input)
        assertFalse(out.contains("abcdef.0123"))
        assertTrue(out.contains("Authorization: <redacted>"))
    }

    @Test
    fun `redactText scrubs password and pwd query params`() {
        val input = "GET /login?user=foo&password=hunter2&pwd=other HTTP/1.1"
        val out = Redactor.redactText(input)
        assertFalse(out.contains("hunter2"))
        assertFalse(out.contains("=other"))
        assertTrue(out.contains("password=<redacted>"))
        assertTrue(out.contains("pwd=<redacted>"))
    }

    @Test
    fun `redactText recurses into URI tokens inside log lines`() {
        val input = "imported vless://$sampleUuid@h.example:443?pbk=ZZZ#R"
        val out = Redactor.redactText(input)
        assertFalse(out.contains(sampleUuid))
        assertFalse(out.contains("ZZZ"))
        assertFalse(out.contains("h.example"))
        assertFalse(out.contains(":443"))
        assertNotNull(out)
    }

    @Test
    fun `redactText scrubs urls ipv4 and host context`() {
        val input = "GET https://sub.example/path?token=abc\n" +
            "dial tcp 203.0.113.9:443 failed\n" +
            "lookup private.example failed\n" +
            "destination edge.example:8443"

        val out = Redactor.redactText(input)

        assertFalse(out.contains("sub.example"))
        assertFalse(out.contains("203.0.113.9"))
        assertFalse(out.contains("private.example"))
        assertFalse(out.contains("edge.example"))
        assertTrue(out.contains("<redacted-url>"))
        assertTrue(out.contains("<ipv4>"))
        assertTrue(out.contains("lookup <redacted-host>"))
        assertTrue(out.contains("destination <redacted-host>:8443"))
    }

    @Test
    fun `redactProfileUri leaves unknown schemes alone`() {
        val input = "https://example.com/path?ok=1"
        val out = Redactor.redactProfileUri(input)
        // Generic redaction may still mask query-secrets; the URL
        // shape itself stays intact.
        assertTrue(out.startsWith("https://example.com"))
    }
}
