package com.gmvpn.client.connection

/**
 * Pure domain description of one future connection attempt.
 *
 * This model is intentionally not wired to the current runtime yet. It
 * carries references and policy choices only. Private profile material stays
 * in the existing trusted profile and runtime layers.
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

/**
 * Stable reference to a persisted profile entry.
 *
 * The value is an app-local reference, not display text and not copied profile
 * material.
 */
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

/**
 * Routing policy for a future connection attempt.
 *
 * The variants intentionally keep allow-list and bypass-list semantics
 * mutually exclusive. Android's VPN builder does not allow applying both
 * styles to one connection plan.
 */
sealed interface RoutingMode {
    data object AllApps : RoutingMode

    data class SelectedAppsOnly(
        val packageNames: Set<String>,
    ) : RoutingMode

    data class AllExceptSelected(
        val packageNames: Set<String>,
    ) : RoutingMode
}

val RoutingMode.isValid: Boolean
    get() = when (this) {
        RoutingMode.AllApps -> true
        is RoutingMode.SelectedAppsOnly -> packageNames.isNotEmpty()
        is RoutingMode.AllExceptSelected -> true
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
