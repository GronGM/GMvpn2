package com.gmvpn.client.connection

/**
 * Structured evidence collected by a future orchestrator.
 *
 * It intentionally contains booleans and categories, not raw exception text,
 * private profile material, or diagnostics dumps.
 */
data class ConnectionEvidence(
    val vpnPermissionPrepared: Boolean = false,
    val vpnInterfaceEstablished: Boolean = false,
    val engineStarted: Boolean = false,
    val androidVpnNetworkVisible: Boolean = false,
    val trafficProbe: TrafficProbeEvidence = TrafficProbeEvidence.NotRun,
    val immediateFailure: ConnectionFailure? = null,
) {
    /**
     * Minimum evidence required before a future orchestrator may expose a
     * normal connected state.
     *
     * Android VPN network validation and traffic probes are stronger release
     * evidence, but they are intentionally separate so routing-source semantics
     * can be designed before making them mandatory in the runtime model.
     */
    val hasMinimumVpnPathEvidence: Boolean
        get() = vpnPermissionPrepared &&
            vpnInterfaceEstablished &&
            engineStarted &&
            immediateFailure == null

    /**
     * Stronger evidence that Android currently exposes a VPN network for this
     * app process. This is not required for the first domain model, but it is
     * tracked separately because release smoke used it as validation evidence.
     */
    val hasAndroidValidatedVpnEvidence: Boolean
        get() = hasMinimumVpnPathEvidence && androidVpnNetworkVisible
}

/**
 * Optional traffic-probe evidence.
 *
 * A failed probe does not erase the minimum VPN path evidence by itself. A
 * future orchestrator may map that combination to [ConnectionState.Degraded].
 */
sealed interface TrafficProbeEvidence {
    data object NotRun : TrafficProbeEvidence
    data object Passed : TrafficProbeEvidence
    data class Failed(val category: ConnectionFailureCategory) : TrafficProbeEvidence
}

data class ConnectionFailure(
    val category: ConnectionFailureCategory,
)

/**
 * Typed, redaction-safe failure category.
 *
 * The category is safe to carry through UI and diagnostics. Free-form runtime
 * text should be categorized or redacted before leaving lower layers.
 */
enum class ConnectionFailureCategory {
    NoProfile,
    InvalidProfile,
    UnsupportedProfileScheme,
    ProfileParseFailed,
    ConfigBuildFailed,
    VpnPermissionDenied,
    VpnInterfaceNotEstablished,
    EngineUnavailable,
    EngineStartFailed,
    DnsFailure,
    ServerUnreachable,
    HandshakeFailure,
    UdpLimited,
    Ipv6NotTested,
    PerAppRoutingInvalid,
    NetworkChangedReconnectFailed,
    CancelledByUser,
    Unknown,
}
