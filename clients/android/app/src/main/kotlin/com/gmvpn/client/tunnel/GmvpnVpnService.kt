package com.gmvpn.client.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gmvpn.client.R
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.profile.ProfileStore
import com.gmvpn.client.profile.hasSupportedProfileScheme
import com.gmvpn.client.routing.PerAppMode
import com.gmvpn.client.routing.PerAppRouting
import com.gmvpn.client.routing.PerAppRoutingStore
import com.gmvpn.client.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import uniffi.gmvpn_ffi.FfiProfile
import uniffi.gmvpn_ffi.GmvpnException
import uniffi.gmvpn_ffi.buildXrayConfig
import uniffi.gmvpn_ffi.defaultTunnelOptions
import uniffi.gmvpn_ffi.parseProfileUri

/**
 * Android tunnel entry point. Pipeline on Start:
 *
 *  1. Pull the active profile URI from [ProfileStore].
 *  2. Parse it via UniFFI ([parseProfileUri]).
 *  3. Build the Xray-core JSON config via UniFFI ([buildXrayConfig]).
 *  4. Establish a TUN through `VpnService.Builder` and hand the fd to
 *     [EngineBridge] alongside the config.
 *  5. Forward engine status events back to [TunnelController].
 *
 * Stop tears the engine down first, then closes the
 * `ParcelFileDescriptor` and stops the foreground service.
 */
class GmvpnVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine = EngineBridge()
    private val tunnelMutex = Mutex()
    // FD ownership contract: gVisor fdbased.New does not take ownership
    // of the descriptor, so this service keeps the ParcelFileDescriptor
    // alive until an explicit stop, revoke, destroy, or failed start
    // cleanup path closes it through closeTun(...).
    private var tunInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var activeProfileName: String = ""

    private val cm by lazy { getSystemService(ConnectivityManager::class.java) }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastSeenNetwork: Network? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action ?: "<none>"} startId=$startId")
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            // Android starts the configured always-on VPN service with
            // the platform action. No intent extras are provided, so the
            // service must use the last persisted active profile.
            VpnService.SERVICE_INTERFACE, null -> handleStart()
            else -> handleStop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onRevoke() {
        // System revoked VPN permission (e.g. another VPN app took over).
        Log.i(TAG, "onRevoke")
        handleStop()
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        unregisterNetworkCallback()
        scope.cancel()
        TunnelController.publishStatus(TunnelStatus.Idle)
        super.onDestroy()
    }

    private fun handleStart() {
        Log.i(TAG, "handleStart")
        scope.launch {
            tunnelMutex.withLock {
                try {
                    if (bringTunnelUp()) {
                        registerNetworkCallback()
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "tunnel start failed", e)
                    emitError(
                        detail = e.message ?: "tunnel failed to start",
                        failureCategory = ConnectionFailureCategory.Unknown,
                    )
                    cleanupAfterFailure()
                }
            }
        }
    }

    private suspend fun bringTunnelUp(): Boolean {
        Log.i(TAG, "bringTunnelUp enter")
        val store = ProfileStore(applicationContext)
        val uri = store.activeUri.firstOrNull()
        if (uri.isNullOrBlank()) {
            Log.i(TAG, "bringTunnelUp no active profile")
            emitError(
                detail = getString(R.string.profile_missing_body),
                failureCategory = ConnectionFailureCategory.NoProfile,
            )
            cleanupAfterFailure()
            return false
        }
        if (!hasSupportedProfileScheme(uri)) {
            Log.i(TAG, "bringTunnelUp unsupported active profile scheme")
            emitError(
                detail = getString(R.string.profile_invalid_body),
                failureCategory = ConnectionFailureCategory.UnsupportedProfileScheme,
            )
            cleanupAfterFailure()
            return false
        }

        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_tunnel_idle)))

        val routingStore = PerAppRoutingStore(applicationContext)
        val routing = routingStore.snapshot()
        val profile: FfiProfile = try {
            parseProfileUri(uri)
        } catch (e: GmvpnException) {
            emitError(
                detail = "profile URI: ${e.message}",
                failureCategory = ConnectionFailureCategory.ProfileParseFailed,
            )
            cleanupAfterFailure()
            return false
        }

        // Randomise the SOCKS inbound port at runtime so a co-resident
        // hostile VPN cannot squat the well-known 10808 (security
        // review 001 §1). On allocation failure fall back to the
        // built-in default; it stays loopback-only either way.
        val opts = defaultTunnelOptions().let { defaults ->
            val randomPort = pickEphemeralLoopbackPort()
            if (randomPort != null) {
                defaults.copy(socksPort = randomPort.toUShort())
            } else {
                defaults
            }
        }
        val configJson = try {
            buildXrayConfig(profile, opts)
        } catch (e: GmvpnException) {
            emitError(
                detail = "config build: ${e.message}",
                failureCategory = ConnectionFailureCategory.ConfigBuildFailed,
            )
            cleanupAfterFailure()
            return false
        }

        val pfd = establishTun(profile, routing)
        if (pfd == null) {
            Log.w(TAG, "VpnService.establish returned null")
            emitError(
                detail = "VpnService.establish() returned null",
                failureCategory = ConnectionFailureCategory.VpnInterfaceNotEstablished,
            )
            cleanupAfterFailure()
            return false
        }
        tunInterface = pfd
        TunnelController.markVpnInterfaceEstablishedForShadow()
        Log.i(TAG, "VpnService.establish ok fd=${pfd.fd} pfd=${pfd.identity()}")

        try {
            Log.i(TAG, "engine.start fd=${pfd.fd} mtu=$TUN_MTU socksPort=${opts.socksPort}")
            engine.start(
                configJson = configJson,
                tunFd = pfd.fd,
                mtu = TUN_MTU,
                socksPort = opts.socksPort.toInt(),
                listener = ::onEngineStatus,
            )
            Log.i(TAG, "engine.start returned")
            TunnelController.markEngineStartedForShadow()
        } catch (e: EngineUnavailableException) {
            emitError(
                detail = e.message ?: "engine missing",
                failureCategory = ConnectionFailureCategory.EngineUnavailable,
            )
            cleanupAfterFailure()
            return false
        } catch (e: EngineStartException) {
            emitError(
                detail = e.cause?.message ?: e.message ?: "engine start failed",
                failureCategory = ConnectionFailureCategory.EngineStartFailed,
            )
            cleanupAfterFailure()
            return false
        }

        activeProfileName = profile.name
        TunnelController.publishStatus(TunnelStatus.Connected)
        startStatsLoop()
        Log.i(TAG, "bringTunnelUp connected")
        return true
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = scope.launch {
            var prev: TrafficStats? = null
            var prevAt = System.currentTimeMillis()
            while (isActive) {
                delay(STATS_INTERVAL_MS)
                val now = engine.stats() ?: break
                val nowAt = System.currentTimeMillis()
                val intervalMs = max(nowAt - prevAt, 1L)
                val upRate = bytesPerSecond(now.uplinkBytes, prev?.uplinkBytes, intervalMs)
                val downRate = bytesPerSecond(now.downlinkBytes, prev?.downlinkBytes, intervalMs)
                val text = getString(
                    R.string.notif_tunnel_stats,
                    formatBytes(upRate),
                    formatBytes(downRate),
                    formatBytes(now.uplinkBytes),
                    formatBytes(now.downlinkBytes),
                )
                postNotification(buildNotification(text))
                prev = now
                prevAt = nowAt
            }
        }
    }

    private fun stopStatsLoop() {
        statsJob?.cancel()
        statsJob = null
    }

    private fun postNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun establishTun(profile: FfiProfile, routing: PerAppRouting): ParcelFileDescriptor? {
        Log.i(
            TAG,
            "establishTun mtu=$TUN_MTU ipv4=$TUN_ADDRESS_V4/$TUN_PREFIX_V4 " +
                "ipv6=$TUN_ADDRESS_V6/$TUN_PREFIX_V6 routing=${routing.mode}",
        )
        val builder = Builder()
            .setSession(profile.name)
            .setMtu(TUN_MTU)
            .addAddress(TUN_ADDRESS_V4, TUN_PREFIX_V4)
            .addAddress(TUN_ADDRESS_V6, TUN_PREFIX_V6)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer(DNS_PRIMARY)
            .addDnsServer(DNS_SECONDARY)

        applyPerAppRouting(builder, routing)

        return builder.establish()
    }

    /**
     * Apply the per-app split tunnel and always exclude self.
     *
     *   Off            — only self excluded.
     *   IncludeOnly    — addAllowedApplication for every selected
     *                    package (filters out self from the list);
     *                    no addDisallowedApplication call needed
     *                    because anything not allowed is bypassed.
     *   ExcludeListed  — addDisallowedApplication for self plus
     *                    each selected package.
     *
     * Android forbids mixing the two; we are careful to call only
     * one variant per build.
     */
    private fun applyPerAppRouting(builder: Builder, routing: PerAppRouting) {
        when (routing.mode) {
            PerAppMode.Off -> {
                disallowSelf(builder)
            }
            PerAppMode.IncludeOnly -> {
                if (routing.packages.isEmpty()) {
                    // An empty include list would tunnel nothing; fall
                    // back to "tunnel everything but self" so the user
                    // never lands on a tunnel that does nothing.
                    disallowSelf(builder)
                    return
                }
                routing.packages
                    .filter { it != packageName }
                    .forEach { pkg ->
                        try {
                            builder.addAllowedApplication(pkg)
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.w(TAG, "addAllowed: $pkg", e)
                        }
                    }
            }
            PerAppMode.ExcludeListed -> {
                disallowSelf(builder)
                routing.packages
                    .filter { it != packageName }
                    .forEach { pkg ->
                        try {
                            builder.addDisallowedApplication(pkg)
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.w(TAG, "addDisallowed: $pkg", e)
                        }
                    }
            }
        }
    }

    private fun disallowSelf(builder: Builder) {
        // Exclude our own app from the tunnel so the SOCKS inbound on
        // 127.0.0.1 stays reachable from the engine's outbound dialer.
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "disallow self failed", e)
        }
    }

    private fun onEngineStatus(status: String, detail: String) {
        Log.i(TAG, "engine status=$status detailPresent=${detail.isNotBlank()}")
        val next = TunnelStatus.fromEngine(status)
        if (next == TunnelStatus.Error) {
            emitError(
                detail = if (detail.isNotBlank()) detail else "engine error",
                failureCategory = ConnectionFailureCategory.Unknown,
            )
        } else {
            TunnelController.publishStatus(next)
        }
    }

    private fun handleStop() {
        // Drop the network watcher first so an in-flight handover can't
        // race with the teardown.
        Log.i(TAG, "handleStop")
        unregisterNetworkCallback()
        scope.launch {
            tunnelMutex.withLock {
                stopStatsLoop()
                try {
                    engine.stop()
                } catch (e: Throwable) {
                    Log.w(TAG, "engine stop threw", e)
                }
                closeTun("handleStop")
                activeProfileName = ""
            }
            TunnelController.publishStatus(TunnelStatus.Idle)
            Log.i(TAG, "stopForeground/remove stopSelf")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cleanupAfterFailure() {
        Log.i(TAG, "cleanupAfterFailure")
        unregisterNetworkCallback()
        stopStatsLoop()
        Log.i(TAG, "engine.stop cleanup")
        runCatching { engine.stop() }
        closeTun("cleanupAfterFailure")
        activeProfileName = ""
        Log.i(TAG, "stopForeground/remove stopSelf after failure")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Re-establishes the tunnel without tearing the foreground service
     * down. Triggered by the [ConnectivityManager.NetworkCallback] when
     * the device's default network changes (Wi-Fi ↔ cellular handover,
     * Wi-Fi reconnect, etc.). Serialized through [tunnelMutex] so it
     * cannot interleave with an explicit Start/Stop or another
     * reconnect.
     */
    private suspend fun reconnectOnNetworkChange(reason: String) {
        tunnelMutex.withLock {
            Log.i(TAG, "reconnect: $reason")
            TunnelController.publishStatus(TunnelStatus.Reconnecting)
            stopStatsLoop()
            Log.i(TAG, "engine.stop reconnect")
            runCatching { engine.stop() }
            closeTun("reconnect")
            try {
                bringTunnelUp()
            } catch (e: Throwable) {
                Log.e(TAG, "reconnect failed", e)
                emitError(
                    detail = e.message ?: "reconnect failed",
                    failureCategory = ConnectionFailureCategory.NetworkChangedReconnectFailed,
                )
                // Service-level cleanup acquires no further locks.
                cleanupAfterFailure()
            }
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!isReconnectCandidate(network)) {
                    Log.i(TAG, "network available ignored: not an underlying internet network")
                    return
                }
                val previous = lastSeenNetwork
                lastSeenNetwork = network
                // First default-network notification after registration:
                // just record it; the tunnel is already established.
                if (previous == null || previous == network) {
                    Log.i(TAG, "network available recorded")
                    return
                }
                scope.launch { reconnectOnNetworkChange("default network changed") }
            }

            override fun onLost(network: Network) {
                if (lastSeenNetwork == network) {
                    lastSeenNetwork = null
                    TunnelController.publishStatus(TunnelStatus.Reconnecting)
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
            Log.i(TAG, "registered default network callback")
        } catch (e: SecurityException) {
            Log.w(TAG, "register network callback failed", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val cb = networkCallback ?: return
        Log.i(TAG, "unregister network callback")
        runCatching { cm.unregisterNetworkCallback(cb) }
        networkCallback = null
        lastSeenNetwork = null
    }

    private fun isReconnectCandidate(network: Network): Boolean {
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun emitError(
        detail: String,
        failureCategory: ConnectionFailureCategory,
    ) {
        Log.w(TAG, "emitError detail=${detail.redactForLog()}")
        TunnelController.publishStatus(
            next = TunnelStatus.Error,
            detail = detail,
            failureCategory = failureCategory,
        )
    }

    private fun String.redactForLog(): String =
        replace(Regex("(?i)\\b(vless|vmess|trojan|ss)://\\S+"), "\$1://<redacted-profile-uri>")
            .replace(Regex("(?i)\\bhttps?://\\S+"), "<redacted-url>")
            .replace(
                Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"),
                "<uuid>",
            )
            .replace(Regex("(?i)\\b(password|token|pbk|sid|spx)=([^&\\s]+)"), "\$1=<redacted>")
            .replace(Regex("(?i)\\b(Authorization|Cookie|X-Api-Key):\\s*\\S+"), "\$1: <redacted>")
            .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "<ipv4>")
            .replace(
                Regex("(?i)\\b(dial tcp|lookup|server|address|host|destination)\\s+([A-Za-z0-9_.-]+)(:\\d+)?"),
                "\$1 <redacted-host>\$3",
            )
            .take(MAX_LOG_ERROR_LENGTH)

    private fun closeTun(reason: String) {
        val pfd = tunInterface ?: return
        Log.i(TAG, "closeTun reason=$reason fd=${pfd.fd} pfd=${pfd.identity()}")
        runCatching { pfd.close() }
            .onFailure { Log.w(TAG, "closeTun threw", it) }
        tunInterface = null
    }

    private fun ParcelFileDescriptor.identity(): String =
        Integer.toHexString(System.identityHashCode(this))

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_tunnel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val title = getString(
            R.string.notif_tunnel_title,
            activeProfileName.ifEmpty { getString(R.string.notif_tunnel_idle) },
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Picks a free TCP port on 127.0.0.1 by binding a server socket
     * with port 0, reading the kernel-assigned port, and closing.
     * Returns null if the kernel can't satisfy the request — we fall
     * back to the static default rather than refuse to connect.
     *
     * Yes, there is a TOCTOU window between close and Xray's bind:
     * mitigated by (a) the port is loopback-only, (b) the window is
     * microseconds, (c) Xray would simply fail to bind and the
     * tunnel start path would surface a clear error.
     */
    private fun pickEphemeralLoopbackPort(): Int? = try {
        java.net.ServerSocket().use { socket ->
            socket.bind(
                java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0),
            )
            socket.localPort.takeIf { it in 1..65_535 }
        }
    } catch (e: Throwable) {
        Log.w(TAG, "ephemeral port allocation failed", e)
        null
    }

    private fun bytesPerSecond(currentBytes: Long, prevBytes: Long?, intervalMs: Long): Long {
        if (prevBytes == null) return 0L
        val delta = currentBytes - prevBytes
        if (delta <= 0L) return 0L
        return (delta * 1000L) / max(intervalMs, 1L)
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1L shl 30 -> String.format("%.2f GB", bytes.toDouble() / (1L shl 30))
        bytes >= 1L shl 20 -> String.format("%.1f MB", bytes.toDouble() / (1L shl 20))
        bytes >= 1L shl 10 -> String.format("%.0f KB", bytes.toDouble() / (1L shl 10))
        else -> "$bytes B"
    }

    companion object {
        const val ACTION_START = "com.gmvpn.client.tunnel.START"
        const val ACTION_STOP = "com.gmvpn.client.tunnel.STOP"

        // Tunnel constants. The /28 + /112 give us a tiny private subnet
        // — only the gateway address is needed since gVisor handles the
        // packet flow. DNS literals are user-replaceable later.
        private const val TUN_MTU = 1500
        private const val TUN_ADDRESS_V4 = "10.10.10.2"
        private const val TUN_PREFIX_V4 = 28
        private const val TUN_ADDRESS_V6 = "fd00:0:0:1::2"
        private const val TUN_PREFIX_V6 = 112
        private const val DNS_PRIMARY = "1.1.1.1"
        private const val DNS_SECONDARY = "8.8.8.8"

        private const val CHANNEL_ID = "gmvpn.tunnel"
        private const val NOTIFICATION_ID = 0x67_6d_76_6e // "gmvn"
        private const val TAG = "GmvpnVpnService"
        private const val STATS_INTERVAL_MS = 2_000L
        private const val MAX_LOG_ERROR_LENGTH = 500
    }
}
