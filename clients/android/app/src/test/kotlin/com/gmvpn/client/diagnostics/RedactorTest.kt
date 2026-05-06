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
    fun `redactProfileUri masks vless uuid in userinfo`() {
        val input = "vless://$sampleUuid@host.example:443?security=reality" +
            "&pbk=ABC&sid=DEF#TestServer"
        val out = Redactor.redactProfileUri(input)
        assertFalse("uuid must be redacted", out.contains(sampleUuid))
        assertTrue("vless prefix preserved", out.startsWith("vless://<uuid>@"))
        assertTrue("host preserved", out.contains("host.example:443"))
        assertFalse("Reality pbk leaked", out.contains("ABC"))
        assertFalse("Reality sid leaked", out.contains("DEF"))
    }

    @Test
    fun `redactProfileUri replaces full vmess body with placeholder`() {
        val input = "vmess://aGVsbG8gd29ybGQ="
        val out = Redactor.redactProfileUri(input)
        assertEquals("vmess://<base64-redacted>", out)
    }

    @Test
    fun `redactProfileUri masks trojan password`() {
        val input = "trojan://hunter2@trojan.example:443?security=tls#main"
        val out = Redactor.redactProfileUri(input)
        assertFalse("password leaked", out.contains("hunter2"))
        assertTrue(out.startsWith("trojan://<pw>@"))
        assertTrue("host preserved", out.contains("trojan.example:443"))
    }

    @Test
    fun `redactProfileUri collapses shadowsocks userinfo`() {
        val input = "ss://Y2hhY2hhMjA6cHc=@ss.example:8388#JP-1"
        val out = Redactor.redactProfileUri(input)
        assertFalse("base64 userinfo leaked", out.contains("Y2hhY2hhMjA6cHc"))
        assertTrue(out.startsWith("ss://<ss-userinfo>@"))
        assertTrue("host preserved", out.contains("ss.example:8388"))
    }

    @Test
    fun `redactProfileUri handles legacy ss without at`() {
        val input = "ss://YWVzLTI1Ni1nY206cHdAc3MuZXhhbXBsZTo4Mzg4#legacy"
        val out = Redactor.redactProfileUri(input)
        assertEquals("ss://<ss-userinfo>", out)
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
        assertNotNull(out)
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
