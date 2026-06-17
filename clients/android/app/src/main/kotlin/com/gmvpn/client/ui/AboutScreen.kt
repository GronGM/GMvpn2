package com.gmvpn.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.gmvpn.client.R
import com.gmvpn.client.ui.components.GmCard
import com.gmvpn.client.ui.components.GmCardTone
import com.gmvpn.client.ui.components.PrivacyNotice
import com.gmvpn.client.ui.theme.GmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String,
    coreVersion: String,
    xrayVersion: String,
    diagnosticsMessage: String?,
    includeDeviceInDiagnostics: Boolean,
    onIncludeDeviceInDiagnosticsChange: (Boolean) -> Unit,
    onCopyDiagnostics: () -> Unit,
    onExportDiagnostics: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_about)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = GmSpacing.lg, vertical = GmSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                GmSpacing.md,
            ),
        ) {
            GmCard(modifier = Modifier.fillMaxWidth(), tone = GmCardTone.Selected) {
                Text(
                    text = stringResource(R.string.about_product_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.about_app_version, appVersion, coreVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_xray_version, xrayVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrivacyNotice(
                title = stringResource(R.string.privacy_notice_title),
                body = stringResource(R.string.about_no_telemetry),
                modifier = Modifier.fillMaxWidth(),
            )

            GmCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.diagnostics_header),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.diagnostics_privacy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeDeviceInDiagnostics,
                        onCheckedChange = onIncludeDeviceInDiagnosticsChange,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_include_device),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(onClick = onCopyDiagnostics, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_copy_bug_report))
                }
                OutlinedButton(onClick = onExportDiagnostics, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_export_bug_report))
                }
                Text(
                    text = stringResource(R.string.diagnostics_review_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!diagnosticsMessage.isNullOrBlank()) {
                    Text(
                        text = diagnosticsMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GmCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.about_licenses_header),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(GmSpacing.xs))
                Text(
                    "• Xray-core (XTLS) — Mozilla Public License 2.0\n" +
                        "• gVisor netstack (Google) — Apache License 2.0\n" +
                        "• UniFFI (Mozilla) — Mozilla Public License 2.0\n" +
                        "• JNA (Java Native Access) — Apache License 2.0\n" +
                        "• AndroidX Compose / DataStore / Lifecycle — Apache License 2.0\n" +
                        "• Kotlin standard library / Coroutines (JetBrains) — Apache License 2.0\n" +
                        "• gomobile (Go authors) — BSD-style",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_privacy_link),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
