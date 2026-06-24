package com.gmvpn.client.tunnel

import com.gmvpn.core.gmvpn.FakeGmvpnRegistry
import com.gmvpn.core.gmvpn.FakeGmvpnTunnel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineBridgeLifecycleTest {

    @After
    fun tearDown() {
        FakeGmvpnRegistry.reset()
    }

    @Test
    fun `stop releases running gomobile tunnel instance`() {
        val fakeTunnel = FakeGmvpnTunnel()
        FakeGmvpnRegistry.nextTunnel = fakeTunnel
        val bridge = EngineBridge()

        bridge.start("{}", tunFd = 3, mtu = 1500, socksPort = 12345) { _, _ -> }

        assertTrue(bridge.isRunning())
        assertEquals(1, fakeTunnel.startCalls)
        assertEquals(12345, fakeTunnel.lastSocksPort)
        assertTrue(fakeTunnel.running)

        bridge.stop()

        assertFalse(bridge.isRunning())
        assertEquals(1, fakeTunnel.stopCalls)
        assertFalse(fakeTunnel.running)

        bridge.stop()

        assertEquals("second stop should be a no-op", 1, fakeTunnel.stopCalls)
    }

    @Test
    fun `failed start does not retain running tunnel instance`() {
        val fakeTunnel = FakeGmvpnTunnel(startError = IllegalStateException("start failed"))
        FakeGmvpnRegistry.nextTunnel = fakeTunnel
        val bridge = EngineBridge()

        assertThrows(EngineStartException::class.java) {
            bridge.start("{}", tunFd = 3, mtu = 1500, socksPort = 12345) { _, _ -> }
        }

        assertFalse(bridge.isRunning())
        assertEquals(1, fakeTunnel.startCalls)
        assertEquals(0, fakeTunnel.stopCalls)
        assertFalse(fakeTunnel.running)
    }
}
