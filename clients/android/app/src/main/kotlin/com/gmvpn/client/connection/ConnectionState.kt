package com.gmvpn.client.connection

/**
 * Behavior-neutral state model for the future Connection Orchestrator.
 *
 * The current app does not consume this type yet. The invariant on
 * [Connected] prevents the new model from representing a connected state
 * without VPN interface and engine-start evidence.
 */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Preparing : ConnectionState
    data object StartingVpnService : ConnectionState
    data object StartingEngine : ConnectionState
    data object Connecting : ConnectionState

    data class Connected(
        val evidence: ConnectionEvidence,
    ) : ConnectionState {
        init {
            require(evidence.supportsConnectedState) {
                "Connected requires VPN interface and engine-start evidence"
            }
        }
    }

    data class Degraded(
        val evidence: ConnectionEvidence,
        val reason: ConnectionFailureCategory,
    ) : ConnectionState

    data class Failed(
        val failure: ConnectionFailure,
    ) : ConnectionState

    data object Disconnecting : ConnectionState
}
