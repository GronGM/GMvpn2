package com.gmvpn.client.tunnel

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailure
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState

/**
 * Internal shadow runtime for the future ConnectionState model.
 *
 * The current UI and service decisions still use TunnelStatus. This helper only
 * keeps redaction-safe evidence beside that flow so Stage 1 can prove the
 * no-fake-Connected invariants before any user-visible adoption.
 */
internal class ConnectionStateShadowRuntime {

    var evidence: ConnectionEvidence = ConnectionEvidence()
        private set

    var state: ConnectionState = ConnectionState.Idle
        private set

    fun reset() {
        evidence = ConnectionEvidence()
        state = ConnectionState.Idle
    }

    fun preparePermission() {
        evidence = ConnectionEvidence()
        state = ConnectionState.Preparing
    }

    fun startWithPreparedPermission() {
        evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
        )
        state = ConnectionState.StartingEngine
    }

    fun permissionDenied() {
        fail(ConnectionFailureCategory.VpnPermissionDenied)
    }

    fun markVpnInterfaceEstablished() {
        evidence = evidence.copy(
            vpnInterfaceEstablished = true,
        )
    }

    fun markEngineStarted() {
        evidence = evidence.copy(
            engineStarted = true,
        )
    }

    fun reconnectingWithPreparedPermission() {
        evidence = ConnectionEvidence(
            vpnPermissionPrepared = true,
        )
        state = ConnectionState.Connecting
    }

    fun disconnecting() {
        evidence = ConnectionEvidence()
        state = ConnectionState.Disconnecting
    }

    fun fail(category: ConnectionFailureCategory) {
        val failure = ConnectionFailure(category = category)
        evidence = evidence.copy(
            immediateFailure = failure,
        )
        state = ConnectionState.Failed(failure)
    }

    fun publishStatus(
        status: TunnelStatus,
        failureCategory: ConnectionFailureCategory? = null,
    ) {
        val failure = failureCategory?.let { ConnectionFailure(category = it) }
        state = when (status) {
            TunnelStatus.Idle -> {
                evidence = ConnectionEvidence()
                ConnectionState.Idle
            }
            TunnelStatus.Preparing -> {
                evidence = ConnectionEvidence()
                ConnectionState.Preparing
            }
            TunnelStatus.Starting -> ConnectionState.StartingEngine
            TunnelStatus.Connected -> status.toConnectionState(evidence)
            TunnelStatus.Reconnecting -> {
                reconnectingWithPreparedPermission()
                ConnectionState.Connecting
            }
            TunnelStatus.Stopping -> {
                evidence = ConnectionEvidence()
                ConnectionState.Disconnecting
            }
            TunnelStatus.Error -> {
                val typedFailure = failure ?: ConnectionFailure(
                    category = ConnectionFailureCategory.Unknown,
                )
                evidence = evidence.copy(
                    immediateFailure = typedFailure,
                )
                ConnectionState.Failed(typedFailure)
            }
        }
    }
}
