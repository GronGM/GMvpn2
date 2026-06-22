package com.gmvpn.client.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun `connected requires VPN interface and engine evidence`() {
        val evidence = ConnectionEvidence(
            vpnInterfaceEstablished = true,
            engineStarted = true,
        )

        val state = ConnectionState.Connected(evidence)

        assertEquals(evidence, state.evidence)
        assertTrue(evidence.supportsConnectedState)
    }

    @Test
    fun `engine started without VPN interface cannot be connected`() {
        val evidence = ConnectionEvidence(
            vpnInterfaceEstablished = false,
            engineStarted = true,
        )

        assertFalse(evidence.supportsConnectedState)
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.Connected(evidence)
        }
    }

    @Test
    fun `immediate failure cannot be connected`() {
        val evidence = ConnectionEvidence(
            vpnInterfaceEstablished = true,
            engineStarted = true,
            immediateFailure = ConnectionFailure(ConnectionFailureCategory.EngineStartFailed),
        )

        assertFalse(evidence.supportsConnectedState)
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.Connected(evidence)
        }
    }

    @Test
    fun `failed state carries only a typed category`() {
        val failure = ConnectionFailure(ConnectionFailureCategory.VpnPermissionDenied)

        assertEquals(failure, ConnectionState.Failed(failure).failure)
    }
}
