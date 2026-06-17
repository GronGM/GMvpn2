package com.gmvpn.client.diagnostics

import com.gmvpn.client.tunnel.TunnelStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RedactedDiagnosticsInput(
    val appVersion: String,
    val versionCode: Int,
    val packageName: String,
    val androidRelease: String,
    val androidSdk: Int,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val status: TunnelStatus,
    val lastErrorCategory: String,
    val selectedProtocolType: String?,
    val profileCount: Int,
    val timestampUtc: String = RedactedDiagnosticsReport.nowUtc(),
)

object RedactedDiagnosticsReport {

    fun render(input: RedactedDiagnosticsInput): String =
        buildString {
            appendLine("GMvpn Android bug report")
            appendLine("timestamp_utc: ${input.timestampUtc}")
            appendLine("app_version: ${input.appVersion}")
            appendLine("version_code: ${input.versionCode}")
            appendLine("package_name: ${input.packageName}")
            appendLine("android: ${input.androidRelease} / API ${input.androidSdk}")
            appendLine("device: ${deviceLabel(input)}")
            appendLine("connection_state: ${input.status.name}")
            appendLine("last_error_category: ${input.lastErrorCategory}")
            appendLine("selected_protocol_type: ${input.selectedProtocolType ?: "none"}")
            appendLine("saved_profile_count: ${input.profileCount}")
            appendLine("privacy: profile URIs, endpoints, UUIDs, passwords, tokens, and raw logs omitted")
        }

    fun categorizeLastError(message: String?): String {
        val text = message?.lowercase(Locale.ROOT).orEmpty()
        return when {
            text.isBlank() -> "none"
            "no active profile" in text || "no profile" in text -> "no_active_profile"
            "engine" in text || "artifact" in text || "unbundled" in text -> "engine_unavailable"
            "permission" in text || "denied" in text -> "vpn_permission"
            "invalid" in text || "malformed" in text || "parse" in text -> "invalid_profile"
            else -> "tunnel_error"
        }
    }

    fun nowUtc(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    private fun deviceLabel(input: RedactedDiagnosticsInput): String {
        val manufacturer = input.deviceManufacturer?.takeIf { it.isNotBlank() }
        val model = input.deviceModel?.takeIf { it.isNotBlank() }
        return if (manufacturer == null && model == null) {
            "omitted"
        } else {
            listOfNotNull(manufacturer, model).joinToString(" ")
        }
    }
}
