package com.gmvpn.client.connection

/**
 * Structured evidence collected by a future orchestrator.
 *
 * This model intentionally carries booleans and typed categories. It does not
 * carry free-form lower-layer text. It also does not carry profile material,
 * diagnostics dumps, or runtime implementation objects.
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
     * Minimum evidence required before the domain model can represent a normal
     * connected state.
     *
     * Minimum path evidence is deliberately stricter than engine startup
     * alone. Permission, VPN interface creation, and engine startup must all be
     * present, and no immediate fatal failure may be recorded.
     */
    val hasMinimumVpnPathEvidence: Boolean
        get() = vpnPermissionPrepared &&
            vpnInterfaceEstablished &&
            engineStarted &&
            immediateFailure == null

    /**
     * Stronger Android-visible evidence.
     *
     * This represents the release-smoke style observation that Android reports
     * a VPN network for the process in addition to the minimum path evidence.
     */
    val hasAndroidValidatedVpnEvidence: Boolean
        get() = hasMinimumVpnPathEvidence &&
            androidVpnNetworkVisible
}

/**
 * Optional traffic-probe evidence.
 *
 * A failed probe does not erase minimum path evidence by itself. A later
 * orchestrator pass may map that combination to [ConnectionState.Degraded]
 * after routing-source semantics are specified.
 */
sealed interface TrafficProbeEvidence {

    data object NotRun : TrafficProbeEvidence

    data object Passed : TrafficProbeEvidence

    data class Failed(
        val category: ConnectionFailureCategory,
    ) : TrafficProbeEvidence
}

/**
 * Typed failure value for the future state model.
 *
 * Keeping the value category-only prevents the first domain layer from
 * becoming a transport for unredacted lower-layer text.
 */
data class ConnectionFailure(
    val category: ConnectionFailureCategory,
)

/**
 * Redaction-safe failure category.
 *
 * These values are safe to use in tests and future diagnostics because they
 * are categorical rather than raw runtime text.
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
