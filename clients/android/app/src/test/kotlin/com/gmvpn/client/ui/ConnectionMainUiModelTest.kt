package com.gmvpn.client.ui

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionMainUiModelTest {

    @Test
    fun `idle with profile renders disconnected connect action`() {
        val model = ConnectionState.Idle.toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = false,
        )

        assertEquals(ConnectionMainUiCategory.Disconnected, model.category)
        assertEquals(ConnectionMainAction.Connect, model.action)
        assertTrue(model.actionEnabled)
        assertFalse(model.rendersConnected)
    }

    @Test
    fun `missing profile renders disabled connect without connected state`() {
        val model = ConnectionState.Idle.toMainUiModel(
            hasProfile = false,
            hasUserVisibleError = false,
        )

        assertEquals(ConnectionMainUiCategory.NeedsProfile, model.category)
        assertEquals(ConnectionMainAction.Connect, model.action)
        assertFalse(model.actionEnabled)
        assertFalse(model.rendersConnected)
    }

    @Test
    fun `connecting states render disabled connecting action`() {
        val states = listOf(
            ConnectionState.Preparing,
            ConnectionState.StartingVpnService,
            ConnectionState.StartingEngine,
            ConnectionState.Connecting,
        )

        states.forEach { state ->
            val model = state.toMainUiModel(
                hasProfile = true,
                hasUserVisibleError = false,
            )

            assertEquals(ConnectionMainUiCategory.Connecting, model.category)
            assertEquals(ConnectionMainAction.Connecting, model.action)
            assertFalse(model.actionEnabled)
            assertFalse(model.rendersConnected)
        }
    }

    @Test
    fun `connected renders disconnect only from safe connection state`() {
        val model = ConnectionState.Connected(
            evidence = ConnectionEvidence(
                vpnPermissionPrepared = true,
                vpnInterfaceEstablished = true,
                engineStarted = true,
            ),
        ).toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = false,
        )

        assertEquals(ConnectionMainUiCategory.Connected, model.category)
        assertEquals(ConnectionMainAction.Disconnect, model.action)
        assertTrue(model.actionEnabled)
        assertTrue(model.rendersConnected)
    }

    @Test
    fun `disconnecting cleanup does not render connected`() {
        val model = ConnectionState.Disconnecting.toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = false,
        )

        assertEquals(ConnectionMainUiCategory.Disconnecting, model.category)
        assertEquals(ConnectionMainAction.Disconnecting, model.action)
        assertFalse(model.actionEnabled)
        assertFalse(model.rendersConnected)
    }

    @Test
    fun `failure does not render connected or disconnect action`() {
        val model = ConnectionState.Failed(
            failure = ConnectionFailure(
                category = ConnectionFailureCategory.VpnInterfaceNotEstablished,
            ),
        ).toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = false,
        )

        assertEquals(ConnectionMainUiCategory.Error, model.category)
        assertEquals(ConnectionMainAction.Retry, model.action)
        assertTrue(model.actionEnabled)
        assertFalse(model.rendersConnected)
    }

    @Test
    fun `user visible error wins over connected model`() {
        val model = ConnectionState.Connected(
            evidence = ConnectionEvidence(
                vpnPermissionPrepared = true,
                vpnInterfaceEstablished = true,
                engineStarted = true,
            ),
        ).toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = true,
        )

        assertEquals(ConnectionMainUiCategory.Error, model.category)
        assertEquals(ConnectionMainAction.Retry, model.action)
        assertFalse(model.rendersConnected)
    }

    @Test
    fun `safe diagnostics do not include profile or endpoint material`() {
        val forbidden = listOf(
            "raw_profile_marker",
            "subscription_marker",
            "server_address_marker",
            "server_port_marker",
            "credential_marker",
            "identifier_marker",
        )
        val diagnostics = ConnectionState.Connected(
            evidence = ConnectionEvidence(
                vpnPermissionPrepared = true,
                vpnInterfaceEstablished = true,
                engineStarted = true,
            ),
        ).toMainUiModel(
            hasProfile = true,
            hasUserVisibleError = false,
        ).safeDiagnosticFields().toString()

        forbidden.forEach { value ->
            assertFalse(
                "diagnostics must not include $value",
                diagnostics.contains(value, ignoreCase = true),
            )
        }
    }
}
