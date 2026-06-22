package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TunnelStatusConnectionStateMapperTest {

    @Test
    fun `idle maps to idle`() {
        assertSame(
            ConnectionState.Idle,
            TunnelStatus.Idle.toConnectionState(),
        )
    }

    @Test
    fun `preparing maps to preparing`() {
        assertSame(
            ConnectionState.Preparing,
            TunnelStatus.Preparing.toConnectionState(),
        )
    }

    @Test
    fun `starting maps to starting engine`() {
        assertSame(
            ConnectionState.StartingEngine,
            TunnelStatus.Starting.toConnectionState(),
        )
    }

    @Test
    fun `reconnecting maps to connecting`() {
        assertSame(
            ConnectionState.Connecting,
            TunnelStatus.Reconnecting.toConnectionState(),
        )
    }

    @Test
    fun `stopping maps to disconnecting`() {
        assertSame(
            ConnectionState.Disconnecting,
            TunnelStatus.Stopping.toConnectionState(),
        )
    }

    @Test
    fun `error maps to unknown failure by default`() {
        val state = TunnelStatus.Error.toConnectionState()

        assertEquals(
            ConnectionFailureCategory.Unknown,
            (state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `error preserves supplied typed failure`() {
        val failure = ConnectionFailure(
            category = ConnectionFailureCategory.EngineStartFailed,
        )

        val state = TunnelStatus.Error.toConnectionState(
            failure = failure,
        )

        assertEquals(
            failure,
            (state as ConnectionState.Failed).failure,
        )
    }

    @Test
    fun `connected with minimum evidence maps to connected`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
        )

        val state = TunnelStatus.Connected.toConnectionState(
            evidence = evidence,
        )

        assertEquals(
            evidence,
            (state as ConnectionState.Connected).evidence,
        )
    }

    @Test
    fun `connected without evidence fails conservatively`() {
        val state = TunnelStatus.Connected.toConnectionState()

        assertEquals(
            ConnectionFailureCategory.VpnInterfaceNotEstablished,
            (state as ConnectionState.Failed).failure.category,
        )
    }

    @Test
    fun `connected with immediate failure does not fake connected`() {
        val evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
            vpnInterfaceEstablished = true,
            engineStarted = true,
            immediateFailure = ConnectionFailure(
                category = ConnectionFailureCategory.EngineStartFailed,
            ),
        )

        val state = TunnelStatus.Connected.toConnectionState(
            evidence = evidence,
        )

        assertEquals(
            ConnectionFailureCategory.VpnInterfaceNotEstablished,
            (state as ConnectionState.Failed).failure.category,
        )
    }
}
