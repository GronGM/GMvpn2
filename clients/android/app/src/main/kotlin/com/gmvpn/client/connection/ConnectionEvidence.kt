package com.gmvpn.client.connection

/**
 * Structured evidence collected by a future orchestrator.
 *
 * It intentionally contains booleans and categories, not raw exception text,
 * profile URIs, endpoint values, ports, UUIDs, or diagnostics dumps.
 */
data class ConnectionEvidence(
    val vpnInterfaceEstablished: Boolean = false,
    val engineStarted: Boolean = false,
    val androidVpnNetworkVisible: Boolean = false,
    val trafficProbe: TrafficProbeEvidence = TrafficProbeEvidence.NotRun,
    val immediateFailure: ConnectionFailure? = null,
) {
    val supportsConnectedState: Boolean
        get() = vpnInterfaceEstablished && engineStarted && immediateFailure == null
}

sealed interface TrafficProbeEvidence {
    data object NotRun : TrafficProbeEvidence
    data object Passed : TrafficProbeEvidence
    data class Failed(val category: ConnectionFailureCategory) : TrafficProbeEvidence
}

data class ConnectionFailure(
    val category: ConnectionFailureCategory,
)

enum class ConnectionFailureCategory {
    NoActiveProfile,
    UnsupportedProfileScheme,
    ProfileParseFailed,
    ConfigBuildFailed,
    VpnPermissionDenied,
    VpnInterfaceFailed,
    EngineUnavailable,
    EngineStartFailed,
    RoutingApplyFailed,
    DnsFailed,
    NetworkChangedReconnectFailed,
    CancelledByUser,
    Unknown,
}
