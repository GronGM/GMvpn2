package com.gmvpn.client.connection

/**
 * Pure domain description of one future connection attempt.
 *
 * This type is intentionally not wired to the current app runtime. It only
 * names the stable profile reference and the policies that a future
 * orchestrator may use when it builds a connection attempt.
 *
 * The current VPN service, tunnel controller, engine bridge, profile store,
 * and UI do not consume this model yet.
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
 * Stable app-local reference to a saved profile entry.
 *
 * The value is not user-facing display text. It is also not copied connection
 * material. That keeps this foundation model safe to pass through tests,
 * diagnostics categories, and future state transitions.
 */
@JvmInline
value class ProfileRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "profileRef must be non-blank"
        }
    }
}

/**
 * Engine selected for a future connection attempt.
 *
 * Xray is the only stable engine in the current product baseline. The
 * experimental value is a placeholder for future planning and is not runtime
 * wiring.
 */
enum class EngineKind {
    XRAY,

    SING_BOX_EXPERIMENTAL,
}

/**
 * Routing policy for a future connection attempt.
 *
 * The sealed variants keep include-list and exclude-list semantics mutually
 * exclusive. Android VPN builder policies must not mix those two modes in one
 * connection plan.
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

/**
 * Syntactic validity for the routing policy.
 *
 * An empty selected-apps-only list would leave no app covered by the plan, so
 * it is invalid at the domain level. An empty all-except-selected list is
 * valid because it means no additional bypass entries are configured.
 */
val RoutingMode.isValid: Boolean
    get() = when (this) {
        RoutingMode.AllApps -> true
        is RoutingMode.SelectedAppsOnly -> packageNames.isNotEmpty()
        is RoutingMode.AllExceptSelected -> true
    }

/**
 * Transport mode selected for a future attempt.
 *
 * Only [Direct] describes the current stable path.
 *
 * The other values remain behavior-neutral placeholders until a separate
 * implementation is approved.
 */
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
