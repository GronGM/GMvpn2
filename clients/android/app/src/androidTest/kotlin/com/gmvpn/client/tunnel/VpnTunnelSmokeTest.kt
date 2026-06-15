package com.gmvpn.client.tunnel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmvpn.client.R
import com.gmvpn.client.profile.ProfileStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator/device smoke tests for the Android tunnel lifecycle. They
 * verify safety boundaries and typed failure states; they do not claim
 * a working VPN tunnel or fake a successful engine connection.
 */
@RunWith(AndroidJUnit4::class)
class VpnTunnelSmokeTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun vpnServiceManifestIsPrivateAndProtected() {
        val service = context.packageManager.getServiceInfo(
            ComponentName(context, GmvpnVpnService::class.java),
            0,
        )

        assertFalse("VpnService must not be exported", service.exported)
        assertEquals("android.permission.BIND_VPN_SERVICE", service.permission)

        val matches = context.packageManager.queryIntentServices(
            Intent(VpnService.SERVICE_INTERFACE).setPackage(context.packageName),
            0,
        )
        assertTrue(
            "manifest must expose the Android VpnService intent filter",
            matches.any { it.serviceInfo.name == GmvpnVpnService::class.java.name },
        )
    }

    @Test
    fun vpnPermissionBoundaryReturnsSystemIntentOrAlreadyGranted() {
        val permissionIntent = VpnService.prepare(context)

        permissionIntent ?: return
        assertTrue(
            "system permission intent must identify a permission UI",
            permissionIntent.action != null || permissionIntent.component != null,
        )
    }

    @Test
    fun engineBridgeReportsBundledVersionOrTypedUnavailable() {
        val bridge = EngineBridge()
        val version = bridge.xrayVersionOrNull()

        if (version != null) {
            assertTrue(
                "XrayVersion must be non-empty when gmvpn.aar is bundled",
                version.isNotBlank(),
            )
            return
        }

        try {
            bridge.start(
                configJson = "{}",
                tunFd = -1,
                mtu = 1500,
                socksPort = 10808,
            ) { _, _ -> }
            fail("missing gmvpn.aar must not look like a successful engine start")
        } catch (e: EngineUnavailableException) {
            assertTrue(
                "missing engine should surface as EngineUnavailableException",
                e.message?.contains("engine class") == true,
            )
        } finally {
            bridge.stop()
        }
    }

    @Test
    fun disconnectWhileIdleDoesNotCrash() {
        TunnelController.requestStop(context)

        eventually("idle disconnect returns to Idle") {
            TunnelController.status.value == TunnelStatus.Idle
        }
    }

    @Test
    fun connectWithoutActiveProfileReachesUserVisibleError() {
        runBlocking {
            ProfileStore(context).clearAll()
        }

        TunnelController.requestStart(context)

        val expected = context.getString(R.string.profile_missing_body)
        eventually("no active profile is reported") {
            TunnelController.lastError.value == expected
        }
        assertTrue(
            "no-profile start must not become Connected",
            TunnelController.status.value != TunnelStatus.Connected,
        )

        TunnelController.requestStop(context)
        eventually("cleanup after no-profile start returns to Idle") {
            TunnelController.status.value == TunnelStatus.Idle
        }
    }

    private fun eventually(
        description: String,
        timeoutMs: Long = 8_000,
        predicate: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        fail(description)
    }
}
