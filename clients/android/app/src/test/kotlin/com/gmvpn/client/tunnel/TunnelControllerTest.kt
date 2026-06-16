package com.gmvpn.client.tunnel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TunnelControllerTest {

    @Test
    fun `permission cancel returns preparing to idle without connected state`() {
        TunnelController.resetForTest(status = TunnelStatus.Preparing)

        TunnelController.onPermissionDenied()

        assertEquals(TunnelStatus.Idle, TunnelController.status.value)
        assertNull(TunnelController.lastError.value)
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
    }
}
