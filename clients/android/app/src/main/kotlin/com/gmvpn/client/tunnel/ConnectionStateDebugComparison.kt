package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionState

/**
 * Debug/test-only comparison between the current TunnelStatus flow and the
 * shadow ConnectionState model.
 *
 * This type is deliberately not wired into UI, notifications, service
 * decisions, or release-visible diagnostics.
 */
internal data class ConnectionStateDebugComparison(
    val statusCategory: TunnelStatusDebugCategory,
    val connectionStateCategory: ConnectionStateDebugCategory,
    val mismatchCategory: ConnectionStateMismatchCategory,
    val hasMinimumConnectedEvidence: Boolean,
    val hasImmediateFailure: Boolean,
    val isDebugOnly: Boolean = true,
    val isReleaseVisible: Boolean = false,
) {

    fun toDebugFields(): Map<String, String> =
        linkedMapOf(
            "status_category" to statusCategory.wireName,
            "connection_state_category" to connectionStateCategory.wireName,
            "mismatch_category" to mismatchCategory.wireName,
            "has_minimum_connected_evidence" to hasMinimumConnectedEvidence.toString(),
            "has_immediate_failure" to hasImmediateFailure.toString(),
            "is_debug_only" to isDebugOnly.toString(),
            "is_release_visible" to isReleaseVisible.toString(),
        )

    companion object {

        fun from(
            legacyStatus: TunnelStatus,
            shadowState: ConnectionState,
            shadowEvidence: ConnectionEvidence = shadowState.evidenceOrEmpty(),
        ): ConnectionStateDebugComparison {
            val effectiveEvidence = shadowState.effectiveEvidence(shadowEvidence)
            return ConnectionStateDebugComparison(
                statusCategory = legacyStatus.toDebugCategory(),
                connectionStateCategory = shadowState.toDebugCategory(),
                mismatchCategory = detectMismatch(
                    legacyStatus = legacyStatus,
                    shadowState = shadowState,
                    effectiveEvidence = effectiveEvidence,
                ),
                hasMinimumConnectedEvidence = effectiveEvidence.hasMinimumVpnPathEvidence,
                hasImmediateFailure = effectiveEvidence.immediateFailure != null ||
                    shadowState is ConnectionState.Failed,
            )
        }

        private fun ConnectionState.effectiveEvidence(
            fallback: ConnectionEvidence,
        ): ConnectionEvidence =
            when (this) {
                is ConnectionState.Connected -> evidence
                is ConnectionState.Degraded -> evidence
                else -> fallback
            }

        private fun ConnectionState.evidenceOrEmpty(): ConnectionEvidence =
            effectiveEvidence(ConnectionEvidence())

        private fun detectMismatch(
            legacyStatus: TunnelStatus,
            shadowState: ConnectionState,
            effectiveEvidence: ConnectionEvidence,
        ): ConnectionStateMismatchCategory =
            when {
                legacyStatus == TunnelStatus.Connected &&
                    shadowState is ConnectionState.Connected &&
                    effectiveEvidence.hasMinimumVpnPathEvidence ->
                    ConnectionStateMismatchCategory.None

                legacyStatus == TunnelStatus.Connected ->
                    ConnectionStateMismatchCategory.LegacyConnectedWithoutMinimumEvidence

                shadowState is ConnectionState.Connected ->
                    ConnectionStateMismatchCategory.ShadowConnectedLegacyNotConnected

                legacyStatus == TunnelStatus.Error && shadowState.isStartingLike() ->
                    ConnectionStateMismatchCategory.LegacyFailedNewConnecting

                shadowState == ConnectionState.Preparing &&
                    legacyStatus != TunnelStatus.Preparing ->
                    ConnectionStateMismatchCategory.StalePreparing

                legacyStatus.isStartingLike() && shadowState is ConnectionState.Failed ->
                    ConnectionStateMismatchCategory.LegacyConnectingNewFailed

                legacyStatus.alignsWith(shadowState) ->
                    ConnectionStateMismatchCategory.None

                else -> ConnectionStateMismatchCategory.Unknown
            }

        private fun TunnelStatus.isStartingLike(): Boolean =
            this == TunnelStatus.Preparing ||
                this == TunnelStatus.Starting ||
                this == TunnelStatus.Reconnecting

        private fun ConnectionState.isStartingLike(): Boolean =
            this == ConnectionState.Preparing ||
                this == ConnectionState.StartingVpnService ||
                this == ConnectionState.StartingEngine ||
                this == ConnectionState.Connecting

        private fun TunnelStatus.alignsWith(state: ConnectionState): Boolean =
            when (this) {
                TunnelStatus.Idle -> state == ConnectionState.Idle
                TunnelStatus.Preparing -> state == ConnectionState.Preparing
                TunnelStatus.Starting -> state == ConnectionState.StartingVpnService ||
                    state == ConnectionState.StartingEngine ||
                    state == ConnectionState.Connecting
                TunnelStatus.Connected -> state is ConnectionState.Connected
                TunnelStatus.Reconnecting -> state == ConnectionState.Connecting
                TunnelStatus.Stopping -> state == ConnectionState.Disconnecting ||
                    state == ConnectionState.Idle
                TunnelStatus.Error -> state is ConnectionState.Failed
            }

        private fun TunnelStatus.toDebugCategory(): TunnelStatusDebugCategory =
            when (this) {
                TunnelStatus.Idle -> TunnelStatusDebugCategory.Idle
                TunnelStatus.Preparing -> TunnelStatusDebugCategory.Preparing
                TunnelStatus.Starting -> TunnelStatusDebugCategory.Starting
                TunnelStatus.Connected -> TunnelStatusDebugCategory.Connected
                TunnelStatus.Reconnecting -> TunnelStatusDebugCategory.Reconnecting
                TunnelStatus.Stopping -> TunnelStatusDebugCategory.Stopping
                TunnelStatus.Error -> TunnelStatusDebugCategory.Error
            }

        private fun ConnectionState.toDebugCategory(): ConnectionStateDebugCategory =
            when (this) {
                ConnectionState.Idle -> ConnectionStateDebugCategory.Idle
                ConnectionState.Preparing -> ConnectionStateDebugCategory.Preparing
                ConnectionState.StartingVpnService -> ConnectionStateDebugCategory.StartingVpnService
                ConnectionState.StartingEngine -> ConnectionStateDebugCategory.StartingEngine
                ConnectionState.Connecting -> ConnectionStateDebugCategory.Connecting
                is ConnectionState.Connected -> ConnectionStateDebugCategory.Connected
                is ConnectionState.Degraded -> ConnectionStateDebugCategory.Degraded
                is ConnectionState.Failed -> ConnectionStateDebugCategory.Failed
                ConnectionState.Disconnecting -> ConnectionStateDebugCategory.Disconnecting
            }
    }
}

internal enum class TunnelStatusDebugCategory(
    val wireName: String,
) {
    Idle("idle"),
    Preparing("preparing"),
    Starting("starting"),
    Connected("connected"),
    Reconnecting("reconnecting"),
    Stopping("stopping"),
    Error("error"),
}

internal enum class ConnectionStateDebugCategory(
    val wireName: String,
) {
    Idle("idle"),
    Preparing("preparing"),
    StartingVpnService("starting_vpn_service"),
    StartingEngine("starting_engine"),
    Connecting("connecting"),
    Connected("connected"),
    Degraded("degraded"),
    Failed("failed"),
    Disconnecting("disconnecting"),
}

internal enum class ConnectionStateMismatchCategory(
    val wireName: String,
) {
    None("none"),
    LegacyConnectedWithoutMinimumEvidence("legacy_connected_without_minimum_evidence"),
    LegacyConnectingNewFailed("legacy_connecting_new_failed"),
    LegacyFailedNewConnecting("legacy_failed_new_connecting"),
    ShadowConnectedLegacyNotConnected("shadow_connected_legacy_not_connected"),
    StalePreparing("stale_preparing"),
    Unknown("unknown"),
}
