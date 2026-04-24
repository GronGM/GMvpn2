package com.gmvpn.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gmvpn.client.R
import com.gmvpn.client.tunnel.TunnelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    status: TunnelStatus,
    lastError: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(text = stringResource(status.labelRes))
            Spacer(Modifier.height(16.dp))

            when (status) {
                TunnelStatus.Idle, TunnelStatus.Error -> Button(onClick = onConnectClick) {
                    Text(stringResource(R.string.action_connect))
                }
                TunnelStatus.Connected,
                TunnelStatus.Starting,
                TunnelStatus.Reconnecting,
                TunnelStatus.Preparing,
                TunnelStatus.Stopping -> OutlinedButton(onClick = onDisconnectClick) {
                    Text(stringResource(R.string.action_disconnect))
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.engine_missing_headline))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.engine_missing_body))
                    if (!lastError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(lastError)
                    }
                }
            }
        }
    }
}

private val TunnelStatus.labelRes: Int
    get() = when (this) {
        TunnelStatus.Idle -> R.string.status_idle
        TunnelStatus.Preparing -> R.string.status_preparing
        TunnelStatus.Starting -> R.string.status_starting
        TunnelStatus.Connected -> R.string.status_connected
        TunnelStatus.Reconnecting -> R.string.status_reconnecting
        TunnelStatus.Stopping -> R.string.status_stopping
        TunnelStatus.Error -> R.string.status_error
    }
