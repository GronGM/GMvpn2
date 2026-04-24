package com.gmvpn.client.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gmvpn.client.R
import com.gmvpn.client.ui.MainActivity

/**
 * Android tunnel entry point. Responsibilities:
 *
 *  1. Run as a foreground service while the tunnel is up.
 *  2. Establish the TUN interface with parameters the engine asks for.
 *  3. Hand the TUN fd to the Go wrapper and forward its status events
 *     back to [TunnelController].
 *
 *  Step 2–3 will be wired once `core/build/gmvpn.aar` is available. This
 *  class deliberately keeps lifecycle and notification handling real so
 *  integrating the engine is a surgical change, not a rewrite.
 */
class GmvpnVpnService : VpnService() {

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
        TunnelController.publishStatus(TunnelStatus.Idle)
        super.onDestroy()
    }

    private fun handleStart() {
        startForeground(NOTIFICATION_ID, buildNotification())

        // TODO(engine): build Xray-core config from the active Profile
        // via shared/gmvpn-ffi, call Builder#establish() with the
        // parameters the config demands, and hand the fd to
        // com.gmvpn.core (gomobile-bound).
        TunnelController.publishStatus(
            TunnelStatus.Error,
            detail = getString(R.string.engine_missing_body),
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleStop() {
        // TODO(engine): tell the Go wrapper to tear the tunnel down,
        // close the TUN fd, and only then stopSelf().
        TunnelController.publishStatus(TunnelStatus.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_tunnel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_tunnel_title))
            .setContentText(getString(R.string.notif_tunnel_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.gmvpn.client.tunnel.START"
        const val ACTION_STOP = "com.gmvpn.client.tunnel.STOP"

        private const val CHANNEL_ID = "gmvpn.tunnel"
        private const val NOTIFICATION_ID = 0x67_6d_76_6e // "gmvn"
    }
}
