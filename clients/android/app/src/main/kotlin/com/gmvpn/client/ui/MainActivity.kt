package com.gmvpn.client.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gmvpn.client.BuildConfig
import com.gmvpn.client.R
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.tunnel.TunnelController
import com.gmvpn.client.ui.theme.GmvpnTheme
import kotlinx.coroutines.launch
import uniffi.gmvpn_ffi.coreVersion

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            TunnelController.onPermissionGranted(this)
        }
    }

    private lateinit var profileStore: ProfileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileStore = ProfileStore(applicationContext)
        setContent {
            GmvpnTheme {
                var showAbout by remember { mutableStateOf(false) }
                val status by TunnelController.status.collectAsStateWithLifecycle()
                val lastError by TunnelController.lastError.collectAsStateWithLifecycle()
                val activeUri by profileStore.activeUri
                    .collectAsStateWithLifecycle(initialValue = null)

                if (showAbout) {
                    AboutScreen(
                        appVersion = BuildConfig.VERSION_NAME,
                        coreVersion = coreVersion(),
                        xrayVersion = engineXrayVersion(),
                    )
                } else {
                    HomeScreen(
                        status = status,
                        lastError = lastError,
                        activeUri = activeUri,
                        onConnectClick = ::handleConnect,
                        onDisconnectClick = { TunnelController.requestStop(this) },
                        onSaveProfile = { uri ->
                            lifecycleScope.launch { profileStore.setActiveUri(uri) }
                        },
                        onClearProfile = {
                            lifecycleScope.launch { profileStore.clear() }
                        },
                        onAlwaysOnClick = ::openAlwaysOnSettings,
                        onAboutClick = { showAbout = true },
                    )
                }
            }
        }
    }

    private fun handleConnect() {
        val intent: Intent? = TunnelController.preparePermission(this)
        if (intent == null) {
            TunnelController.requestStart(this)
        } else {
            vpnPermissionLauncher.launch(intent)
        }
    }

    private fun openAlwaysOnSettings() {
        val candidates = listOf(
            // Most-specific first; falls back through global pages if a
            // specific one is missing on the device.
            Intent("android.net.vpn.SETTINGS"),
            Intent(Settings.ACTION_VPN_SETTINGS),
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try next.
            }
        }
        Toast.makeText(this, R.string.always_on_unavailable, Toast.LENGTH_LONG).show()
    }

    /**
     * Pulls the pinned Xray-core version from the engine without
     * coupling MainActivity to the gomobile classes — if the engine
     * `.aar` is missing the call returns "unbundled".
     */
    private fun engineXrayVersion(): String = try {
        val cls = Class.forName("com.gmvpn.core.gmvpn.Gmvpn")
        val method = cls.methods.firstOrNull { it.name == "xrayVersion" && it.parameterCount == 0 }
        (method?.invoke(null) as? String) ?: "unbundled"
    } catch (_: Throwable) {
        "unbundled"
    }
}
