package com.gmvpn.client.connection

/**
 * Behavior-neutral state model for the future Connection Orchestrator.
 *
 * The current app does not consume this type yet. The invariant on
 * [Connected] prevents the new model from representing a connected state
 * without permission, VPN interface, and engine-start evidence.
 */
sealed interface ConnectionState {
    data object Idle : ConnectionState

    data object Preparing : ConnectionState

    data object StartingVpnService : ConnectionState

    data object StartingEngine : ConnectionState

    data object Connecting : ConnectionState

    /**
     * Terminal healthy state for the minimum path evidence.
     *
     * Stronger Android network visibility or traffic probe evidence can be
     * tracked separately without weakening this invariant.
     */
    data class Connected(
        val evidence: ConnectionEvidence,
    ) : ConnectionState {
        init {
            require(evidence.hasMinimumVpnPathEvidence) {
                "Connected requires permission, VPN interface, and engine evidence"
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
