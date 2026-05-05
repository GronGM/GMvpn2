package com.gmvpn.client.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gmvpn.client.BuildConfig
import com.gmvpn.client.R
import com.gmvpn.client.diagnostics.DiagnosticsCollector
import com.gmvpn.client.profile.LatencyProbe
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.profile.SubscriptionFetchException
import com.gmvpn.client.profile.SubscriptionFetcher
import com.gmvpn.client.routing.InstalledApp
import com.gmvpn.client.routing.InstalledAppsLoader
import com.gmvpn.client.routing.PerAppMode
import com.gmvpn.client.routing.PerAppRoutingStore
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
import uniffi.gmvpn_ffi.GmvpnException
import uniffi.gmvpn_ffi.coreVersion
import uniffi.gmvpn_ffi.decodeSubscriptionUris
import uniffi.gmvpn_ffi.parseProfileUri

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            TunnelController.onPermissionGranted(this)
        }
    }

    private lateinit var profileStore: ProfileStore
    private lateinit var routingStore: PerAppRoutingStore
    private val subscriptionFetcher = SubscriptionFetcher()
    private val latencyProbe = LatencyProbe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileStore = ProfileStore(applicationContext)
        routingStore = PerAppRoutingStore(applicationContext)

        setContent {
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

                val status by TunnelController.status.collectAsStateWithLifecycle()
                val lastError by TunnelController.lastError.collectAsStateWithLifecycle()
                val library by profileStore.library.collectAsState(initial = emptyList())
                val activeIndex by profileStore.activeIndex.collectAsState(initial = -1)
                val activeUri by profileStore.activeUri.collectAsState(initial = null)

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
                        onExportDiagnostics = {
                            lifecycleScope.launch {
                                diagnosticsMessage = exportDiagnostics(
                                    status = status,
                                    lastError = lastError,
                                    library = library,
                                    latencies = latencies,
                                )
                            }
                        },
                    )
                } else {
                    HomeScreen(
                        state = HomeUiState(
                            status = status,
                            lastError = lastError,
                            library = library,
                            activeIndex = activeIndex,
                            activeUri = activeUri,
                            subscriptionMessage = subscriptionMessage,
                            subscriptionInFlight = subscriptionInFlight,
                            latencies = latencies,
                        ),
                        actions = HomeActions(
                            onConnect = ::handleConnect,
                            onDisconnect = { TunnelController.requestStop(this) },
                            onAddUri = { uri ->
                                lifecycleScope.launch { profileStore.setActiveUri(uri) }
                            },
                            onSelectProfile = { idx ->
                                lifecycleScope.launch { profileStore.setActive(idx) }
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
                                    val outcome = runCatching {
                                        importSubscription(url, format)
                                    }
                                    subscriptionInFlight = false
                                    subscriptionMessage = outcome.fold(
                                        onSuccess = { it },
                                        onFailure = { err ->
                                            getString(
                                                R.string.subscription_failed,
                                                err.message ?: err.javaClass.simpleName,
                                            )
                                        },
                                    )
                                }
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

    private suspend fun importSubscription(url: String, format: FfiSubscriptionFormat): String {
        val body = try {
            subscriptionFetcher.fetch(url)
        } catch (e: SubscriptionFetchException) {
            throw IllegalStateException(e.message, e)
        }
        val out = try {
            decodeSubscriptionUris(body, format)
        } catch (e: GmvpnException) {
            throw IllegalStateException(e.message, e)
        }
        if (out.uris.isEmpty()) {
            throw IllegalStateException("subscription contained no usable profiles")
        }
        profileStore.replaceAll(out.uris)
        return getString(R.string.subscription_imported, out.uris.size, out.warnings.size)
    }

    private fun handleConnect() {
        val intent: Intent? = TunnelController.preparePermission(this)
        if (intent == null) {
            TunnelController.requestStart(this)
        } else {
            vpnPermissionLauncher.launch(intent)
        }
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
     * coupling MainActivity to the gomobile classes — if the engine
     * `.aar` is missing the call returns "unbundled".
     */
    private fun engineXrayVersion(): String = try {
        val cls = Class.forName("com.gmvpn.core.gmvpn.Gmvpn")
        val method = cls.methods.firstOrNull { it.name == "xrayVersion" && it.parameterCount == 0 }
        (method?.invoke(null) as? String) ?: "unbundled"
    } catch (_: Throwable) {
        "unbundled"
    }

    /**
     * Builds a redacted diagnostics blob, writes it to the cache, and
     * launches an ACTION_SEND chooser. Returns a short status message
     * the About screen can show under the export button.
     */
    private suspend fun exportDiagnostics(
        status: TunnelStatus,
        lastError: String?,
        library: List<String>,
        latencies: Map<Int, LatencyState>,
    ): String = try {
        val text = DiagnosticsCollector.collect(
            context = applicationContext,
            appVersion = BuildConfig.VERSION_NAME,
            xrayVersion = engineXrayVersion(),
            status = status,
            lastError = lastError,
            library = library,
            latencies = latencies,
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

    private fun diagnosticsTimestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
}
