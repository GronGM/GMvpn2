package com.gmvpn.client.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileEntryTest {

    private val sampleUuid = "00000000-0000-0000-0000-000000000000"

    @Test
    fun `stored entry round trips metadata without exposing uri fields`() {
        val uri = "vless://$sampleUuid@1.2.3.4:443?security=tls#Unsafe"
        val entry = ProfileEntry(
            uri = uri,
            customName = "Home test",
            createdAtEpochMillis = 100,
            updatedAtEpochMillis = 200,
            source = ProfileSource.MANUAL,
        )

        val encoded = StoredProfileEntry.encode(entry)
        val decoded = StoredProfileEntry.decode(encoded)

        assertFalse(encoded.contains("1.2.3.4"))
        assertFalse(encoded.contains(sampleUuid))
        assertEquals(uri, decoded.uri)
        assertEquals("Home test", decoded.customName)
        assertEquals(ProfileSource.MANUAL, decoded.source)
        assertEquals(100L, decoded.createdAtEpochMillis)
        assertEquals(200L, decoded.updatedAtEpochMillis)
    }

    @Test
    fun `legacy raw uri decodes as legacy profile`() {
        val decoded = StoredProfileEntry.decode("trojan://password@1.2.3.4:443")

        assertEquals(ProfileSource.LEGACY, decoded.source)
        assertNull(decoded.customName)
        assertEquals("trojan://password@1.2.3.4:443", decoded.uri)
    }

    @Test
    fun `rename sanitizes controls and rejects private labels`() {
        assertEquals("Work profile", sanitizeCustomProfileName("Work\nprofile"))
        assertNull(sanitizeCustomProfileName(""))
        assertNull(sanitizeCustomProfileName("1.2.3.4:443"))
        assertNull(sanitizeCustomProfileName("vpn.example.com"))
        assertNull(sanitizeCustomProfileName(sampleUuid))
        assertNull(sanitizeCustomProfileName("password=hunter2"))
        assertNull(sanitizeCustomProfileName("vless://$sampleUuid@1.2.3.4:443"))
    }

    @Test
    fun `import plan skips duplicate uri and suffixes duplicate safe names`() {
        val uri1 = "vless://$sampleUuid@1.2.3.4:443#Office"
        val uri2 = "trojan://password@5.6.7.8:443#Office"

        val plan = buildProfileImportPlan(listOf(uri1, uri1, uri2))

        assertEquals(1, plan.duplicateUriCount)
        assertEquals(2, plan.profiles.size)
        assertEquals("Office", plan.profiles[0].suggestedName)
        assertEquals("Office (2)", plan.profiles[1].suggestedName)
        assertNotNull(plan.profiles.single { it.protocolLabel == "VLESS" })
        assertNotNull(plan.profiles.single { it.protocolLabel == "Trojan" })
    }

    @Test
    fun `delete active profile falls back to first remaining profile`() {
        assertEquals(0, activeIndexAfterRemoval(activeIndex = 2, removedIndex = 2, itemCount = 3))
        assertEquals(1, activeIndexAfterRemoval(activeIndex = 2, removedIndex = 1, itemCount = 3))
        assertEquals(-1, activeIndexAfterRemoval(activeIndex = 0, removedIndex = 0, itemCount = 1))
    }
}
