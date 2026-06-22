package com.gmvpn.client.diagnostics

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import com.gmvpn.client.connection.TrafficProbeEvidence
import com.gmvpn.client.tunnel.TunnelStatus
import com.gmvpn.client.tunnel.toConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConnectionStateDiagnosticsPreviewTest {

    @Test
    fun `preview serializes only typed diagnostic fields`() {
        val preview = ConnectionStateDiagnosticsPreview.from(
            state = ConnectionState.Connecting,
            evidence = ConnectionEvidence(
                vpnPermissionPrepared = true,
                vpnInterfaceEstablished = false,
                engineStarted = true,
                androidVpnNetworkVisible = false,
            ),
        )

        assertEquals(
            mapOf(
                "connection_state_category" to "Connecting",
                "failure_category" to "none",
                "has_vpn_permission_evidence" to "true",
                "has_vpn_interface_evidence" to "false",
                "has_engine_started_evidence" to "true",
                "has_immediate_failure" to "false",
                "android_vpn_network_visible" to "false",
                "traffic_probe_status" to "NotRun",
            ),
            preview.toDiagnosticFields(),
        )
    }

    @Test
    fun `preview schema omits raw profile and endpoint fields`() {
        val rendered = ConnectionStateDiagnosticsPreview.from(
            state = ConnectionState.Failed(
                failure = ConnectionFailure(
                    category = ConnectionFailureCategory.ProfileParseFailed,
                ),
            ),
        ).toDiagnosticFields().entries.joinToString(separator = "\n") {
            "${it.key}: ${it.value}"
        }
        val forbiddenSchemaTokens = listOf(
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

        forbiddenSchemaTokens.forEach { token ->
            assertFalse(rendered.contains(token, ignoreCase = true))
        }
    }

    @Test
    fun `failure category is serialized as category only`() {
        val preview = ConnectionStateDiagnosticsPreview.from(
            state = ConnectionState.Failed(
                failure = ConnectionFailure(
                    category = ConnectionFailureCategory.EngineStartFailed,
                ),
            ),
        )

        assertEquals(
            "Failed",
            preview.toDiagnosticFields().getValue("connection_state_category"),
        )
        assertEquals(
            "EngineStartFailed",
            preview.toDiagnosticFields().getValue("failure_category"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("has_immediate_failure"),
        )
    }

    @Test
    fun `engine started alone cannot produce connected preview`() {
        val evidence = ConnectionEvidence(
            engineStarted = true,
        )
        val state = TunnelStatus.Connected.toConnectionState(
            evidence = evidence,
        )

        val preview = ConnectionStateDiagnosticsPreview.from(
            state = state,
            evidence = evidence,
        )

        assertNotEquals(
            "Connected",
            preview.toDiagnosticFields().getValue("connection_state_category"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("has_engine_started_evidence"),
        )
        assertEquals(
            "false",
            preview.toDiagnosticFields().getValue("has_vpn_interface_evidence"),
        )
    }

    @Test
    fun `permission interface and engine evidence are boolean only`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
            androidVpnNetworkVisible = true,
        )
        val preview = ConnectionStateDiagnosticsPreview.from(
            state = ConnectionState.Connected(evidence),
        )

        assertEquals(
            "Connected",
            preview.toDiagnosticFields().getValue("connection_state_category"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("has_vpn_permission_evidence"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("has_vpn_interface_evidence"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("has_engine_started_evidence"),
        )
        assertEquals(
            "true",
            preview.toDiagnosticFields().getValue("android_vpn_network_visible"),
        )
    }

    @Test
    fun `traffic probe status is represented as typed status only`() {
        val preview = ConnectionStateDiagnosticsPreview.from(
            state = ConnectionState.Connecting,
            evidence = ConnectionEvidence(
                trafficProbe = TrafficProbeEvidence.Failed(
                    category = ConnectionFailureCategory.DnsFailure,
                ),
            ),
        )

        assertEquals(
            "Failed",
            preview.toDiagnosticFields().getValue("traffic_probe_status"),
        )
        assertEquals(
            "none",
            preview.toDiagnosticFields().getValue("failure_category"),
        )
    }
}
