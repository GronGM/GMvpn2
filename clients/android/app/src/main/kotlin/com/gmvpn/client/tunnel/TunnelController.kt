package com.gmvpn.client.tunnel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide facade around the VPN tunnel. The UI only talks to
 * this; it never touches [GmvpnVpnService] or the engine directly.
 */
object TunnelController {

    private val _status = MutableStateFlow(TunnelStatus.Idle)
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Returns the intent required by [VpnService.prepare] when the user
     * has not yet granted VPN permission, or null if permission is
     * already granted. Callers must launch the returned intent with
     * `startActivityForResult` and call [onPermissionGranted] on success.
     */
    fun preparePermission(context: Context): Intent? {
        _status.value = TunnelStatus.Preparing
        val intent = VpnService.prepare(context)
        if (intent == null) {
            _status.value = TunnelStatus.Idle
        }
        return intent
    }

    fun onPermissionGranted(context: Context) {
        requestStart(context)
    }

    fun requestStart(context: Context) {
        _lastError.value = null
        _status.value = TunnelStatus.Starting
        val intent = Intent(context, GmvpnVpnService::class.java).apply {
            action = GmvpnVpnService.ACTION_START
        }
        // The service promotes itself to foreground only after it has
        // an active profile to start. A no-profile request must be able
        // to return a user-visible error without tripping Android's
        // foreground-service timeout or systemExempted permission gate.
        context.startService(intent)
    }

    fun requestStop(context: Context) {
        _status.value = TunnelStatus.Stopping
        val intent = Intent(context, GmvpnVpnService::class.java).apply {
            action = GmvpnVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    /** Called by [GmvpnVpnService] as it transitions through states. */
    internal fun publishStatus(next: TunnelStatus, detail: String? = null) {
        _status.value = next
        if (next == TunnelStatus.Error) {
            _lastError.value = detail
        }
    }
}
