package com.gmvpn.client.tunnel

/**
 * UI-facing tunnel state. Maps 1:1 to the string constants emitted by the
 * Go wrapper (`gmvpn.StatusIdle`, etc.) plus two Android-only states the
 * engine does not know about.
 */
enum class TunnelStatus {
    Idle,
    Preparing,
    Starting,
    Connected,
    Reconnecting,
    Stopping,
    Error,
    ;

    companion object {
        fun fromEngine(value: String): TunnelStatus = when (value) {
            "idle" -> Idle
            "starting" -> Starting
            "connected" -> Connected
            "reconnecting" -> Reconnecting
            "stopping" -> Stopping
            "error" -> Error
            else -> Error
        }
    }
}
