package com.gmvpn.client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmvpn.client.R
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.profile.ProfileEntry
import com.gmvpn.client.profile.ProfileImportPreview
import com.gmvpn.client.profile.ProfileSource
import com.gmvpn.client.profile.profileDisplaySummary
import com.gmvpn.client.profile.sanitizeCustomProfileName
import com.gmvpn.client.tunnel.TunnelStatus
import com.gmvpn.client.ui.components.GmCard
import com.gmvpn.client.ui.components.GmCardTone
import com.gmvpn.client.ui.components.GmIconKind
import com.gmvpn.client.ui.components.GmLineIcon
import com.gmvpn.client.ui.components.GmStatusTone
import com.gmvpn.client.ui.components.PremiumConnectButton
import com.gmvpn.client.ui.components.PrivacyNotice
import com.gmvpn.client.ui.components.PrivacySettingsCard
import com.gmvpn.client.ui.components.ProfileListItem
import com.gmvpn.client.ui.components.StatusPill
import com.gmvpn.client.ui.components.gmAppBackground
import com.gmvpn.client.ui.theme.GmColors
import com.gmvpn.client.ui.theme.GmSpacing
import com.gmvpn.client.ui.theme.GmvpnTheme
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
    val onExportDiagnostics: () -> Unit,
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

private enum class GmTab(
    val labelRes: Int,
    val titleRes: Int,
    val icon: GmIconKind,
) {
    Home(R.string.nav_home, R.string.app_name, GmIconKind.Home),
    Profiles(R.string.nav_profiles, R.string.nav_profiles, GmIconKind.Profiles),
    Import(R.string.nav_import, R.string.subscription_header, GmIconKind.Import),
    Settings(R.string.nav_settings, R.string.privacy_settings_title, GmIconKind.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions) {
    GmAppShell(state = state, actions = actions, initialTab = GmTab.Home)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GmAppShell(
    state: HomeUiState,
    actions: HomeActions,
    initialTab: GmTab,
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var showDiagnostics by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .gmAppBackground(),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    AppTopBar(
                        tab = selectedTab,
                        profileCount = state.profiles.size,
                        onBackHome = { selectedTab = GmTab.Home },
                        onSettings = { selectedTab = GmTab.Settings },
                        onTestAllProfiles = actions.onTestAllProfiles,
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                    )
                },
            ) { padding ->
                when (selectedTab) {
                    GmTab.Home -> HomeTab(
                        state = state,
                        actions = actions,
                        padding = padding,
                        onProfiles = { selectedTab = GmTab.Profiles },
                        onImport = { selectedTab = GmTab.Import },
                        onDiagnostics = { showDiagnostics = true },
                    )
                    GmTab.Profiles -> ProfilesTab(
                        state = state,
                        actions = actions,
                        padding = padding,
                    )
                    GmTab.Import -> ImportTab(
                        state = state,
                        actions = actions,
                        padding = padding,
                    )
                    GmTab.Settings -> PrivacySettingsTab(
                        state = state,
                        actions = actions,
                        padding = padding,
                    )
                }

                state.pendingImport?.let {
                    ConfirmImportDialog(
                        pending = it,
                        onConfirm = actions.onConfirmImport,
                        onCancel = actions.onCancelImport,
                    )
                }

                if (showDiagnostics) {
                    DiagnosticsDialog(
                        message = state.diagnosticsMessage,
                        onCopy = {
                            actions.onCopyDiagnostics()
                            showDiagnostics = false
                        },
                        onExport = {
                            actions.onExportDiagnostics()
                            showDiagnostics = false
                        },
                        onDismiss = { showDiagnostics = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    tab: GmTab,
    profileCount: Int,
    onBackHome: () -> Unit,
    onSettings: () -> Unit,
    onTestAllProfiles: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            if (tab != GmTab.Home) {
                TextButton(onClick = onBackHome) {
                    Text(text = "<", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        title = {
            Column {
                Text(
                    text = stringResource(tab.titleRes),
                    style = if (tab == GmTab.Home) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tab == GmTab.Home) {
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            when (tab) {
                GmTab.Home -> TextButton(onClick = onSettings) {
                    GmLineIcon(
                        kind = GmIconKind.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                    )
                }
                GmTab.Profiles -> if (profileCount > 0) {
                    OutlinedButton(
                        onClick = onTestAllProfiles,
                        contentPadding = PaddingValues(horizontal = GmSpacing.sm, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.action_test_all),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                else -> Unit
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun BottomNavBar(selectedTab: GmTab, onSelectTab: (GmTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        GmTab.values().forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    GmLineIcon(
                        kind = tab.icon,
                        contentDescription = stringResource(tab.labelRes),
                        selected = selected,
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GmColors.PrimaryBlue,
                    selectedTextColor = GmColors.PrimaryBlue,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = GmColors.PrimaryBlue.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

@Composable
private fun HomeTab(
    state: HomeUiState,
    actions: HomeActions,
    padding: PaddingValues,
    onProfiles: () -> Unit,
    onImport: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    ScreenColumn(padding) {
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
            onClick = onProfiles,
        )

        ToolsSection(
            onRouting = actions.onPerAppRouting,
            onDiagnostics = onDiagnostics,
        )

        SavedProfilesPreview(
            profiles = state.profiles,
            activeIndex = state.activeIndex,
            latencies = state.latencies,
            onProfiles = onProfiles,
            onImport = onImport,
        )
    }
}

@Composable
private fun ProfilesTab(
    state: HomeUiState,
    actions: HomeActions,
    padding: PaddingValues,
) {
    var detailsIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }

    ScreenColumn(padding) {
        Text(
            text = stringResource(R.string.library_count, state.profiles.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.profiles.isEmpty()) {
            EmptyProfilesCard()
        } else {
            state.profiles.forEachIndexed { index, profile ->
                val summary = profileDisplaySummary(profile, index + 1)
                GmCard(
                    modifier = Modifier.fillMaxWidth(),
                    tone = if (index == state.activeIndex) {
                        GmCardTone.Selected
                    } else {
                        GmCardTone.Neutral
                    },
                ) {
                    ProfileListItem(
                        displayName = summary.displayName,
                        protocol = summary.secondaryLabel,
                        active = index == state.activeIndex,
                        activeLabel = stringResource(R.string.profile_status_active),
                        latency = latencyLabel(state.latencies[index]),
                        onClick = { detailsIndex = index },
                        trailingContent = {
                            TextButton(onClick = { detailsIndex = index }) {
                                Text("...")
                            }
                        },
                    )
                    if (index != state.activeIndex) {
                        OutlinedButton(
                            onClick = { actions.onSelectProfile(index) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = GmSpacing.sm, vertical = GmSpacing.xs),
                        ) {
                            Text(
                                text = stringResource(R.string.action_choose_active),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = actions.onClearLibrary,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GmColors.Error),
                border = BorderStroke(1.dp, GmColors.Error.copy(alpha = 0.58f)),
            ) {
                Text(stringResource(R.string.action_clear))
            }
        }
    }

    detailsIndex?.let { index ->
        state.profiles.getOrNull(index)?.let { profile ->
            ProfileDetailsDialog(
                profile = profile,
                index = index,
                active = index == state.activeIndex,
                latency = state.latencies[index],
                onSelect = { actions.onSelectProfile(index) },
                onRename = { name -> actions.onRenameProfile(index, name) },
                onDelete = { deleteIndex = index },
                onTest = { actions.onTestProfile(index) },
                onDismiss = { detailsIndex = null },
            )
        } ?: run { detailsIndex = null }
    }

    deleteIndex?.let { index ->
        val profile = state.profiles.getOrNull(index)
        if (profile == null) {
            deleteIndex = null
        } else {
            ConfirmDeleteProfileDialog(
                name = profileDisplaySummary(profile, index + 1).displayName,
                active = index == state.activeIndex,
                onConfirm = {
                    actions.onRemoveProfile(index)
                    deleteIndex = null
                    if (detailsIndex == index) detailsIndex = null
                },
                onCancel = { deleteIndex = null },
            )
        }
    }
}

@Composable
private fun ImportTab(
    state: HomeUiState,
    actions: HomeActions,
    padding: PaddingValues,
) {
    ScreenColumn(padding) {
        SubscriptionCard(
            inFlight = state.subscriptionInFlight,
            message = state.subscriptionMessage,
            onFetch = actions.onFetchSubscription,
        )
        ManualProfileCard(onAdd = actions.onAddUri)
    }
}

@Composable
private fun PrivacySettingsTab(
    state: HomeUiState,
    actions: HomeActions,
    padding: PaddingValues,
) {
    ScreenColumn(padding) {
        PrivacySettingsCard(
            title = stringResource(R.string.routing_card_title),
            body = stringResource(R.string.privacy_settings_routing_body),
            icon = GmIconKind.Routing,
            actionText = stringResource(R.string.privacy_settings_open),
            onClick = actions.onPerAppRouting,
            modifier = Modifier.fillMaxWidth(),
        )
        PrivacySettingsCard(
            title = stringResource(R.string.privacy_notice_title),
            body = stringResource(R.string.privacy_notice_body),
            icon = GmIconKind.Privacy,
            modifier = Modifier.fillMaxWidth(),
        )
        PrivacySettingsCard(
            title = stringResource(R.string.always_on_title),
            body = stringResource(R.string.always_on_explainer),
            icon = GmIconKind.Lock,
            actionText = stringResource(R.string.action_always_on),
            onClick = actions.onAlwaysOn,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScreenColumn(
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 18.dp, vertical = GmSpacing.sm)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        content = content,
    )
}

@Composable
private fun ConnectionHero(state: HomeUiState, actions: HomeActions) {
    val hasProfile = !state.activeUri.isNullOrBlank()
    val tone = connectionTone(state.status, hasProfile, state.lastError)
    val title = connectionTitle(state.status, hasProfile, state.lastError)
    val body = connectionBody(state.status, hasProfile, state.lastError)
    val destructive = state.status == TunnelStatus.Connected
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        ) {
            GmLineIcon(
                kind = GmIconKind.Shield,
                contentDescription = title,
                tone = tone,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
        OutlinedButton(onClick = onCopyDiagnostics) {
            Text(stringResource(R.string.action_copy_redacted_diagnostics))
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
    onClick: () -> Unit,
) {
    val summary = profiles.getOrNull(activeIndex)
        ?.let { profileDisplaySummary(it, activeIndex + 1) }
    GmCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier),
        tone = if (summary == null) GmCardTone.Warning else GmCardTone.Selected,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_active_profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (summary == null) {
                    Text(
                        text = stringResource(R.string.home_no_active_profile_body),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = summary.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(summary.secondaryLabel, latencyLabel(latencies[activeIndex]))
                            .filter { it.isNotBlank() }
                            .joinToString(" - "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text(">", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ToolsSection(onRouting: () -> Unit, onDiagnostics: () -> Unit) {
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_tools_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        ) {
            ToolActionTile(
                title = stringResource(R.string.routing_card_action),
                subtitle = stringResource(R.string.routing_card_short),
                icon = GmIconKind.Routing,
                onClick = onRouting,
                modifier = Modifier.weight(1f),
            )
            ToolActionTile(
                title = stringResource(R.string.action_diagnostics),
                subtitle = stringResource(R.string.diagnostics_short),
                icon = GmIconKind.Diagnostics,
                onClick = onDiagnostics,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolActionTile(
    title: String,
    subtitle: String,
    icon: GmIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(138.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(GmSpacing.sm),
            verticalArrangement = Arrangement.Center,
        ) {
            GmLineIcon(
                kind = icon,
                contentDescription = title,
                tone = GmStatusTone.Privacy,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.height(GmSpacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SavedProfilesPreview(
    profiles: List<ProfileEntry>,
    activeIndex: Int,
    latencies: Map<Int, LatencyState>,
    onProfiles: () -> Unit,
    onImport: () -> Unit,
) {
    GmCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_header),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.library_count, profiles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onProfiles) {
                Text(stringResource(R.string.action_view_all))
            }
        }
        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty_premium),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onImport) {
                Text(stringResource(R.string.action_import_profiles))
            }
        } else {
            profiles.take(2).forEachIndexed { index, profile ->
                val summary = profileDisplaySummary(profile, index + 1)
                ProfileListItem(
                    displayName = summary.displayName,
                    protocol = summary.secondaryLabel,
                    active = index == activeIndex,
                    activeLabel = stringResource(R.string.profile_status_active),
                    latency = latencyLabel(latencies[index]),
                    onClick = onProfiles,
                )
            }
        }
    }
}

@Composable
private fun EmptyProfilesCard() {
    GmCard(modifier = Modifier.fillMaxWidth(), tone = GmCardTone.Warning) {
        Text(
            text = stringResource(R.string.profiles_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.library_empty_premium),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.action_save)) }
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
                        text = "- " +
                            profile.suggestedName + " - " + profile.protocolLabel,
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
            placeholder = { Text(stringResource(R.string.subscription_url_hint_masked)) },
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
            modifier = Modifier.fillMaxWidth(),
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
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
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
private fun DiagnosticsDialog(
    message: String?,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diagnostics_header)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GmSpacing.sm)) {
                Text(
                    text = stringResource(R.string.diagnostics_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.diagnostics_review_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_export_bug_report), maxLines = 1)
                }
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.action_copy_report_short), maxLines = 1)
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

private fun previewActions(): HomeActions = HomeActions(
    onConnect = {},
    onDisconnect = {},
    onDismissError = {},
    onCopyDiagnostics = {},
    onExportDiagnostics = {},
    onAddUri = {},
    onSelectProfile = {},
    onRenameProfile = { _, _ -> },
    onRemoveProfile = {},
    onClearLibrary = {},
    onFetchSubscription = { _, _ -> },
    onConfirmImport = {},
    onCancelImport = {},
    onAlwaysOn = {},
    onAbout = {},
    onPerAppRouting = {},
    onTestProfile = {},
    onTestAllProfiles = {},
)

private fun previewProfiles(): List<ProfileEntry> = listOf(
    ProfileEntry("preview-nl", "Netherlands", 0, 0, ProfileSource.IMPORT),
    ProfileEntry("preview-nl-2", "Netherlands 2", 0, 0, ProfileSource.IMPORT),
    ProfileEntry("preview-de", "Germany", 0, 0, ProfileSource.IMPORT),
    ProfileEntry("preview-pl", "Poland", 0, 0, ProfileSource.IMPORT),
)

@Preview(name = "Home disconnected")
@Composable
private fun HomeDisconnectedPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Idle,
                lastError = null,
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Home,
        )
    }
}

@Preview(name = "Home connected")
@Composable
private fun HomeConnectedPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Connected,
                lastError = null,
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Home,
        )
    }
}

@Preview(name = "Home empty")
@Composable
private fun HomeEmptyPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Idle,
                lastError = null,
                profiles = emptyList(),
                activeIndex = -1,
                activeUri = null,
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Home,
        )
    }
}

@Preview(name = "Home error")
@Composable
private fun HomeErrorPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Error,
                lastError = "Connection failed. Check the profile or network.",
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Home,
        )
    }
}

@Preview(name = "Profiles tab")
@Composable
private fun ProfilesTabPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Idle,
                lastError = null,
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Profiles,
        )
    }
}

@Preview(name = "Import tab")
@Composable
private fun ImportTabPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Idle,
                lastError = null,
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Import,
        )
    }
}

@Preview(name = "Privacy settings tab")
@Composable
private fun SettingsTabPreview() {
    GmvpnTheme {
        GmAppShell(
            state = HomeUiState(
                status = TunnelStatus.Idle,
                lastError = null,
                profiles = previewProfiles(),
                activeIndex = 0,
                activeUri = "preview-nl",
                subscriptionMessage = null,
                subscriptionInFlight = false,
            ),
            actions = previewActions(),
            initialTab = GmTab.Settings,
        )
    }
}
