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
}
