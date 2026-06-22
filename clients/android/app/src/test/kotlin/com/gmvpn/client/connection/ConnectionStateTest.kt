package com.gmvpn.client.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun `engine started alone is not connected`() {
        val evidence = ConnectionEvidence(
            engineStarted = true,
        )

        assertFalse(evidence.hasMinimumVpnPathEvidence)
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.Connected(evidence)
        }
    }

    @Test
    fun `permission and interface without engine is not connected`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = false,
        )

        assertFalse(evidence.hasMinimumVpnPathEvidence)
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.Connected(evidence)
        }
    }

    @Test
    fun `minimum evidence allows connected`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
        )

        val state = ConnectionState.Connected(
            evidence = evidence,
        )

        assertEquals(
            evidence,
            state.evidence,
        )
        assertTrue(evidence.hasMinimumVpnPathEvidence)
    }

    @Test
    fun `fatal error blocks connected`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
            immediateFailure = ConnectionFailure(
                category = ConnectionFailureCategory.EngineStartFailed,
            ),
        )

        assertFalse(evidence.hasMinimumVpnPathEvidence)
        assertThrows(IllegalArgumentException::class.java) {
            ConnectionState.Connected(evidence)
        }
    }

    @Test
    fun `android VPN network visible marks validated`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
            androidVpnNetworkVisible = true,
        )

        assertTrue(evidence.hasMinimumVpnPathEvidence)
        assertTrue(evidence.hasAndroidValidatedVpnEvidence)
    }

    @Test
    fun `traffic probe fail does not erase minimum path evidence`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
            trafficProbe = TrafficProbeEvidence.Failed(
                category = ConnectionFailureCategory.DnsFailure,
            ),
        )

        assertTrue(evidence.hasMinimumVpnPathEvidence)
        assertFalse(evidence.hasAndroidValidatedVpnEvidence)
        assertEquals(
            ConnectionFailureCategory.DnsFailure,
            (evidence.trafficProbe as TrafficProbeEvidence.Failed).category,
        )
    }

    @Test
    fun `failed state carries only a typed category`() {
        val failure = ConnectionFailure(
            category = ConnectionFailureCategory.VpnPermissionDenied,
        )

        val state = ConnectionState.Failed(
            failure = failure,
        )

        assertEquals(
            failure,
            state.failure,
        )
    }
}
