package com.gmvpn.client.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gmvpn.client.R
import com.gmvpn.client.profile.ProfileStore
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
    private var tunInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var activeProfileName: String = ""

    private val cm by lazy { getSystemService(ConnectivityManager::class.java) }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastSeenNetwork: Network? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            else -> handleStop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onRevoke() {
        // System revoked VPN permission (e.g. another VPN app took over).
        handleStop()
        super.onRevoke()
    }

    override fun onDestroy() {
        unregisterNetworkCallback()
        scope.cancel()
        TunnelController.publishStatus(TunnelStatus.Idle)
        super.onDestroy()
    }

    private fun handleStart() {
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_tunnel_idle)))
        scope.launch {
            tunnelMutex.withLock {
                try {
                    bringTunnelUp()
                    registerNetworkCallback()
                } catch (e: Throwable) {
                    Log.e(TAG, "tunnel start failed", e)
                    emitError(e.message ?: "tunnel failed to start")
                    cleanupAfterFailure()
                }
            }
        }
    }

    private suspend fun bringTunnelUp() {
        val store = ProfileStore(applicationContext)
        val uri = store.activeUri.firstOrNull()
        if (uri.isNullOrBlank()) {
            emitError(getString(R.string.engine_missing_body))
            cleanupAfterFailure()
            return
        }

        val profile: FfiProfile = try {
            parseProfileUri(uri)
        } catch (e: GmvpnException) {
            emitError("profile URI: ${e.message}")
            cleanupAfterFailure()
            return
        }

        val opts = defaultTunnelOptions()
        val configJson = try {
            buildXrayConfig(profile, opts)
        } catch (e: GmvpnException) {
            emitError("config build: ${e.message}")
            cleanupAfterFailure()
            return
        }

        val pfd = establishTun(profile)
        if (pfd == null) {
            emitError("VpnService.establish() returned null")
            cleanupAfterFailure()
            return
        }
        tunInterface = pfd

        try {
            engine.start(
                configJson = configJson,
                tunFd = pfd.fd,
                mtu = TUN_MTU,
                socksPort = opts.socksPort.toInt(),
                listener = ::onEngineStatus,
            )
        } catch (e: EngineUnavailableException) {
            emitError(e.message ?: "engine missing")
            cleanupAfterFailure()
            return
        } catch (e: EngineStartException) {
            emitError(e.cause?.message ?: e.message ?: "engine start failed")
            cleanupAfterFailure()
            return
        }

        activeProfileName = profile.name
        TunnelController.publishStatus(TunnelStatus.Connected)
        startStatsLoop()
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

    private fun establishTun(profile: FfiProfile): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession(profile.name)
            .setMtu(TUN_MTU)
            .addAddress(TUN_ADDRESS_V4, TUN_PREFIX_V4)
            .addAddress(TUN_ADDRESS_V6, TUN_PREFIX_V6)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer(DNS_PRIMARY)
            .addDnsServer(DNS_SECONDARY)

        // Exclude our own app from the tunnel so the SOCKS inbound on
        // 127.0.0.1 stays reachable from the engine's outbound dialer.
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "disallow self failed", e)
        }

        return builder.establish()
    }

    private fun onEngineStatus(status: String, detail: String) {
        val next = TunnelStatus.fromEngine(status)
        if (next == TunnelStatus.Error) {
            emitError(if (detail.isNotBlank()) detail else "engine error")
        } else {
            TunnelController.publishStatus(next)
        }
    }

    private fun handleStop() {
        // Drop the network watcher first so an in-flight handover can't
        // race with the teardown.
        unregisterNetworkCallback()
        scope.launch {
            tunnelMutex.withLock {
                stopStatsLoop()
                try {
                    engine.stop()
                } catch (e: Throwable) {
                    Log.w(TAG, "engine stop threw", e)
                }
                tunInterface?.close()
                tunInterface = null
                activeProfileName = ""
            }
            TunnelController.publishStatus(TunnelStatus.Idle)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cleanupAfterFailure() {
        unregisterNetworkCallback()
        stopStatsLoop()
        runCatching { engine.stop() }
        tunInterface?.close()
        tunInterface = null
        activeProfileName = ""
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
            runCatching { engine.stop() }
            tunInterface?.close()
            tunInterface = null
            try {
                bringTunnelUp()
            } catch (e: Throwable) {
                Log.e(TAG, "reconnect failed", e)
                emitError(e.message ?: "reconnect failed")
                // Service-level cleanup acquires no further locks.
                cleanupAfterFailure()
            }
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val previous = lastSeenNetwork
                lastSeenNetwork = network
                // First default-network notification after registration:
                // just record it; the tunnel is already established.
                if (previous == null || previous == network) return
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
        } catch (e: SecurityException) {
            Log.w(TAG, "register network callback failed", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val cb = networkCallback ?: return
        runCatching { cm.unregisterNetworkCallback(cb) }
        networkCallback = null
        lastSeenNetwork = null
    }

    private fun emitError(detail: String) {
        TunnelController.publishStatus(TunnelStatus.Error, detail = detail)
    }

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
    }
}
