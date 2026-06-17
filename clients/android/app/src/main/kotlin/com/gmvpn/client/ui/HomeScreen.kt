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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gmvpn.client.R
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.profile.ProfileEntry
import com.gmvpn.client.profile.ProfileImportPreview
import com.gmvpn.client.profile.ProfileSource
import com.gmvpn.client.profile.profileDisplaySummary
import com.gmvpn.client.profile.sanitizeCustomProfileName
import com.gmvpn.client.tunnel.TunnelStatus
import com.gmvpn.client.ui.components.ConnectionStatusMark
import com.gmvpn.client.ui.components.GmCard
import com.gmvpn.client.ui.components.GmCardTone
import com.gmvpn.client.ui.components.GmStatusTone
import com.gmvpn.client.ui.components.PremiumConnectButton
import com.gmvpn.client.ui.components.PrivacyNotice
import com.gmvpn.client.ui.components.ProfileListItem
import com.gmvpn.client.ui.theme.GmSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import uniffi.gmvpn_ffi.FfiSubscriptionFormat

/** Snapshot view-model fed in from MainActivity. */
data class HomeUiState(
    val status: TunnelStatus,
    val lastError: String?,
    val profiles: List<ProfileEntry>,
    val activeIndex: Int,
    val activeUri: String?,
    val subscriptionMessage: String?,
    val subscriptionInFlight: Boolean,
    val pendingImport: PendingImport? = null,
    val latencies: Map<Int, LatencyState> = emptyMap(),
    val diagnosticsMessage: String? = null,
)

/**
 * Decoded subscription waiting for user confirmation before replacing
 * the saved library. Held in transient UI state, not persisted.
 */
data class PendingImport(
    val profiles: List<ProfileImportPreview>,
    val warnings: Int,
    val duplicateUris: Int,
)

/** Pure callback bag; keeps Compose stateless. */
data class HomeActions(
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
    val onDismissError: () -> Unit,
    val onCopyDiagnostics: () -> Unit,
    val onAddUri: (String) -> Unit,
    val onSelectProfile: (Int) -> Unit,
    val onRenameProfile: (Int, String) -> Unit,
    val onRemoveProfile: (Int) -> Unit,
    val onClearLibrary: () -> Unit,
    val onFetchSubscription: (url: String, format: FfiSubscriptionFormat) -> Unit,
    val onConfirmImport: () -> Unit,
    val onCancelImport: () -> Unit,
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = actions.onAbout) {
                        Text(stringResource(R.string.action_about))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding: PaddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = GmSpacing.lg, vertical = GmSpacing.md)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(GmSpacing.md),
        ) {
            ConnectionHero(state = state, actions = actions)

            if (!state.lastError.isNullOrBlank()) {
                ErrorBanner(
                    error = state.lastError,
                    diagnosticsMessage = state.diagnosticsMessage,
                    onDismiss = actions.onDismissError,
                    onCopyDiagnostics = actions.onCopyDiagnostics,
                )
            }

            ActiveProfileCard(
                profiles = state.profiles,
                activeIndex = state.activeIndex,
                latencies = state.latencies,
            )

            SecondaryActionsCard(actions = actions)

            SectionHeader(
                title = stringResource(R.string.library_header),
                subtitle = stringResource(R.string.library_section_subtitle),
            )
            LibraryCard(
                profiles = state.profiles,
                activeIndex = state.activeIndex,
                latencies = state.latencies,
                onSelect = actions.onSelectProfile,
                onRename = actions.onRenameProfile,
                onRemove = actions.onRemoveProfile,
                onClear = actions.onClearLibrary,
                onTest = actions.onTestProfile,
                onTestAll = actions.onTestAllProfiles,
            )

            SectionHeader(
                title = stringResource(R.string.subscription_header),
                subtitle = stringResource(R.string.subscription_section_subtitle),
            )
            SubscriptionCard(
                inFlight = state.subscriptionInFlight,
                message = state.subscriptionMessage,
                onFetch = actions.onFetchSubscription,
            )

            ManualProfileCard(onAdd = actions.onAddUri)

            RoutingCard(onClick = actions.onPerAppRouting)

            PrivacyNotice(
                title = stringResource(R.string.privacy_notice_title),
                body = stringResource(R.string.privacy_notice_body),
                modifier = Modifier.fillMaxWidth(),
            )

            AlwaysOnHint(onClick = actions.onAlwaysOn)
        }

        state.pendingImport?.let {
            ConfirmImportDialog(
                pending = it,
                onConfirm = actions.onConfirmImport,
                onCancel = actions.onCancelImport,
            )
        }
    }
}

@Composable
private fun ConnectionHero(state: HomeUiState, actions: HomeActions) {
    val hasProfile = !state.activeUri.isNullOrBlank()
    val tone = connectionTone(state.status, hasProfile, state.lastError)
    val title = connectionTitle(state.status, hasProfile, state.lastError)
    val body = connectionBody(state.status, hasProfile, state.lastError)
    val destructive = state.status in setOf(
        TunnelStatus.Connected,
    )
    val inFlight = state.status in setOf(
        TunnelStatus.Starting,
        TunnelStatus.Reconnecting,
        TunnelStatus.Preparing,
        TunnelStatus.Stopping,
    )
    val buttonText = when {
        state.status == TunnelStatus.Stopping -> stringResource(R.string.action_disconnecting)
        inFlight -> stringResource(R.string.action_connecting)
        destructive -> stringResource(R.string.action_disconnect)
        state.status == TunnelStatus.Error -> stringResource(R.string.action_retry)
        else -> stringResource(R.string.action_connect)
    }

    GmCard(
        modifier = Modifier.fillMaxWidth(),
        tone = when (tone) {
            GmStatusTone.Connected -> GmCardTone.Selected
            GmStatusTone.Error -> GmCardTone.Error
            GmStatusTone.Warning -> GmCardTone.Warning
            else -> GmCardTone.Neutral
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        ) {
            ConnectionStatusMark(tone = tone, text = title)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PremiumConnectButton(
                text = buttonText,
                onClick = if (destructive) actions.onDisconnect else actions.onConnect,
                enabled = !inFlight && (hasProfile || destructive),
                destructive = destructive,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun connectionTitle(
    status: TunnelStatus,
    hasProfile: Boolean,
    lastError: String?,
): String = when {
    !lastError.isNullOrBlank() || status == TunnelStatus.Error ->
        stringResource(R.string.home_status_error_title)
    !hasProfile -> stringResource(R.string.home_status_needs_profile_title)
    status == TunnelStatus.Connected -> stringResource(R.string.home_status_connected_title)
    status in setOf(TunnelStatus.Preparing, TunnelStatus.Starting, TunnelStatus.Reconnecting) ->
        stringResource(R.string.home_status_preparing_title)
    status == TunnelStatus.Stopping -> stringResource(R.string.home_status_stopping_title)
    else -> stringResource(R.string.home_status_disconnected_title)
}

@Composable
private fun connectionBody(
    status: TunnelStatus,
    hasProfile: Boolean,
    lastError: String?,
): String = when {
    !lastError.isNullOrBlank() || status == TunnelStatus.Error ->
        stringResource(R.string.home_status_error_body)
    !hasProfile -> stringResource(R.string.home_status_needs_profile_body)
    status == TunnelStatus.Connected -> stringResource(R.string.home_status_connected_body)
    status in setOf(TunnelStatus.Preparing, TunnelStatus.Starting, TunnelStatus.Reconnecting) ->
        stringResource(R.string.home_status_preparing_body)
    status == TunnelStatus.Stopping -> stringResource(R.string.home_status_stopping_body)
    else -> stringResource(R.string.home_status_disconnected_body)
}

private fun connectionTone(
    status: TunnelStatus,
    hasProfile: Boolean,
    lastError: String?,
): GmStatusTone = when {
    !lastError.isNullOrBlank() || status == TunnelStatus.Error -> GmStatusTone.Error
    !hasProfile -> GmStatusTone.Warning
    status == TunnelStatus.Connected -> GmStatusTone.Connected
    status in setOf(TunnelStatus.Preparing, TunnelStatus.Starting, TunnelStatus.Reconnecting) ->
        GmStatusTone.Preparing
    status == TunnelStatus.Stopping -> GmStatusTone.Warning
    else -> GmStatusTone.Disconnected
}

@Composable
private fun ErrorBanner(
    error: String,
    diagnosticsMessage: String?,
    onDismiss: () -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    GmCard(modifier = Modifier.fillMaxWidth(), tone = GmCardTone.Error) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.status_error),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm)) {
            OutlinedButton(onClick = onCopyDiagnostics) {
                Text(stringResource(R.string.action_copy_redacted_diagnostics))
            }
        }
        if (!diagnosticsMessage.isNullOrBlank()) {
            Text(
                text = diagnosticsMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActiveProfileCard(
    profiles: List<ProfileEntry>,
    activeIndex: Int,
    latencies: Map<Int, LatencyState>,
) {
    val summary = profiles.getOrNull(activeIndex)
        ?.let { profileDisplaySummary(it, activeIndex + 1) }
    GmCard(
        modifier = Modifier.fillMaxWidth(),
        tone = if (summary == null) GmCardTone.Warning else GmCardTone.Selected,
    ) {
        Text(
            text = stringResource(R.string.home_active_profile),
            style = MaterialTheme.typography.titleMedium,
        )
        if (summary == null) {
            Text(
                text = stringResource(R.string.home_no_active_profile_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(text = summary.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = listOf(summary.secondaryLabel, latencyLabel(latencies[activeIndex]))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_active_profile_change_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecondaryActionsCard(actions: HomeActions) {
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_secondary_actions),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        ) {
            OutlinedButton(onClick = actions.onPerAppRouting, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.routing_card_action))
            }
            OutlinedButton(onClick = actions.onAbout, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_diagnostics))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManualProfileCard(onAdd: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_manual_header),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.profile_manual_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.profile_uri_label)) },
            placeholder = { Text(stringResource(R.string.profile_uri_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
            ),
        )
        Button(
            onClick = {
                onAdd(draft.trim())
                draft = ""
            },
            enabled = draft.isNotBlank(),
        ) { Text(stringResource(R.string.action_save)) }
    }
}

@Composable
private fun LibraryCard(
    profiles: List<ProfileEntry>,
    activeIndex: Int,
    latencies: Map<Int, LatencyState>,
    onSelect: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onTest: (Int) -> Unit,
    onTestAll: () -> Unit,
) {
    var detailsIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    GmCard(modifier = Modifier.fillMaxWidth()) {
        if (profiles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_count, profiles.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onTestAll) {
                    Text(stringResource(R.string.action_test_all))
                }
            }
        }
        if (profiles.isEmpty()) {
            EmptyLibraryState()
        } else {
            profiles.forEachIndexed { index, profile ->
                val summary = profileDisplaySummary(profile, index + 1)
                ProfileListItem(
                    displayName = summary.displayName,
                    protocol = summary.secondaryLabel,
                    active = index == activeIndex,
                    activeLabel = stringResource(R.string.profile_status_active),
                    latency = latencyLabel(latencies[index]),
                    onClick = { detailsIndex = index },
                    trailingContent = {
                        if (index != activeIndex) {
                            TextButton(onClick = { onSelect(index) }) {
                                Text(stringResource(R.string.action_choose_active))
                            }
                        }
                    },
                )
            }
            OutlinedButton(onClick = onClear) {
                Text(stringResource(R.string.action_clear))
            }
        }
    }

    detailsIndex?.let { index ->
        profiles.getOrNull(index)?.let { profile ->
            ProfileDetailsDialog(
                profile = profile,
                index = index,
                active = index == activeIndex,
                latency = latencies[index],
                onSelect = { onSelect(index) },
                onRename = { name -> onRename(index, name) },
                onDelete = { deleteIndex = index },
                onTest = { onTest(index) },
                onDismiss = { detailsIndex = null },
            )
        } ?: run { detailsIndex = null }
    }

    deleteIndex?.let { index ->
        val profile = profiles.getOrNull(index)
        if (profile == null) {
            deleteIndex = null
        } else {
            ConfirmDeleteProfileDialog(
                name = profileDisplaySummary(profile, index + 1).displayName,
                active = index == activeIndex,
                onConfirm = {
                    onRemove(index)
                    deleteIndex = null
                    if (detailsIndex == index) detailsIndex = null
                },
                onCancel = { deleteIndex = null },
            )
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    GmCard(modifier = Modifier.fillMaxWidth(), tone = GmCardTone.Warning) {
        Text(
            text = stringResource(R.string.home_status_needs_profile_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.library_empty_premium),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileDetailsDialog(
    profile: ProfileEntry,
    index: Int,
    active: Boolean,
    latency: LatencyState?,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    onDismiss: () -> Unit,
) {
    var renameDraft by remember(profile) {
        mutableStateOf(profileDisplaySummary(profile, index + 1).displayName)
    }
    val sanitizedName = sanitizeCustomProfileName(renameDraft)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.xs)) {
                val summary = profileDisplaySummary(profile, index + 1)
                Text(summary.displayName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.profile_details_protocol, summary.secondaryLabel))
                Text(
                    stringResource(
                        R.string.profile_details_status,
                        if (active) {
                            stringResource(R.string.profile_status_active)
                        } else {
                            stringResource(R.string.profile_status_inactive)
                        },
                    ),
                )
                Text(stringResource(R.string.profile_details_latency, latencyLabel(latency)))
                Text(stringResource(R.string.profile_details_source, profile.source.label()))
                Text(
                    stringResource(
                        R.string.profile_details_added,
                        profile.createdAtEpochMillis.formatDate(),
                    ),
                )
                Text(
                    stringResource(
                        R.string.profile_details_updated,
                        profile.updatedAtEpochMillis.formatDate(),
                    ),
                )
                Text(
                    stringResource(R.string.profile_details_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(GmSpacing.xs))
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_rename_label)) },
                    isError = renameDraft.isNotBlank() && sanitizedName == null,
                )
                if (renameDraft.isBlank() || sanitizedName == null) {
                    Text(
                        stringResource(R.string.profile_rename_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(GmSpacing.xs)) {
                    OutlinedButton(onClick = onTest) {
                        Text(stringResource(R.string.action_test))
                    }
                    if (!active) {
                        OutlinedButton(onClick = onSelect) {
                            Text(stringResource(R.string.action_choose_active))
                        }
                    }
                }
                OutlinedButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    sanitizedName?.let(onRename)
                    onDismiss()
                },
                enabled = sanitizedName != null,
            ) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.subscription_confirm_cancel))
            }
        },
    )
}

@Composable
private fun ConfirmDeleteProfileDialog(
    name: String,
    active: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.profile_delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.xs)) {
                Text(stringResource(R.string.profile_delete_confirm_body, name))
                if (active) {
                    Text(
                        stringResource(R.string.profile_delete_active_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.subscription_confirm_cancel))
            }
        },
    )
}

@Composable
private fun ConfirmImportDialog(
    pending: PendingImport,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.subscription_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.xs)) {
                Text(
                    stringResource(
                        R.string.subscription_confirm_summary,
                        pending.profiles.size,
                        pending.warnings,
                    ),
                )
                Text(
                    stringResource(
                        R.string.subscription_confirm_protocols,
                        pending.profiles
                            .groupingBy { it.protocolLabel }
                            .eachCount()
                            .entries
                            .joinToString(", ") { "${it.key}: ${it.value}" },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (pending.duplicateUris > 0) {
                    Text(
                        stringResource(
                            R.string.subscription_confirm_duplicates,
                            pending.duplicateUris,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    stringResource(R.string.subscription_confirm_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val previewMax = 5
                pending.profiles.take(previewMax).forEach { profile ->
                    Text(
                        text = "• " +
                            profile.suggestedName + " · " + profile.protocolLabel,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pending.profiles.size > previewMax) {
                    Text(
                        stringResource(
                            R.string.subscription_confirm_more,
                            pending.profiles.size - previewMax,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.subscription_confirm_save, pending.profiles.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.subscription_confirm_cancel))
            }
        },
    )
}

@Composable
private fun SubscriptionCard(
    inFlight: Boolean,
    message: String?,
    onFetch: (String, FfiSubscriptionFormat) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(FfiSubscriptionFormat.BASE64_URI_LIST) }

    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.subscription_card_input_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.subscription_privacy_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.subscription_url_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
            ),
        )
        FormatPicker(selected = format, onChange = { format = it })
        Text(
            stringResource(R.string.subscription_replaces_library),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun RoutingCard(onClick: () -> Unit) {
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.routing_card_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.routing_card_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClick) {
            Text(stringResource(R.string.routing_card_action))
        }
    }
}

@Composable
private fun AlwaysOnHint(onClick: () -> Unit) {
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.always_on_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.always_on_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClick) {
            Text(stringResource(R.string.action_always_on))
        }
    }
}

@Composable
private fun ProfileSource.label(): String = when (this) {
    ProfileSource.MANUAL -> stringResource(R.string.profile_source_manual)
    ProfileSource.SUBSCRIPTION -> stringResource(R.string.profile_source_subscription)
    ProfileSource.IMPORT -> stringResource(R.string.profile_source_import)
    ProfileSource.LEGACY -> stringResource(R.string.profile_source_legacy)
}

private fun Long?.formatDate(): String =
    this?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date(it))
    } ?: "unknown"

@Composable
private fun latencyLabel(state: LatencyState?): String = when (state) {
    null, LatencyState.Idle -> stringResource(R.string.latency_idle)
    LatencyState.InFlight -> stringResource(R.string.latency_inflight)
    is LatencyState.Result -> state.ms?.let {
        stringResource(R.string.latency_value, it)
    } ?: stringResource(R.string.latency_unreachable)
}

@Composable
private fun FfiSubscriptionFormat.label(): String = when (this) {
    FfiSubscriptionFormat.URI_LIST -> stringResource(R.string.subscription_format_uri_list)
    FfiSubscriptionFormat.BASE64_URI_LIST -> stringResource(R.string.subscription_format_base64)
    FfiSubscriptionFormat.SIP008 -> stringResource(R.string.subscription_format_sip008)
}
