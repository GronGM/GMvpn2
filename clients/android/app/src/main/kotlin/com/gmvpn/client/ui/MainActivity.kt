package com.gmvpn.client.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.tunnel.TunnelController
import com.gmvpn.client.ui.theme.GmvpnTheme
import kotlinx.coroutines.launch

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
                val status by TunnelController.status.collectAsStateWithLifecycle()
                val lastError by TunnelController.lastError.collectAsStateWithLifecycle()
                val activeUri by profileStore.activeUri
                    .collectAsStateWithLifecycle(initialValue = null)

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
                )
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
}
