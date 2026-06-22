package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateDebugComparisonTest {

    @Test
    fun `legacy connected without minimum evidence is categorized`() {
        val evidence = ConnectionEvidence(
            engineStarted = true,
        )
        val shadowState = TunnelStatus.Connected.toConnectionState(
            evidence = evidence,
        )

        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Connected,
            shadowState = shadowState,
            shadowEvidence = evidence,
        )

        assertEquals(
            ConnectionStateMismatchCategory.LegacyConnectedWithoutMinimumEvidence,
            comparison.mismatchCategory,
        )
        assertFalse(comparison.hasMinimumConnectedEvidence)
        assertEquals(
            "legacy_connected_without_minimum_evidence",
            comparison.toDebugFields().getValue("mismatch_category"),
        )
    }

    @Test
    fun `legacy connected and shadow connected with minimum evidence has no mismatch`() {
        val evidence = minimumEvidence()
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Connected,
            shadowState = ConnectionState.Connected(evidence),
        )

        assertEquals(ConnectionStateMismatchCategory.None, comparison.mismatchCategory)
        assertTrue(comparison.hasMinimumConnectedEvidence)
        assertEquals("none", comparison.toDebugFields().getValue("mismatch_category"))
    }

    @Test
    fun `stale preparing is categorized`() {
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Idle,
            shadowState = ConnectionState.Preparing,
        )

        assertEquals(
            ConnectionStateMismatchCategory.StalePreparing,
            comparison.mismatchCategory,
        )
    }

    @Test
    fun `permission denial stays failed and never becomes debug connected`() {
        val failure = ConnectionFailure(
            category = ConnectionFailureCategory.VpnPermissionDenied,
        )
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Preparing,
            shadowState = ConnectionState.Failed(failure),
            shadowEvidence = ConnectionEvidence(
                immediateFailure = failure,
            ),
        )

        assertEquals(
            ConnectionStateMismatchCategory.LegacyConnectingNewFailed,
            comparison.mismatchCategory,
        )
        assertEquals(
            ConnectionStateDebugCategory.Failed,
            comparison.connectionStateCategory,
        )
        assertTrue(comparison.hasImmediateFailure)
        assertFalse(comparison.hasMinimumConnectedEvidence)
    }

    @Test
    fun `engine started alone cannot become debug connected`() {
        val evidence = ConnectionEvidence(
            engineStarted = true,
        )
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Connected,
            shadowState = TunnelStatus.Connected.toConnectionState(
                evidence = evidence,
            ),
            shadowEvidence = evidence,
        )

        assertEquals(
            ConnectionStateDebugCategory.Failed,
            comparison.connectionStateCategory,
        )
        assertEquals(
            ConnectionStateMismatchCategory.LegacyConnectedWithoutMinimumEvidence,
            comparison.mismatchCategory,
        )
        assertFalse(comparison.hasMinimumConnectedEvidence)
    }

    @Test
    fun `vpn interface alone cannot become debug connected`() {
        val evidence = ConnectionEvidence(
            vpnInterfaceEstablished = true,
        )
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Connected,
            shadowState = TunnelStatus.Connected.toConnectionState(
                evidence = evidence,
            ),
            shadowEvidence = evidence,
        )

        assertEquals(
            ConnectionStateDebugCategory.Failed,
            comparison.connectionStateCategory,
        )
        assertEquals(
            ConnectionStateMismatchCategory.LegacyConnectedWithoutMinimumEvidence,
            comparison.mismatchCategory,
        )
        assertFalse(comparison.hasMinimumConnectedEvidence)
    }

    @Test
    fun `shadow connected while legacy is not connected is categorized`() {
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Idle,
            shadowState = ConnectionState.Connected(minimumEvidence()),
        )

        assertEquals(
            ConnectionStateMismatchCategory.ShadowConnectedLegacyNotConnected,
            comparison.mismatchCategory,
        )
    }

    @Test
    fun `legacy failed while shadow still connects is categorized`() {
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Error,
            shadowState = ConnectionState.Connecting,
        )

        assertEquals(
            ConnectionStateMismatchCategory.LegacyFailedNewConnecting,
            comparison.mismatchCategory,
        )
    }

    @Test
    fun `reconnect and disconnect clear stale comparison state`() {
        val runtime = ConnectionStateShadowRuntime().apply {
            startWithPreparedPermission()
            markVpnInterfaceEstablished()
            markEngineStarted()
            publishStatus(TunnelStatus.Connected)
        }

        assertEquals(
            ConnectionStateMismatchCategory.None,
            ConnectionStateDebugComparison.from(
                legacyStatus = TunnelStatus.Connected,
                shadowState = runtime.state,
                shadowEvidence = runtime.evidence,
            ).mismatchCategory,
        )

        runtime.publishStatus(TunnelStatus.Reconnecting)
        assertEquals(
            ConnectionStateMismatchCategory.None,
            ConnectionStateDebugComparison.from(
                legacyStatus = TunnelStatus.Reconnecting,
                shadowState = runtime.state,
                shadowEvidence = runtime.evidence,
            ).mismatchCategory,
        )

        runtime.publishStatus(TunnelStatus.Stopping)
        assertEquals(
            ConnectionStateMismatchCategory.None,
            ConnectionStateDebugComparison.from(
                legacyStatus = TunnelStatus.Stopping,
                shadowState = runtime.state,
                shadowEvidence = runtime.evidence,
            ).mismatchCategory,
        )
    }

    @Test
    fun `comparison is debug only and not release visible`() {
        val comparison = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Idle,
            shadowState = ConnectionState.Idle,
        )

        assertTrue(comparison.isDebugOnly)
        assertFalse(comparison.isReleaseVisible)
        assertEquals("true", comparison.toDebugFields().getValue("is_debug_only"))
        assertEquals("false", comparison.toDebugFields().getValue("is_release_visible"))
    }

    @Test
    fun `debug comparison output is redaction safe`() {
        val rendered = ConnectionStateDebugComparison.from(
            legacyStatus = TunnelStatus.Connected,
            shadowState = ConnectionState.Connected(minimumEvidence()),
        ).toDebugFields().entries.joinToString(separator = "\n") {
            "${it.key}: ${it.value}"
        }
        val forbiddenTokens = listOf(
            "raw_uri",
            "uuid",
            "server_ip",
            "server",
            "host",
            "domain",
            "port",
            "subscription_url",
            "token",
            "password",
            "private_key",
            "raw_diagnostics",
            "device_dump",
            "lower_layer_exception",
        )

        forbiddenTokens.forEach { token ->
            assertFalse(rendered.contains(token, ignoreCase = true))
        }
    }

    private fun minimumEvidence(): ConnectionEvidence =
        ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
        )
}
