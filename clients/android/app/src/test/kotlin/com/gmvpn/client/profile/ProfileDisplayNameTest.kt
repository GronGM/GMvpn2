package com.gmvpn.client.profile

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDisplayNameTest {

    private val sampleUuid = "00000000-0000-0000-0000-000000000000"

    @Test
    fun `vless with ip and no fragment hides address`() {
        val uri = "vless://$sampleUuid@1.2.3.4:443?security=tls"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VLESS профиль", summary.displayName)
        assertEquals("VLESS", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `vless with host and no fragment hides host`() {
        val uri = "vless://$sampleUuid@vpn.example.com:443?security=tls"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VLESS профиль", summary.displayName)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains("vpn.example.com"))
    }

    @Test
    fun `vless with unsafe ip fragment falls back to generic label`() {
        val uri = "vless://$sampleUuid@1.2.3.4:443#1.2.3.4"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VLESS профиль", summary.displayName)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `vless uses safe decoded russian fragment`() {
        val uri = "vless://$sampleUuid@1.2.3.4:443?security=tls#" +
            "%D0%9D%D0%B8%D0%B4%D0%B5%D1%80%D0%BB%D0%B0%D0%BD%D0%B4%D1%8B"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Нидерланды", summary.displayName)
        assertEquals("VLESS", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `trojan uses safe fragment and never shows password or address`() {
        val uri = "trojan://password@1.2.3.4:443?security=tls#Netherlands"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Netherlands", summary.displayName)
        assertEquals("Trojan", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains("password"))
    }

    @Test
    fun `trojan without fragment hides password and address`() {
        val uri = "trojan://password@1.2.3.4:443?security=tls"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Trojan профиль", summary.displayName)
        assertEquals("Trojan", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains("password"))
    }

    @Test
    fun `vmess uses safe ps from decoded json`() {
        val uri = "vmess://" + vmessBase64(
            ps = "Amsterdam Test",
            add = "1.2.3.4",
            port = "443",
        )

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Amsterdam Test", summary.displayName)
        assertEquals("VMess", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `vmess without ps hides add port uuid and base64`() {
        val payload = vmessBase64(
            ps = "",
            add = "1.2.3.4",
            port = "443",
        )
        val summary = profileSummary("vmess://$payload", fallbackIndex = 1)

        assertEquals("VMess профиль", summary.displayName)
        assertEquals("VMess", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains(sampleUuid))
        assertFalse(summary.joined().contains(payload.take(12)))
    }

    @Test
    fun `vmess with unsafe ps hides address`() {
        val uri = "vmess://" + vmessBase64(
            ps = "1.2.3.4:443",
            add = "1.2.3.4",
            port = "443",
        )

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VMess профиль", summary.displayName)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `shadowsocks sip002 uses safe decoded fragment only`() {
        val uri = "ss://YWVzLTI1Ni1nY206cGFzcw@1.2.3.4:8388#Test%20SS"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Test SS", summary.displayName)
        assertEquals("Shadowsocks", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains("cGFzcw"))
    }

    @Test
    fun `shadowsocks without fragment hides method password host and port`() {
        val payload = Base64.getEncoder()
            .encodeToString("aes-256-gcm:pass@1.2.3.4:8388".toByteArray())
        val uri = "ss://$payload"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("Shadowsocks профиль", summary.displayName)
        assertEquals("Shadowsocks", summary.secondaryLabel)
        assertNoPrivateEndpoint(summary)
        assertFalse(summary.joined().contains("pass"))
        assertFalse(summary.joined().contains(payload))
    }

    @Test
    fun `unsafe hostname fragment is not shown`() {
        val uri = "vless://$sampleUuid@1.2.3.4:443#vpn.example.com"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VLESS профиль", summary.displayName)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `unsafe ipv6 fragment is not shown`() {
        val uri = "vless://$sampleUuid@[2001:db8::1]:443#[2001:db8::1]:443"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertEquals("VLESS профиль", summary.displayName)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `unsafe uuid uri and query-like fragments are not shown`() {
        val values = listOf(
            sampleUuid,
            "https://subscription.example.com/list",
            "user@example.com",
            "password=hunter2",
            "token=abcdef",
            "uuid=$sampleUuid",
            "alterId=0",
            "YWVzLTI1Ni1nY206cGFzc3dvcmQ",
        )

        values.forEachIndexed { index, fragment ->
            val summary = profileSummary(
                "vless://$sampleUuid@1.2.3.4:443#${fragment.encodeFragment()}",
                fallbackIndex = index + 1,
            )
            assertEquals("VLESS профиль", summary.displayName)
            assertNoPrivateEndpoint(summary)
        }
    }

    @Test
    fun `malformed uri falls back to numbered profile without crash`() {
        val summary = profileSummary("not-a-uri", fallbackIndex = 3)

        assertEquals("Профиль 3", summary.displayName)
        assertEquals("Профиль", summary.secondaryLabel)
    }

    @Test
    fun `display name removes controls and trims long safe labels`() {
        val longName = "Line1%0ALine2%09" + "Safe Name ".repeat(12)
        val uri = "trojan://password@1.2.3.4:443#$longName"

        val summary = profileSummary(uri, fallbackIndex = 1)

        assertFalse(summary.displayName.contains('\n'))
        assertFalse(summary.displayName.contains('\t'))
        assertTrue(summary.displayName.endsWith("…"))
        assertTrue(summary.displayName.length <= 72)
        assertNoPrivateEndpoint(summary)
    }

    @Test
    fun `secondary labels never include endpoint data`() {
        val uris = listOf(
            "vless://$sampleUuid@1.2.3.4:443?security=tls",
            "vmess://" + vmessBase64(ps = "", add = "1.2.3.4", port = "443"),
            "trojan://password@vpn.example.com:443?security=tls",
            "ss://YWVzLTI1Ni1nY206cGFzcw@vpn.example.com:8388",
            "broken",
        )

        uris.forEachIndexed { index, uri ->
            assertNoPrivateEndpoint(profileSummary(uri, fallbackIndex = index + 1))
        }
    }

    private fun assertNoPrivateEndpoint(summary: ProfileSummary) {
        val text = summary.joined()
        assertFalse(text.contains("1.2.3.4"))
        assertFalse(text.contains("2001:db8"))
        assertFalse(text.contains("example.com"))
        assertFalse(text.contains("vpn.example.com"))
        assertFalse(text.contains(":443"))
        assertFalse(text.contains(":8388"))
        assertFalse(text.contains(sampleUuid))
        assertFalse(text.contains("://"))
        assertFalse(text.contains('@'))
    }

    private fun ProfileSummary.joined(): String =
        "$displayName\n$secondaryLabel"

    private fun String.encodeFragment(): String =
        java.net.URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun vmessBase64(ps: String, add: String, port: String): String {
        val json = """
            {
              "v":"2",
              "ps":"$ps",
              "add":"$add",
              "port":"$port",
              "id":"$sampleUuid",
              "aid":"0",
              "net":"tcp",
              "type":"none",
              "host":"",
              "path":"",
              "tls":"tls"
            }
        """.trimIndent()
        return Base64.getEncoder()
            .encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }
}
