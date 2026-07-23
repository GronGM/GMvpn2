package com.gmvpn.client.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUriValidationTest {

    @Test
    fun `supported VPN profile schemes are accepted`() {
        assertTrue(hasSupportedProfileScheme("vless://id@example.invalid:443"))
        assertTrue(hasSupportedProfileScheme("vmess://payload"))
        assertTrue(hasSupportedProfileScheme("trojan://password@example.invalid:443"))
        assertTrue(hasSupportedProfileScheme("ss://payload@example.invalid:8388"))
    }

    @Test
    fun `plain text and subscription URLs are rejected before native parsing`() {
        assertFalse(hasSupportedProfileScheme(""))
        assertFalse(hasSupportedProfileScheme("SyntheticOne"))
        assertFalse(hasSupportedProfileScheme("https://subscription.example.invalid/user"))
        assertFalse(hasSupportedProfileScheme("ftp://example.invalid/profile"))
    }

    @Test
    fun `hysteria2 is recognised but not connectable by the current engine`() {
        // Parsed and displayed, but the bundled Xray cannot carry its traffic,
        // so it must not be in the connect allowlist and must be flagged so the
        // UI can explain why instead of starting a dead tunnel.
        assertFalse(hasSupportedProfileScheme("hysteria2://password@example.invalid:443"))
        assertFalse(hasSupportedProfileScheme("hy2://password@example.invalid:443"))
        assertTrue(isEngineUnsupportedScheme("hysteria2://password@example.invalid:443"))
        assertTrue(isEngineUnsupportedScheme("hy2://password@example.invalid:443"))
        assertTrue(isEngineUnsupportedScheme("HYSTERIA2://password@example.invalid:443"))
    }

    @Test
    fun `supported and unknown schemes are not flagged as engine-unsupported`() {
        assertFalse(isEngineUnsupportedScheme("vless://id@example.invalid:443"))
        assertFalse(isEngineUnsupportedScheme("trojan://password@example.invalid:443"))
        assertFalse(isEngineUnsupportedScheme("https://subscription.example.invalid/user"))
        assertFalse(isEngineUnsupportedScheme(""))
    }
}
