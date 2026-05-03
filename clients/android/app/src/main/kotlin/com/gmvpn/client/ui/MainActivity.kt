package com.gmvpn.client.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gmvpn.client.BuildConfig
import com.gmvpn.client.R
import com.gmvpn.client.profile.LatencyProbe
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.profile.SubscriptionFetchException
import com.gmvpn.client.profile.SubscriptionFetcher
import com.gmvpn.client.tunnel.TunnelController
import com.gmvpn.client.ui.theme.GmvpnTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private val subscriptionFetcher = SubscriptionFetcher()
    private val latencyProbe = LatencyProbe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileStore = ProfileStore(applicationContext)

        setContent {
            GmvpnTheme {
                var showAbout by remember { mutableStateOf(false) }
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

                if (showAbout) {
                    AboutScreen(
                        appVersion = BuildConfig.VERSION_NAME,
                        coreVersion = coreVersion(),
                        xrayVersion = engineXrayVersion(),
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
}
