package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TunnelControllerTest {

    @Test
    fun `permission cancel returns preparing to idle without connected state`() {
        TunnelController.resetForTest(status = TunnelStatus.Preparing)

        TunnelController.onPermissionDenied()

        assertEquals(TunnelStatus.Idle, TunnelController.status.value)
        assertNull(TunnelController.lastError.value)
        assertEquals(
            ConnectionFailureCategory.VpnPermissionDenied,
            (
                TunnelController.shadowConnectionStateForTest()
                    as ConnectionState.Failed
                ).failure.category,
        )
    }

    @Test
    fun `permission cancel does not hide an existing error`() {
        TunnelController.resetForTest(
            status = TunnelStatus.Preparing,
            lastError = "previous failure",
        )

        TunnelController.onPermissionDenied()

        assertEquals(TunnelStatus.Idle, TunnelController.status.value)
        assertEquals("previous failure", TunnelController.lastError.value)
        assertFalse(TunnelController.shadowConnectionStateForTest() is ConnectionState.Connected)
    }

    @Test
    fun `error survives idle until dismissed`() {
        TunnelController.resetForTest(
            status = TunnelStatus.Error,
            lastError = "profile URI: unsupported protocol",
        )

        TunnelController.publishStatus(TunnelStatus.Idle)

        assertEquals(TunnelStatus.Idle, TunnelController.status.value)
        assertEquals(
            "profile URI: unsupported protocol",
            TunnelController.lastError.value,
        )

        TunnelController.dismissError()

        assertNull(TunnelController.lastError.value)
    }

    @Test
    fun `successful connection clears stored error`() {
        TunnelController.resetForTest(
            status = TunnelStatus.Error,
            lastError = "profile URI: unsupported protocol",
        )

        TunnelController.publishStatus(TunnelStatus.Connected)

        assertEquals(TunnelStatus.Connected, TunnelController.status.value)
        assertNull(TunnelController.lastError.value)
        assertFalse(TunnelController.shadowConnectionStateForTest() is ConnectionState.Connected)
    }

    @Test
    fun `service evidence can produce shadow connected without changing tunnel status api`() {
        TunnelController.resetForTest()

        TunnelController.startWithPreparedPermissionForTest()
        TunnelController.publishStatus(TunnelStatus.Starting)
        TunnelController.markVpnInterfaceEstablishedForTest()
        TunnelController.markEngineStartedForTest()
        TunnelController.publishStatus(TunnelStatus.Connected)

        assertEquals(TunnelStatus.Connected, TunnelController.status.value)
        assertEquals(
            true,
            TunnelController.shadowConnectionStateForTest() is ConnectionState.Connected,
        )
    }

    @Test
    fun `disconnect clears service shadow evidence`() {
        TunnelController.resetForTest()
        TunnelController.startWithPreparedPermissionForTest()
        TunnelController.publishStatus(TunnelStatus.Starting)
        TunnelController.markVpnInterfaceEstablishedForTest()
        TunnelController.markEngineStartedForTest()
        TunnelController.publishStatus(TunnelStatus.Connected)

        TunnelController.publishStatus(TunnelStatus.Stopping)

        assertEquals(TunnelStatus.Stopping, TunnelController.status.value)
        assertEquals(ConnectionState.Disconnecting, TunnelController.shadowConnectionStateForTest())
        assertFalse(TunnelController.shadowConnectionEvidenceForTest().engineStarted)
        assertFalse(TunnelController.shadowConnectionEvidenceForTest().vpnInterfaceEstablished)
    }
}
