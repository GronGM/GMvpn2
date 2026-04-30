package com.gmvpn.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gmvpn.client.R
import com.gmvpn.client.tunnel.TunnelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    status: TunnelStatus,
    lastError: String?,
    activeUri: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSaveProfile: (String) -> Unit,
    onClearProfile: () -> Unit,
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
                TunnelStatus.Idle, TunnelStatus.Error -> Button(
                    onClick = onConnectClick,
                    enabled = !activeUri.isNullOrBlank(),
                ) {
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

            ProfileEditor(
                activeUri = activeUri,
                onSave = onSaveProfile,
                onClear = onClearProfile,
            )

            if (!lastError.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.status_error))
                        Spacer(Modifier.height(4.dp))
                        Text(lastError)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    activeUri: String?,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var draft by remember(activeUri) { mutableStateOf(activeUri.orEmpty()) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (activeUri.isNullOrBlank()) {
                    stringResource(R.string.profile_missing)
                } else {
                    stringResource(R.string.profile_active)
                },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.profile_uri_label)) },
                placeholder = { Text(stringResource(R.string.profile_uri_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { onSave(draft.trim()) },
                    enabled = draft.isNotBlank() && draft.trim() != activeUri,
                ) { Text(stringResource(R.string.action_save)) }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        draft = ""
                        onClear()
                    },
                    enabled = !activeUri.isNullOrBlank(),
                ) { Text(stringResource(R.string.action_clear)) }
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
