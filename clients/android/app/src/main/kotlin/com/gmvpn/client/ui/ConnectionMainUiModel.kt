package com.gmvpn.client.ui

import com.gmvpn.client.connection.ConnectionState

internal data class ConnectionMainUiModel(
    val category: ConnectionMainUiCategory,
    val action: ConnectionMainAction,
) {

    val rendersConnected: Boolean
        get() = category == ConnectionMainUiCategory.Connected

    val actionEnabled: Boolean
        get() = category == ConnectionMainUiCategory.Disconnected ||
            category == ConnectionMainUiCategory.Connected ||
            category == ConnectionMainUiCategory.Error

    fun safeDiagnosticFields(): Map<String, String> =
        mapOf(
            "main_ui_connection_category" to category.safeValue,
            "main_ui_connection_action" to action.safeValue,
        )
}

internal enum class ConnectionMainUiCategory(
    val safeValue: String,
) {
    NeedsProfile("needs_profile"),
    Disconnected("disconnected"),
    Connecting("connecting"),
    Connected("connected"),
    Disconnecting("disconnecting"),
    Error("error"),
}

internal enum class ConnectionMainAction(
    val safeValue: String,
) {
    Connect("connect"),
    Connecting("connecting"),
    Disconnect("disconnect"),
    Disconnecting("disconnecting"),
    Retry("retry"),
}

internal fun ConnectionState.toMainUiModel(
    hasProfile: Boolean,
    hasUserVisibleError: Boolean,
): ConnectionMainUiModel {
    val category = when {
        hasUserVisibleError -> ConnectionMainUiCategory.Error
        !hasProfile -> ConnectionMainUiCategory.NeedsProfile
        this is ConnectionState.Connected -> ConnectionMainUiCategory.Connected
        this == ConnectionState.Preparing ||
            this == ConnectionState.StartingVpnService ||
            this == ConnectionState.StartingEngine ||
            this == ConnectionState.Connecting -> ConnectionMainUiCategory.Connecting
        this == ConnectionState.Disconnecting -> ConnectionMainUiCategory.Disconnecting
        this is ConnectionState.Failed -> ConnectionMainUiCategory.Error
        else -> ConnectionMainUiCategory.Disconnected
    }
    val action = when (category) {
        ConnectionMainUiCategory.Connected -> ConnectionMainAction.Disconnect
        ConnectionMainUiCategory.Connecting -> ConnectionMainAction.Connecting
        ConnectionMainUiCategory.Disconnecting -> ConnectionMainAction.Disconnecting
        ConnectionMainUiCategory.Error -> ConnectionMainAction.Retry
        ConnectionMainUiCategory.NeedsProfile,
        ConnectionMainUiCategory.Disconnected -> ConnectionMainAction.Connect
    }
    return ConnectionMainUiModel(
        category = category,
        action = action,
    )
}
