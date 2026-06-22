package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState

/**
 * Behavior-neutral adapter from the current UI-facing tunnel state to the
 * future Connection Orchestrator state model.
 *
 * This mapper is intentionally not wired into runtime UI or service code yet.
 */
fun TunnelStatus.toConnectionState(
    evidence: ConnectionEvidence = ConnectionEvidence(),
    failure: ConnectionFailure? = null,
): ConnectionState =
    when (this) {
        TunnelStatus.Idle -> ConnectionState.Idle
        TunnelStatus.Preparing -> ConnectionState.Preparing
        TunnelStatus.Starting -> ConnectionState.StartingEngine
        TunnelStatus.Connected -> toConnectedConnectionState(evidence)
        TunnelStatus.Reconnecting -> ConnectionState.Connecting
        TunnelStatus.Stopping -> ConnectionState.Disconnecting
        TunnelStatus.Error -> ConnectionState.Failed(
            failure = failure ?: ConnectionFailure(
                category = ConnectionFailureCategory.Unknown,
            ),
        )
    }

private fun toConnectedConnectionState(
    evidence: ConnectionEvidence,
): ConnectionState =
    if (evidence.hasMinimumVpnPathEvidence) {
        ConnectionState.Connected(
            evidence = evidence,
        )
    } else {
        ConnectionState.Failed(
            failure = ConnectionFailure(
                category = ConnectionFailureCategory.VpnInterfaceNotEstablished,
            ),
        )
    }
