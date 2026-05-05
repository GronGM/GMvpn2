package com.gmvpn.client.routing

/**
 * How [GmvpnVpnService] applies the per-app package list when it
 * builds the TUN.
 *
 * - [Off]            — every app is tunneled (current default).
 * - [IncludeOnly]    — only the listed packages are tunneled. Apps
 *                      not in the list bypass the VPN entirely.
 * - [ExcludeListed]  — every app is tunneled except the listed ones.
 *
 * GMvpn itself is always excluded (regardless of mode) so the SOCKS
 * inbound on 127.0.0.1 stays reachable from the engine.
 */
enum class PerAppMode { Off, IncludeOnly, ExcludeListed }

/** Persisted setting applied at every TUN establishment. */
data class PerAppRouting(
    val mode: PerAppMode = PerAppMode.Off,
    val packages: Set<String> = emptySet(),
)
