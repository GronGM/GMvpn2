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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.gmvpn.client.routing.InstalledApp
import com.gmvpn.client.routing.PerAppMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppRoutingScreen(
    mode: PerAppMode,
    selected: Set<String>,
    apps: List<InstalledApp>,
    appsLoading: Boolean,
    onModeChange: (PerAppMode) -> Unit,
    onTogglePackage: (String) -> Unit,
    onClearSelection: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routing_title)) }) },
    ) { padding: PaddingValues ->
        var query by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            ModePicker(mode, onModeChange)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.routing_self_note),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.routing_search_label)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                )
                Spacer(Modifier.padding(end = 8.dp))
                OutlinedButton(
                    onClick = onClearSelection,
                    enabled = selected.isNotEmpty() && mode != PerAppMode.Off,
                ) { Text(stringResource(R.string.routing_clear_selection)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.routing_selection_summary,
                    selected.size,
                    apps.size,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))

            if (appsLoading && apps.isEmpty()) {
                Text(stringResource(R.string.routing_loading))
            } else {
                val filtered = remember(apps, query) {
                    if (query.isBlank()) apps
                    else apps.filter {
                        it.label.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in selected,
                            enabled = mode != PerAppMode.Off,
                            onToggle = { onTogglePackage(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePicker(mode: PerAppMode, onModeChange: (PerAppMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.routing_mode_header),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            ModeRow(
                selected = mode == PerAppMode.Off,
                title = stringResource(R.string.routing_mode_off),
                subtitle = stringResource(R.string.routing_mode_off_desc),
                onClick = { onModeChange(PerAppMode.Off) },
            )
            ModeRow(
                selected = mode == PerAppMode.IncludeOnly,
                title = stringResource(R.string.routing_mode_include),
                subtitle = stringResource(R.string.routing_mode_include_desc),
                onClick = { onModeChange(PerAppMode.IncludeOnly) },
            )
            ModeRow(
                selected = mode == PerAppMode.ExcludeListed,
                title = stringResource(R.string.routing_mode_exclude),
                subtitle = stringResource(R.string.routing_mode_exclude_desc),
                onClick = { onModeChange(PerAppMode.ExcludeListed) },
            )
        }
    }
}

@Composable
private fun ModeRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 8.dp)) {
            Text(text = title)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(text = app.label)
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
