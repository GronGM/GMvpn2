package com.gmvpn.client.tunnel

/**
 * Snapshot of cumulative byte counters since the current tunnel started.
 * Mirrors `gmvpn.TrafficStats` on the Go side; we keep it as a local
 * class so the rest of the app does not depend on the gomobile types.
 */
data class TrafficStats(
    val uplinkBytes: Long,
    val downlinkBytes: Long,
)
