package com.gmvpn.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.tunnel.TunnelStatus
import uniffi.gmvpn_ffi.FfiSubscriptionFormat

/** Snapshot view-model fed in from MainActivity. */
data class HomeUiState(
    val status: TunnelStatus,
    val lastError: String?,
    val library: List<String>,
    val activeIndex: Int,
    val activeUri: String?,
    val subscriptionMessage: String?,
    val subscriptionInFlight: Boolean,
    val latencies: Map<Int, LatencyState> = emptyMap(),
)

/** Pure callback bag; keeps Compose stateless. */
data class HomeActions(
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
    val onAddUri: (String) -> Unit,
    val onSelectProfile: (Int) -> Unit,
    val onRemoveProfile: (Int) -> Unit,
    val onClearLibrary: () -> Unit,
    val onFetchSubscription: (url: String, format: FfiSubscriptionFormat) -> Unit,
    val onAlwaysOn: () -> Unit,
    val onAbout: () -> Unit,
    val onPerAppRouting: () -> Unit,
    val onTestProfile: (Int) -> Unit,
    val onTestAllProfiles: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { IconButton(onClick = actions.onAbout) { Text("ⓘ") } },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(text = stringResource(state.status.labelRes))
            Spacer(Modifier.height(16.dp))
            ConnectButton(state, actions)
            Spacer(Modifier.height(24.dp))

            ProfileEditor(activeUri = state.activeUri, onAdd = actions.onAddUri)

            Spacer(Modifier.height(16.dp))
            LibraryCard(
                library = state.library,
                activeIndex = state.activeIndex,
                latencies = state.latencies,
                onSelect = actions.onSelectProfile,
                onRemove = actions.onRemoveProfile,
                onClear = actions.onClearLibrary,
                onTest = actions.onTestProfile,
                onTestAll = actions.onTestAllProfiles,
            )

            Spacer(Modifier.height(16.dp))
            SubscriptionCard(
                inFlight = state.subscriptionInFlight,
                message = state.subscriptionMessage,
                onFetch = actions.onFetchSubscription,
            )

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.routing_card_explainer))
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = actions.onPerAppRouting) {
                        Text(stringResource(R.string.routing_card_action))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AlwaysOnHint(onClick = actions.onAlwaysOn)

            if (!state.lastError.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.status_error))
                        Spacer(Modifier.height(4.dp))
                        Text(state.lastError)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectButton(state: HomeUiState, actions: HomeActions) {
    when (state.status) {
        TunnelStatus.Idle, TunnelStatus.Error -> Button(
            onClick = actions.onConnect,
            enabled = !state.activeUri.isNullOrBlank(),
        ) { Text(stringResource(R.string.action_connect)) }
        TunnelStatus.Connected,
        TunnelStatus.Starting,
        TunnelStatus.Reconnecting,
        TunnelStatus.Preparing,
        TunnelStatus.Stopping -> OutlinedButton(onClick = actions.onDisconnect) {
            Text(stringResource(R.string.action_disconnect))
        }
    }
}

@Composable
private fun ProfileEditor(activeUri: String?, onAdd: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
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
            Button(
                onClick = {
                    onAdd(draft.trim())
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@Composable
private fun LibraryCard(
    library: List<String>,
    activeIndex: Int,
    latencies: Map<Int, LatencyState>,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onTest: (Int) -> Unit,
    onTestAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_header),
                    modifier = Modifier.weight(1f),
                )
                if (library.isNotEmpty()) {
                    OutlinedButton(onClick = onTestAll) {
                        Text(stringResource(R.string.action_test_all))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (library.isEmpty()) {
                Text(stringResource(R.string.library_empty))
            } else {
                library.forEachIndexed { index, uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = index == activeIndex,
                            onClick = { onSelect(index) },
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        ) {
                            Text(text = profileLabel(uri))
                            Text(
                                text = latencyLabel(latencies[index]),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { onTest(index) }) {
                            Text(stringResource(R.string.action_test))
                        }
                        TextButton(onClick = { onRemove(index) }) {
                            Text(stringResource(R.string.action_remove))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClear) {
                    Text(stringResource(R.string.action_clear))
                }
            }
        }
    }
}

@Composable
private fun latencyLabel(state: LatencyState?): String = when (state) {
    null, LatencyState.Idle -> stringResource(R.string.latency_idle)
    LatencyState.InFlight -> stringResource(R.string.latency_inflight)
    is LatencyState.Result -> state.ms?.let {
        stringResource(R.string.latency_value, it)
    } ?: stringResource(R.string.latency_unreachable)
}

@Composable
private fun SubscriptionCard(
    inFlight: Boolean,
    message: String?,
    onFetch: (String, FfiSubscriptionFormat) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(FfiSubscriptionFormat.Base64UriList) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.subscription_header))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.subscription_url_label)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
            )
            Spacer(Modifier.height(8.dp))
            FormatPicker(selected = format, onChange = { format = it })
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.subscription_replaces_library))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onFetch(url.trim(), format) },
                enabled = !inFlight && url.isNotBlank(),
            ) {
                Text(
                    if (inFlight) stringResource(R.string.subscription_fetching)
                    else stringResource(R.string.action_fetch),
                )
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}

@Composable
private fun FormatPicker(
    selected: FfiSubscriptionFormat,
    onChange: (FfiSubscriptionFormat) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.subscription_format_label) + ": " + selected.label())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FfiSubscriptionFormat.values().forEach { fmt ->
                DropdownMenuItem(
                    text = { Text(fmt.label()) },
                    onClick = {
                        onChange(fmt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AlwaysOnHint(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.always_on_explainer))
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClick) {
                Text(stringResource(R.string.action_always_on))
            }
        }
    }
}

@Composable
private fun FfiSubscriptionFormat.label(): String = when (this) {
    FfiSubscriptionFormat.UriList -> stringResource(R.string.subscription_format_uri_list)
    FfiSubscriptionFormat.Base64UriList -> stringResource(R.string.subscription_format_base64)
    FfiSubscriptionFormat.Sip008 -> stringResource(R.string.subscription_format_sip008)
}

private fun profileLabel(uri: String): String {
    val maxChars = 80
    val scheme = uri.substringBefore("://", missingDelimiterValue = "")
    val host = uri.substringAfter("://", "").substringBefore('?').substringBefore('#')
    val tail = host.takeIf { it.isNotEmpty() } ?: uri
    val fragment = uri.substringAfter('#', missingDelimiterValue = "")
    val label = if (fragment.isNotBlank()) "$scheme · $fragment" else "$scheme · $tail"
    return if (label.length <= maxChars) label else label.take(maxChars - 1) + "…"
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
