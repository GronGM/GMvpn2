package com.gmvpn.client.tunnel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mapping from raw engine status strings (emitted from the Go
 * side) to [TunnelStatus] is a stable contract: native clients match
 * on the string constants in `core/gmvpn/tunnel.go`. Pinning it here
 * means a rename on either side is caught the moment we run unit
 * tests.
 */
class TunnelStatusTest {

    @Test
    fun `known engine strings map to their typed value`() {
        assertEquals(TunnelStatus.Idle, TunnelStatus.fromEngine("idle"))
        assertEquals(TunnelStatus.Starting, TunnelStatus.fromEngine("starting"))
        assertEquals(TunnelStatus.Connected, TunnelStatus.fromEngine("connected"))
        assertEquals(TunnelStatus.Reconnecting, TunnelStatus.fromEngine("reconnecting"))
        assertEquals(TunnelStatus.Stopping, TunnelStatus.fromEngine("stopping"))
        assertEquals(TunnelStatus.Error, TunnelStatus.fromEngine("error"))
    }

    @Test
    fun `unknown engine strings collapse to Error`() {
        // Anything we cannot map deserves "the engine said something
        // we don't understand" — Error surfaces it in the UI rather
        // than letting the tunnel silently look healthy.
        assertEquals(TunnelStatus.Error, TunnelStatus.fromEngine(""))
        assertEquals(TunnelStatus.Error, TunnelStatus.fromEngine("CONNECTED"))
        assertEquals(TunnelStatus.Error, TunnelStatus.fromEngine("idle "))
        assertEquals(TunnelStatus.Error, TunnelStatus.fromEngine("nonsense"))
    }
}
