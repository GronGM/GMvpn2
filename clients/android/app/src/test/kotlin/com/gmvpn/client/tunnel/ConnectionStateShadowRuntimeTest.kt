package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import com.gmvpn.client.connection.RoutingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateShadowRuntimeTest {

    @Test
    fun `engine started without vpn interface cannot produce connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()
        runtime.markEngineStarted()

        runtime.publishStatus(TunnelStatus.Connected)

        assertNotConnected(runtime.state)
        assertEquals(
            ConnectionFailureCategory.VpnInterfaceNotEstablished,
            (runtime.state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `vpn interface without engine cannot produce connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()
        runtime.markVpnInterfaceEstablished()

        runtime.publishStatus(TunnelStatus.Connected)

        assertNotConnected(runtime.state)
    }

    @Test
    fun `missing vpn permission cannot produce connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.markVpnInterfaceEstablished()
        runtime.markEngineStarted()

        runtime.publishStatus(TunnelStatus.Connected)

        assertNotConnected(runtime.state)
    }

    @Test
    fun `immediate failure blocks connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()
        runtime.markVpnInterfaceEstablished()
        runtime.markEngineStarted()
        runtime.fail(ConnectionFailureCategory.EngineStartFailed)

        runtime.publishStatus(TunnelStatus.Connected)

        assertNotConnected(runtime.state)
    }

    @Test
    fun `permission denial records typed failure without connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.preparePermission()

        runtime.permissionDenied()

        assertNotConnected(runtime.state)
        assertEquals(
            ConnectionFailureCategory.VpnPermissionDenied,
            (runtime.state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `builder establish failure records typed failure without connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()

        runtime.fail(ConnectionFailureCategory.VpnInterfaceNotEstablished)

        assertNotConnected(runtime.state)
        assertEquals(
            ConnectionFailureCategory.VpnInterfaceNotEstablished,
            (runtime.state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `engine start failure records typed failure without connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()
        runtime.markVpnInterfaceEstablished()

        runtime.fail(ConnectionFailureCategory.EngineStartFailed)

        assertNotConnected(runtime.state)
        assertEquals(
            ConnectionFailureCategory.EngineStartFailed,
            (runtime.state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `profile and config failures block connected`() {
        val categories = listOf(
            ConnectionFailureCategory.NoProfile,
            ConnectionFailureCategory.UnsupportedProfileScheme,
            ConnectionFailureCategory.ProfileParseFailed,
            ConnectionFailureCategory.ConfigBuildFailed,
        )

        categories.forEach { category ->
            val runtime = connectedRuntime()

            runtime.fail(category)
            assertEquals(
                category,
                (runtime.state as ConnectionState.Failed).failure.category,
            )

            runtime.publishStatus(TunnelStatus.Connected)

            assertNotConnected(runtime.state)
            assertEquals(
                category,
                runtime.evidence.immediateFailure?.category,
            )
        }
    }

    @Test
    fun `minimum evidence can produce shadow connected`() {
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()
        runtime.markVpnInterfaceEstablished()
        runtime.markEngineStarted()

        runtime.publishStatus(TunnelStatus.Connected)

        assertTrue(runtime.state is ConnectionState.Connected)
    }

    @Test
    fun `reconnect clears stale connected evidence`() {
        val runtime = connectedRuntime()

        runtime.publishStatus(TunnelStatus.Reconnecting)

        assertEquals(ConnectionState.Connecting, runtime.state)
        assertFalse(runtime.evidence.vpnInterfaceEstablished)
        assertFalse(runtime.evidence.engineStarted)
        assertTrue(runtime.evidence.vpnPermissionPrepared)
    }

    @Test
    fun `disconnect clears shadow evidence`() {
        val runtime = connectedRuntime()

        runtime.publishStatus(TunnelStatus.Stopping)

        assertEquals(ConnectionState.Disconnecting, runtime.state)
        assertFalse(runtime.evidence.vpnPermissionPrepared)
        assertFalse(runtime.evidence.vpnInterfaceEstablished)
        assertFalse(runtime.evidence.engineStarted)

        runtime.publishStatus(TunnelStatus.Idle)

        assertEquals(ConnectionState.Idle, runtime.state)
        assertFalse(runtime.evidence.vpnPermissionPrepared)
    }

    @Test
    fun `per app allow and disallow evidence stays separated from vpn path proof`() {
        val selectedOnly = RoutingMode.SelectedAppsOnly(
            packageNames = setOf("com.example.browser"),
        )
        val allExceptSelected = RoutingMode.AllExceptSelected(
            packageNames = setOf("com.example.browser"),
        )
        val runtime = ConnectionStateShadowRuntime()
        runtime.startWithPreparedPermission()

        runtime.publishStatus(TunnelStatus.Connected)

        assertNotEquals(selectedOnly::class, allExceptSelected::class)
        assertNotConnected(runtime.state)
    }

    @Test
    fun `shadow runtime fields remain redaction safe`() {
        val forbidden = setOf(
            "rawUri",
            "uuid",
            "server",
            "host",
            "domain",
            "port",
            "subscriptionUrl",
            "token",
            "password",
            "privateKey",
        )
        val fieldNames = ConnectionStateShadowRuntime::class
            .java
            .declaredFields
            .map { it.name }
            .toSet()

        assertEquals(emptySet<String>(), fieldNames.intersect(forbidden))
    }

    private fun connectedRuntime(): ConnectionStateShadowRuntime =
        ConnectionStateShadowRuntime().apply {
            startWithPreparedPermission()
            markVpnInterfaceEstablished()
            markEngineStarted()
            publishStatus(TunnelStatus.Connected)
            assertTrue(state is ConnectionState.Connected)
        }

    private fun assertNotConnected(state: ConnectionState) {
        assertFalse(state is ConnectionState.Connected)
    }
}
