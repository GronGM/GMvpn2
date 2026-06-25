package com.gmvpn.client.tunnel

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProxyExposureTrackerTest {

    @Test
    fun `initial snapshot is idle and contains no live listener evidence`() {
        val tracker = LocalProxyExposureTracker()

        val snapshot = tracker.snapshot()

        assertEquals(LocalProxyListenerState.Idle, snapshot.state)
        assertEquals(LocalProxyBindClass.Unknown, snapshot.bindClass)
        assertEquals(LocalProxyPortClass.Unknown, snapshot.portClass)
        assertFalse(snapshot.engineRunning)
        assertFalse(snapshot.tunEstablished)
    }

    @Test
    fun `bind address classification records only safe address class`() {
        val loopback = InetAddress.getLoopbackAddress().hostAddress
        val nonLoopback = InetAddress.getByAddress(byteArrayOf(10, 0, 0, 1)).hostAddress

        assertEquals(
            LocalProxyBindClass.Loopback,
            LocalProxyExposureTracker.classifyBindAddress(loopback),
        )
        assertEquals(
            LocalProxyBindClass.NonLoopback,
            LocalProxyExposureTracker.classifyBindAddress(nonLoopback),
        )
        assertEquals(
            LocalProxyBindClass.Unknown,
            LocalProxyExposureTracker.classifyBindAddress(""),
        )
    }

    @Test
    fun `running snapshot is cleared and generation rotates after stop`() {
        val tracker = LocalProxyExposureTracker()
        tracker.markAllocated(
            bindClass = LocalProxyBindClass.Loopback,
            portClass = LocalProxyPortClass.EphemeralRuntime,
        )
        val allocatedGeneration = tracker.snapshot().generation

        tracker.markStarting(tunEstablished = true)
        tracker.markRunning(tunEstablished = true)

        val running = tracker.snapshot()
        assertEquals(LocalProxyListenerState.Running, running.state)
        assertTrue(running.engineRunning)
        assertTrue(running.tunEstablished)

        tracker.markStopping()
        tracker.markStopped()

        val stopped = tracker.snapshot()
        assertEquals(LocalProxyListenerState.Stopped, stopped.state)
        assertEquals(LocalProxyBindClass.Unknown, stopped.bindClass)
        assertEquals(LocalProxyPortClass.Unknown, stopped.portClass)
        assertFalse(stopped.engineRunning)
        assertFalse(stopped.tunEstablished)
        assertNotEquals(allocatedGeneration, stopped.generation)
    }

    @Test
    fun `redacted summary does not include raw network or profile values`() {
        val tracker = LocalProxyExposureTracker()
        tracker.markAllocated(
            bindClass = LocalProxyBindClass.Loopback,
            portClass = LocalProxyPortClass.EphemeralRuntime,
        )
        tracker.markStarting(tunEstablished = true)

        val summary = tracker.snapshot().redactedSummary()

        assertTrue(summary.contains("state="))
        assertTrue(summary.contains("bindClass="))
        assertTrue(summary.contains("portClass="))
        assertFalse(summary.contains("://"))
        assertFalse(summary.contains("@"))
        assertFalse(summary.contains("?"))
        assertFalse(summary.contains("&"))
    }
}
