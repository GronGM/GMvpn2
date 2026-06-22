package com.gmvpn.client.diagnostics

import com.gmvpn.client.connection.ConnectionEvidence
import com.gmvpn.client.connection.ConnectionFailureCategory
import com.gmvpn.client.connection.ConnectionState
import com.gmvpn.client.connection.TrafficProbeEvidence

/**
 * Internal typed preview for future redacted diagnostics.
 *
 * This is deliberately not wired into the user-facing diagnostics export yet.
 * It carries only categories and booleans so it cannot transport raw profile
 * material, endpoint details, lower-layer exception text, or device dumps.
 */
internal data class ConnectionStateDiagnosticsPreview(
    val connectionStateCategory: ConnectionStateDiagnosticsCategory,
    val failureCategory: ConnectionFailureCategory?,
    val hasVpnPermissionEvidence: Boolean,
    val hasVpnInterfaceEvidence: Boolean,
    val hasEngineStartedEvidence: Boolean,
    val hasImmediateFailure: Boolean,
    val androidVpnNetworkVisible: Boolean,
    val trafficProbeStatus: ConnectionTrafficProbeStatus,
) {

    fun toDiagnosticFields(): Map<String, String> =
        linkedMapOf(
            "connection_state_category" to connectionStateCategory.name,
            "failure_category" to (failureCategory?.name ?: "none"),
            "has_vpn_permission_evidence" to hasVpnPermissionEvidence.toString(),
            "has_vpn_interface_evidence" to hasVpnInterfaceEvidence.toString(),
            "has_engine_started_evidence" to hasEngineStartedEvidence.toString(),
            "has_immediate_failure" to hasImmediateFailure.toString(),
            "android_vpn_network_visible" to androidVpnNetworkVisible.toString(),
            "traffic_probe_status" to trafficProbeStatus.name,
        )

    companion object {

        fun from(
            state: ConnectionState,
            evidence: ConnectionEvidence = state.evidenceOrEmpty(),
        ): ConnectionStateDiagnosticsPreview {
            val effectiveEvidence = when (state) {
                is ConnectionState.Connected -> state.evidence
                is ConnectionState.Degraded -> state.evidence
                else -> evidence
            }
            return ConnectionStateDiagnosticsPreview(
                connectionStateCategory = state.toDiagnosticsCategory(),
                failureCategory = state.failureCategoryOrNull()
                    ?: effectiveEvidence.immediateFailure?.category,
                hasVpnPermissionEvidence = effectiveEvidence.vpnPermissionPrepared,
                hasVpnInterfaceEvidence = effectiveEvidence.vpnInterfaceEstablished,
                hasEngineStartedEvidence = effectiveEvidence.engineStarted,
                hasImmediateFailure = effectiveEvidence.immediateFailure != null ||
                    state is ConnectionState.Failed,
                androidVpnNetworkVisible = effectiveEvidence.androidVpnNetworkVisible,
                trafficProbeStatus = effectiveEvidence.trafficProbe.toDiagnosticsStatus(),
            )
        }

        private fun ConnectionState.evidenceOrEmpty(): ConnectionEvidence =
            when (this) {
                is ConnectionState.Connected -> evidence
                is ConnectionState.Degraded -> evidence
                else -> ConnectionEvidence()
            }

        private fun ConnectionState.toDiagnosticsCategory(): ConnectionStateDiagnosticsCategory =
            when (this) {
                ConnectionState.Idle -> ConnectionStateDiagnosticsCategory.Idle
                ConnectionState.Preparing -> ConnectionStateDiagnosticsCategory.Preparing
                ConnectionState.StartingVpnService -> ConnectionStateDiagnosticsCategory.StartingVpnService
                ConnectionState.StartingEngine -> ConnectionStateDiagnosticsCategory.StartingEngine
                ConnectionState.Connecting -> ConnectionStateDiagnosticsCategory.Connecting
                is ConnectionState.Connected -> ConnectionStateDiagnosticsCategory.Connected
                is ConnectionState.Degraded -> ConnectionStateDiagnosticsCategory.Degraded
                is ConnectionState.Failed -> ConnectionStateDiagnosticsCategory.Failed
                ConnectionState.Disconnecting -> ConnectionStateDiagnosticsCategory.Disconnecting
            }

        private fun ConnectionState.failureCategoryOrNull(): ConnectionFailureCategory? =
            when (this) {
                is ConnectionState.Degraded -> reason
                is ConnectionState.Failed -> failure.category
                else -> null
            }

        private fun TrafficProbeEvidence.toDiagnosticsStatus(): ConnectionTrafficProbeStatus =
            when (this) {
                TrafficProbeEvidence.NotRun -> ConnectionTrafficProbeStatus.NotRun
                TrafficProbeEvidence.Passed -> ConnectionTrafficProbeStatus.Passed
                is TrafficProbeEvidence.Failed -> ConnectionTrafficProbeStatus.Failed
            }
    }
}

internal enum class ConnectionStateDiagnosticsCategory {
    Idle,
    Preparing,
    StartingVpnService,
    StartingEngine,
    Connecting,
    Connected,
    Degraded,
    Failed,
    Disconnecting,
}

internal enum class ConnectionTrafficProbeStatus {
    NotRun,
    Passed,
    Failed,
}
