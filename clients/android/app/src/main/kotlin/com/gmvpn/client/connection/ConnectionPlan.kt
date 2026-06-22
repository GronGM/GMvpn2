package com.gmvpn.client.connection

/**
 * Pure domain description of one future connection attempt.
 *
 * This model is intentionally not wired to the current runtime yet. It
 * carries references and policy choices only; raw profile URIs and endpoint
 * values must stay in the existing trusted profile/runtime layers.
 */
data class ConnectionPlan(
    val profileRef: ProfileRef,
    val engine: EngineKind = EngineKind.XRAY,
    val routingMode: RoutingMode = RoutingMode.AllApps,
    val transportMode: TransportMode = TransportMode.Direct,
    val dnsPolicy: DnsPolicy = DnsPolicy.Default,
    val diagnosticsPolicy: DiagnosticsPolicy = DiagnosticsPolicy.Default,
    val redactionPolicy: RedactionPolicy = RedactionPolicy.Strict,
)

@JvmInline
value class ProfileRef(val value: String) {
    init {
        require(value.isNotBlank()) { "profileRef must be non-blank" }
    }
}

enum class EngineKind {
    XRAY,
    SING_BOX_EXPERIMENTAL,
}

sealed interface RoutingMode {
    data object AllApps : RoutingMode

    data class SelectedAppsOnly(
        val packageNames: Set<String>,
    ) : RoutingMode

    data class AllExceptSelected(
        val packageNames: Set<String>,
    ) : RoutingMode
}

enum class TransportMode {
    Direct,
    LocalForwardExperimental,
    Hysteria2ViaXray,
    TurnExperimental,
    SshExperimentalLater,
}

enum class DnsPolicy {
    Default,
}

enum class DiagnosticsPolicy {
    Default,
}

enum class RedactionPolicy {
    Strict,
}
