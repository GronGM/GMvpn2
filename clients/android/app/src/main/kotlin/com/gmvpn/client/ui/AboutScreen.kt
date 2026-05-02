package com.gmvpn.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmvpn.client.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String,
    coreVersion: String,
    xrayVersion: String,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.action_about)) }) },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.about_app_version, appVersion, coreVersion))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.about_xray_version, xrayVersion))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about_no_telemetry))
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.about_licenses_header),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "• Xray-core (XTLS) — Mozilla Public License 2.0\n" +
                    "• gVisor netstack (Google) — Apache License 2.0\n" +
                    "• UniFFI (Mozilla) — Mozilla Public License 2.0\n" +
                    "• JNA (Java Native Access) — Apache License 2.0\n" +
                    "• AndroidX Compose / DataStore / Lifecycle — Apache License 2.0\n" +
                    "• Kotlin standard library / Coroutines (JetBrains) — Apache License 2.0\n" +
                    "• gomobile (Go authors) — BSD-style",
            )
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.about_privacy_link))
        }
    }
}
