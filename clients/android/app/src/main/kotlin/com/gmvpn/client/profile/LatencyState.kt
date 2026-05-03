package com.gmvpn.client.profile

/**
 * Per-profile latency-probe state surfaced to the UI. Not a domain
 * model — it lives next to the UI to keep [ProfileStore] free of
 * ephemeral test results.
 */
sealed class LatencyState {
    /** Never probed (or library was edited since the last probe). */
    object Idle : LatencyState()

    /** A probe is in flight; the UI shows a spinner / dots. */
    object InFlight : LatencyState()

    /** Probe finished. `ms == null` means timed out / refused. */
    data class Result(val ms: Long?) : LatencyState()
}
