package com.gmvpn.client.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gmvpn.client.BuildConfig
import com.gmvpn.client.R
import com.gmvpn.client.diagnostics.RedactedDiagnosticsInput
import com.gmvpn.client.diagnostics.RedactedImportDiagnostics
import com.gmvpn.client.diagnostics.RedactedDiagnosticsReport
import com.gmvpn.client.profile.LatencyProbe
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.profile.ProfileEntryInput
import com.gmvpn.client.profile.ProfileSource
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.profile.SubscriptionDecodeOutput
import com.gmvpn.client.profile.SubscriptionFetchDiagnostics
import com.gmvpn.client.profile.SubscriptionFetcher
import com.gmvpn.client.profile.SubscriptionImportFailureCategory
import com.gmvpn.client.profile.hasSupportedProfileScheme
import com.gmvpn.client.profile.prepareSubscriptionImport
import com.gmvpn.client.profile.profileSummary
import com.gmvpn.client.profile.subscriptionImportFailureCategory
import com.gmvpn.client.profile.subscriptionSaveFailure
import com.gmvpn.client.routing.InstalledApp
import com.gmvpn.client.routing.InstalledAppsLoader
import com.gmvpn.client.routing.PerAppMode
import com.gmvpn.client.routing.PerAppRoutingStore
import com.gmvpn.client.tunnel.EngineBridge
import com.gmvpn.client.tunnel.TunnelController
import com.gmvpn.client.tunnel.TunnelStatus
import com.gmvpn.client.ui.theme.GmvpnTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.gmvpn_ffi.FfiSubscriptionFormat
import uniffi.gmvpn_ffi.coreVersion
import uniffi.gmvpn_ffi.decodeSubscriptionUris
import uniffi.gmvpn_ffi.parseProfileUri

private const val IMPORT_DIAGNOSTIC_TAG = "GMvpnImport"

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            TunnelController.onPermissionGranted(this)
        } else {
            TunnelController.onPermissionDenied()
        }
    }

    private lateinit var profileStore: ProfileStore
    private lateinit var routingStore: PerAppRoutingStore
    private val subscriptionFetcher = SubscriptionFetcher()
    private val latencyProbe = LatencyProbe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        profileStore = ProfileStore(applicationContext)
        routingStore = PerAppRoutingStore(applicationContext)

        setContent {
            val baseDensity = LocalDensity.current
            val appDensity = remember(baseDensity.density, baseDensity.fontScale) {
                Density(baseDensity.density, baseDensity.fontScale.coerceAtMost(1.15f))
            }
            CompositionLocalProvider(LocalDensity provides appDensity) {
                GmvpnTheme {
                var showAbout by remember { mutableStateOf(false) }
                var showRouting by remember { mutableStateOf(false) }
                var installedApps by remember {
                    mutableStateOf<List<InstalledApp>>(emptyList())
                }
                var appsLoading by remember { mutableStateOf(false) }
                val routing by routingStore.routing.collectAsState(
                    initial = com.gmvpn.client.routing.PerAppRouting(),
                )
                var subscriptionMessage by remember { mutableStateOf<String?>(null) }
                var subscriptionInFlight by remember { mutableStateOf(false) }
                var latencies by remember {
                    mutableStateOf<Map<Int, LatencyState>>(emptyMap())
                }
                val probeJobs = remember { mutableMapOf<Int, Job>() }
                var diagnosticsIncludeDevice by remember { mutableStateOf(false) }
                var lastImportDiagnostic by remember {
                    mutableStateOf<RedactedImportDiagnostics?>(null)
                }

                val status by TunnelController.status.collectAsStateWithLifecycle()
                val lastError by TunnelController.lastError.collectAsStateWithLifecycle()
                val profiles by profileStore.entries.collectAsState(initial = emptyList())
                val activeIndex by profileStore.activeIndex.collectAsState(initial = -1)
                val library = profiles.map { it.uri }
                val activeUri = profiles.getOrNull(activeIndex)?.uri

                fun probeProfile(index: Int) {
                    val uri = library.getOrNull(index) ?: return
                    probeJobs.remove(index)?.cancel()
                    latencies = latencies + (index to LatencyState.InFlight)
                    val job = lifecycleScope.launch {
                        val result = runCatching {
                            val profile = parseProfileUri(uri)
                            latencyProbe.probe(profile.server, profile.port.toInt())
                        }.getOrNull()
                        latencies = latencies + (index to LatencyState.Result(result))
                    }
                    probeJobs[index] = job
                }

                var diagnosticsMessage by remember { mutableStateOf<String?>(null) }
                var pendingImport by remember { mutableStateOf<PendingImport?>(null) }

                if (showRouting) {
                    BackHandler { showRouting = false }
                    PerAppRoutingScreen(
                        mode = routing.mode,
                        selected = routing.packages,
                        apps = installedApps,
                        appsLoading = appsLoading,
                        onModeChange = { mode ->
                            lifecycleScope.launch { routingStore.setMode(mode) }
                        },
                        onTogglePackage = { pkg ->
                            lifecycleScope.launch { routingStore.togglePackage(pkg) }
                        },
                        onClearSelection = {
                            lifecycleScope.launch { routingStore.clearPackages() }
                        },
                    )
                } else if (showAbout) {
                    BackHandler { showAbout = false }
                    AboutScreen(
                        appVersion = BuildConfig.VERSION_NAME,
                        coreVersion = coreVersion(),
                        xrayVersion = engineXrayVersion(),
                        diagnosticsMessage = diagnosticsMessage,
                        includeDeviceInDiagnostics = diagnosticsIncludeDevice,
                        onIncludeDeviceInDiagnosticsChange = {
                            diagnosticsIncludeDevice = it
                        },
                        onCopyDiagnostics = {
                            diagnosticsMessage = copyDiagnosticsReport(
                                status = status,
                                lastError = lastError,
                                activeUri = activeUri,
                                profileCount = library.size,
                                includeDevice = diagnosticsIncludeDevice,
                                lastImportAttempt = lastImportDiagnostic,
                            )
                        },
                        onExportDiagnostics = {
                            lifecycleScope.launch {
                                diagnosticsMessage = exportDiagnostics(
                                    status = status,
                                    lastError = lastError,
                                    activeUri = activeUri,
                                    profileCount = library.size,
                                    includeDevice = diagnosticsIncludeDevice,
                                    lastImportAttempt = lastImportDiagnostic,
                                )
                            }
                        },
                    )
                } else {
                    HomeScreen(
                        state = HomeUiState(
                            status = status,
                            lastError = lastError,
                            profiles = profiles,
                            activeIndex = activeIndex,
                            activeUri = activeUri,
                            subscriptionMessage = subscriptionMessage,
                            subscriptionInFlight = subscriptionInFlight,
                            pendingImport = pendingImport,
                            latencies = latencies,
                            diagnosticsMessage = diagnosticsMessage,
                        ),
                        actions = HomeActions(
                            onConnect = { handleConnect(activeUri) },
                            onDisconnect = { TunnelController.requestStop(this) },
                            onDismissError = { TunnelController.dismissError() },
                            onCopyDiagnostics = {
                                diagnosticsMessage = copyDiagnosticsReport(
                                    status = status,
                                    lastError = lastError,
                                    activeUri = activeUri,
                                    profileCount = library.size,
                                    includeDevice = diagnosticsIncludeDevice,
                                    lastImportAttempt = lastImportDiagnostic,
                                )
                            },
                            onExportDiagnostics = {
                                lifecycleScope.launch {
                                    diagnosticsMessage = exportDiagnostics(
                                        status = status,
                                        lastError = lastError,
                                        activeUri = activeUri,
                                        profileCount = library.size,
                                        includeDevice = diagnosticsIncludeDevice,
                                        lastImportAttempt = lastImportDiagnostic,
                                    )
                                }
                            },
                            onAddUri = { uri ->
                                lifecycleScope.launch { profileStore.setActiveUri(uri) }
                            },
                            onSelectProfile = { idx ->
                                lifecycleScope.launch { profileStore.setActive(idx) }
                            },
                            onRenameProfile = { idx, name ->
                                lifecycleScope.launch {
                                    val renamed = profileStore.renameAt(idx, name)
                                    if (!renamed) {
                                        subscriptionMessage = getString(
                                            R.string.profile_rename_invalid,
                                        )
                                    }
                                }
                            },
                            onRemoveProfile = { idx ->
                                lifecycleScope.launch { profileStore.removeAt(idx) }
                            },
                            onClearLibrary = {
                                lifecycleScope.launch { profileStore.clearAll() }
                            },
                            onFetchSubscription = { url, format ->
                                lifecycleScope.launch {
                                    subscriptionInFlight = true
                                    subscriptionMessage = getString(R.string.subscription_fetching)
                                    lastImportDiagnostic = RedactedImportDiagnostics.inFlight(
                                        SubscriptionFetchDiagnostics.fromInput(url),
                                    )
                                    val outcome = runCatching {
                                        decodeSubscription(url, format)
                                    }
                                    subscriptionInFlight = false
                                    outcome.fold(
                                        onSuccess = { decoded ->
                                            pendingImport = decoded
                                            subscriptionMessage = null
                                            lastImportDiagnostic = RedactedImportDiagnostics.decodeSuccess(
                                                decoded.profiles.size,
                                                previousAttempt = lastImportDiagnostic,
                                            )
                                        },
                                        onFailure = { err ->
                                            val safeMessageKey = safeSubscriptionFailureMessageKey(err)
                                            val redactedDiagnostic =
                                                RedactedImportDiagnostics.failureFromThrowable(
                                                    error = err,
                                                    previousAttempt = lastImportDiagnostic,
                                                )
                                            logSubscriptionImportDiagnostics(
                                                diagnostic = redactedDiagnostic,
                                                safeMessageKey = safeMessageKey,
                                            )
                                            lastImportDiagnostic = redactedDiagnostic
                                            subscriptionMessage = getString(
                                                R.string.subscription_failed,
                                                safeSubscriptionFailureMessage(safeMessageKey),
                                            )
                                        },
                                    )
                                }
                            },
                            onConfirmImport = {
                                pendingImport?.let { pending ->
                                    pendingImport = null
                                    lastImportDiagnostic = RedactedImportDiagnostics.saveStart(
                                        previousAttempt = lastImportDiagnostic,
                                    )
                                    lifecycleScope.launch {
                                        val save = runCatching {
                                            profileStore.replaceAllEntries(
                                                pending.profiles.map { preview ->
                                                    ProfileEntryInput(
                                                        uri = preview.uri,
                                                        customName = preview.suggestedName,
                                                        source = ProfileSource.SUBSCRIPTION,
                                                    )
                                                },
                                            )
                                        }
                                        save.fold(
                                            onSuccess = {
                                                lastImportDiagnostic = RedactedImportDiagnostics.success(
                                                    pending.profiles.size,
                                                    previousAttempt = lastImportDiagnostic,
                                                )
                                                subscriptionMessage = getString(
                                                    R.string.subscription_imported,
                                                    pending.profiles.size,
                                                    pending.warnings,
                                                )
                                            },
                                            onFailure = { err ->
                                                val wrapped = subscriptionSaveFailure(err)
                                                val redactedDiagnostic =
                                                    RedactedImportDiagnostics.failureFromThrowable(
                                                        error = wrapped,
                                                        previousAttempt = lastImportDiagnostic,
                                                    )
                                                logSubscriptionImportDiagnostics(
                                                    diagnostic = redactedDiagnostic,
                                                    safeMessageKey = safeSubscriptionFailureMessageKey(wrapped),
                                                )
                                                lastImportDiagnostic = redactedDiagnostic
                                                subscriptionMessage = getString(
                                                    R.string.subscription_failed,
                                                    safeSubscriptionFailureMessage(wrapped),
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                            onCancelImport = {
                                pendingImport = null
                                subscriptionMessage = getString(
                                    R.string.subscription_cancelled,
                                )
                            },
                            onAlwaysOn = ::openAlwaysOnSettings,
                            onAbout = { showAbout = true },
                            onPerAppRouting = {
                                showRouting = true
                                if (installedApps.isEmpty() && !appsLoading) {
                                    appsLoading = true
                                    lifecycleScope.launch {
                                        installedApps = InstalledAppsLoader
                                            .load(applicationContext)
                                        appsLoading = false
                                    }
                                }
                            },
                            onTestProfile = { idx -> probeProfile(idx) },
                            onTestAllProfiles = {
                                library.indices.forEach { probeProfile(it) }
                            },
                        ),
                    )
                }
            }
        }
        }
    }

    /**
     * Fetch + decode a subscription URL into a [PendingImport]. Does
     * **not** touch [profileStore]; the caller (UI) shows a confirm
     * dialog and only commits on user approval. This makes a hostile
     * subscription URL unable to silently rotate the user's library
     * -- see security-review-001 section 2.
     */
    private suspend fun decodeSubscription(
        url: String,
        format: FfiSubscriptionFormat,
    ): PendingImport {
        val prepared = prepareSubscriptionImport(
            url = url,
            format = format,
            fetchBody = { subscriptionFetcher.fetch(it) },
            decodeUris = { body, requestedFormat ->
                val out = decodeSubscriptionUris(body, requestedFormat)
                SubscriptionDecodeOutput(
                    uris = out.uris,
                    warningCount = out.warnings.size,
                )
            },
        )
        return PendingImport(
            profiles = prepared.profiles,
            warnings = prepared.warnings,
            duplicateUris = prepared.duplicateUris,
        )
    }

    private fun handleConnect(activeUri: String?) {
        if (activeUri.isNullOrBlank()) {
            TunnelController.publishStatus(
                TunnelStatus.Error,
                getString(R.string.profile_missing_body),
            )
            return
        }

        if (!hasSupportedProfileScheme(activeUri)) {
            TunnelController.publishStatus(
                TunnelStatus.Error,
                getString(R.string.profile_invalid_body),
            )
            return
        }

        lifecycleScope.launch {
            val profileIsValid = withContext(Dispatchers.Default) {
                runCatching { parseProfileUri(activeUri) }.isSuccess
            }
            if (!profileIsValid) {
                TunnelController.publishStatus(
                    TunnelStatus.Error,
                    getString(R.string.profile_invalid_body),
                )
                return@launch
            }

            val intent: Intent? = TunnelController.preparePermission(this@MainActivity)
            if (intent == null) {
                TunnelController.requestStart(this@MainActivity)
            } else {
                vpnPermissionLauncher.launch(intent)
            }
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.rgb(10, 14, 19)
        window.navigationBarColor = Color.rgb(10, 14, 19)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
    }

    private fun safeSubscriptionFailureMessage(error: Throwable): String =
        safeSubscriptionFailureMessage(safeSubscriptionFailureMessageKey(error))

    private fun safeSubscriptionFailureMessage(messageKey: String): String =
        when (messageKey) {
            "subscription_error_invalid_url" -> getString(R.string.subscription_error_invalid_url)
            "subscription_error_network" -> getString(R.string.subscription_error_network)
            "subscription_error_format" -> getString(R.string.subscription_error_format)
            "subscription_error_empty" -> getString(R.string.subscription_error_empty)
            else -> getString(R.string.subscription_error_generic)
        }

    private fun safeSubscriptionFailureMessageKey(error: Throwable): String =
        when (subscriptionImportFailureCategory(error)) {
            SubscriptionImportFailureCategory.EmptyInput -> "subscription_error_invalid_url"
            SubscriptionImportFailureCategory.FetchFailed -> "subscription_error_network"
            SubscriptionImportFailureCategory.UnsupportedFormat,
            SubscriptionImportFailureCategory.ParseFailed -> "subscription_error_format"
            SubscriptionImportFailureCategory.NoProfilesFound -> "subscription_error_empty"
            SubscriptionImportFailureCategory.SaveFailed,
            SubscriptionImportFailureCategory.Unknown -> "subscription_error_generic"
        }

    private fun logSubscriptionImportDiagnostics(
        diagnostic: RedactedImportDiagnostics,
        safeMessageKey: String,
    ) {
        Log.i(
            IMPORT_DIAGNOSTIC_TAG,
            "subscription_import_diagnostics " +
                diagnostic.toSafeLogString(safeMessageKey = safeMessageKey),
        )
    }

    private fun openAlwaysOnSettings() {
        val candidates = listOf(
            Intent("android.net.vpn.SETTINGS"),
            Intent(Settings.ACTION_VPN_SETTINGS),
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try next.
            }
        }
        Toast.makeText(this, R.string.always_on_unavailable, Toast.LENGTH_LONG).show()
    }

    /**
     * Pulls the pinned Xray-core version from the engine without
     * coupling MainActivity to the gomobile classes -- if the engine
     * `.aar` is missing the call returns "unbundled".
     */
    private fun engineXrayVersion(): String =
        EngineBridge().xrayVersionOrNull() ?: "unbundled"

    private fun copyDiagnosticsReport(
        status: TunnelStatus,
        lastError: String?,
        activeUri: String?,
        profileCount: Int,
        includeDevice: Boolean,
        lastImportAttempt: RedactedImportDiagnostics?,
    ): String = try {
        val text = buildDiagnosticsReport(
            status = status,
            lastError = lastError,
            activeUri = activeUri,
            profileCount = profileCount,
            includeDevice = includeDevice,
            lastImportAttempt = lastImportAttempt,
        )
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("GMvpn bug report", text),
        )
        getString(R.string.diagnostics_copied)
    } catch (e: Throwable) {
        getString(
            R.string.diagnostics_failed,
            e.message ?: e.javaClass.simpleName,
        )
    }

    /**
     * Builds a short redacted bug report, writes it to cache, and
     * launches an ACTION_SEND chooser. It intentionally excludes raw
     * logcat and full profile URIs.
     */
    private suspend fun exportDiagnostics(
        status: TunnelStatus,
        lastError: String?,
        activeUri: String?,
        profileCount: Int,
        includeDevice: Boolean,
        lastImportAttempt: RedactedImportDiagnostics?,
    ): String = try {
        val text = buildDiagnosticsReport(
            status = status,
            lastError = lastError,
            activeUri = activeUri,
            profileCount = profileCount,
            includeDevice = includeDevice,
            lastImportAttempt = lastImportAttempt,
        )
        val file = withContext(Dispatchers.IO) {
            val dir = File(cacheDir, "diagnostics").apply { mkdirs() }
            val name = "gmvpn-diagnostics-${diagnosticsTimestamp()}.txt"
            File(dir, name).also { it.writeText(text) }
        }
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            send,
            getString(R.string.diagnostics_share_chooser),
        )
        startActivity(chooser)
        getString(R.string.diagnostics_share_chooser)
    } catch (e: Throwable) {
        getString(
            R.string.diagnostics_failed,
            e.message ?: e.javaClass.simpleName,
        )
    }

    private fun buildDiagnosticsReport(
        status: TunnelStatus,
        lastError: String?,
        activeUri: String?,
        profileCount: Int,
        includeDevice: Boolean,
        lastImportAttempt: RedactedImportDiagnostics?,
    ): String =
        RedactedDiagnosticsReport.render(
            RedactedDiagnosticsInput(
                appVersion = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                packageName = packageName,
                androidRelease = Build.VERSION.RELEASE ?: "unknown",
                androidSdk = Build.VERSION.SDK_INT,
                deviceManufacturer = if (includeDevice) Build.MANUFACTURER else null,
                deviceModel = if (includeDevice) Build.MODEL else null,
                status = status,
                lastErrorCategory = RedactedDiagnosticsReport.categorizeLastError(lastError),
                selectedProtocolType = activeUri?.let { profileSummary(it, 1).secondaryLabel },
                profileCount = profileCount,
                lastImportAttempt = lastImportAttempt,
            ),
        )

    private fun diagnosticsTimestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
}
