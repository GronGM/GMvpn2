package com.gmvpn.client.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmvpn.client.tunnel.TunnelController
import com.gmvpn.client.ui.theme.GmvpnTheme

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            TunnelController.onPermissionGranted(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GmvpnTheme {
                val status by TunnelController.status.collectAsStateWithLifecycle()
                val lastError by TunnelController.lastError.collectAsStateWithLifecycle()

                HomeScreen(
                    status = status,
                    lastError = lastError,
                    onConnectClick = ::handleConnect,
                    onDisconnectClick = { TunnelController.requestStop(this) },
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
