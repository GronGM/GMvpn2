package com.gmvpn.client.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM round-trip tests for the wire format produced by
 * [PerAppRoutingStore.encode] and parsed back by [decode]. The
 * store itself talks to DataStore and needs Android, but the
 * encoder/decoder pair is plain text — we can pin the schema with
 * pure JUnit.
 */
class PerAppRoutingStoreTest {

    @Test
    fun `empty blob decodes to default routing`() {
        val out = PerAppRoutingStore.decode("")
        assertEquals(PerAppMode.Off, out.mode)
        assertEquals(emptySet<String>(), out.packages)
    }

    @Test
    fun `unknown mode token falls back to Off`() {
        val out = PerAppRoutingStore.decode("WhatIsThis\ncom.example.app\n")
        assertEquals(PerAppMode.Off, out.mode)
        assertEquals(setOf("com.example.app"), out.packages)
    }

    @Test
    fun `IncludeOnly mode round-trips`() {
        val original = PerAppRouting(
            mode = PerAppMode.IncludeOnly,
            packages = setOf("com.android.chrome", "org.mozilla.firefox"),
        )
        val blob = PerAppRoutingStore.encode(original)
        assertTrue("encoded mode line", blob.startsWith("IncludeOnly\n"))
        val decoded = PerAppRoutingStore.decode(blob)
        assertEquals(original.mode, decoded.mode)
        assertEquals(original.packages, decoded.packages)
    }

    @Test
    fun `ExcludeListed mode round-trips`() {
        val original = PerAppRouting(
            mode = PerAppMode.ExcludeListed,
            packages = setOf("com.example.banking"),
        )
        val decoded = PerAppRoutingStore.decode(PerAppRoutingStore.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `encode sorts packages deterministically`() {
        val a = PerAppRouting(PerAppMode.IncludeOnly, setOf("z.app", "a.app", "m.app"))
        val b = PerAppRouting(PerAppMode.IncludeOnly, setOf("m.app", "a.app", "z.app"))
        // Two semantically equal routings encode byte-for-byte equal —
        // which means DataStore will not record spurious change events
        // when the in-memory iteration order shifts.
        assertEquals(PerAppRoutingStore.encode(a), PerAppRoutingStore.encode(b))
    }

    @Test
    fun `blank lines in the package list are tolerated`() {
        val out = PerAppRoutingStore.decode("IncludeOnly\n\ncom.x\n\n\ncom.y\n")
        assertEquals(PerAppMode.IncludeOnly, out.mode)
        assertEquals(setOf("com.x", "com.y"), out.packages)
    }
}
