package com.gmvpn.client.diagnostics

import android.content.Context
import android.os.Build
import com.gmvpn.client.profile.LatencyState
import com.gmvpn.client.tunnel.TunnelStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import uniffi.gmvpn_ffi.coreVersion

/**
 * Build a redacted diagnostics bundle the user can share. The output
 * is a single text blob with sections:
 *
 *   ---- meta            app version, OS, device, engine versions
 *   ---- tunnel          last known status + last error
 *   ---- profiles        each library entry, redacted via [Redactor]
 *   ---- latencies       last measured RTT per profile
 *   ---- logcat (tail)   last N lines of self-process logcat,
 *                        passed through [Redactor.redactText]
 */
object DiagnosticsCollector {

    suspend fun collect(
        context: Context,
        appVersion: String,
        xrayVersion: String,
        status: TunnelStatus,
        lastError: String?,
        library: List<String>,
        latencies: Map<Int, LatencyState>,
        logcatLines: Int = 500,
    ): String {
        val sb = StringBuilder()
        appendHeader(sb, appVersion, xrayVersion)
        appendTunnel(sb, status, lastError)
        appendProfiles(sb, library)
        appendLatencies(sb, library.size, latencies)
        appendLogcat(sb, logcatLines)
        return sb.toString()
    }

    private fun appendHeader(sb: StringBuilder, appVersion: String, xrayVersion: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        sb.appendLine("---- meta")
        sb.appendLine("timestamp_utc: $ts")
        sb.appendLine("app_version:   $appVersion")
        sb.appendLine("core_version:  ${coreVersion()}")
        sb.appendLine("xray_version:  $xrayVersion")
        sb.appendLine("android_sdk:   ${Build.VERSION.SDK_INT}")
        sb.appendLine("device:        ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("abi:           ${Build.SUPPORTED_ABIS.joinToString(",")}")
        sb.appendLine()
    }

    private fun appendTunnel(sb: StringBuilder, status: TunnelStatus, lastError: String?) {
        sb.appendLine("---- tunnel")
        sb.appendLine("status:        ${status.name}")
        if (!lastError.isNullOrBlank()) {
            sb.appendLine("last_error:    ${Redactor.redactText(lastError)}")
        }
        sb.appendLine()
    }

    private fun appendProfiles(sb: StringBuilder, library: List<String>) {
        sb.appendLine("---- profiles (${library.size})")
        if (library.isEmpty()) {
            sb.appendLine("(none)")
        } else {
            library.forEachIndexed { idx, uri ->
                sb.appendLine("[$idx] ${Redactor.redactProfileUri(uri)}")
            }
        }
        sb.appendLine()
    }

    private fun appendLatencies(
        sb: StringBuilder,
        profileCount: Int,
        latencies: Map<Int, LatencyState>,
    ) {
        sb.appendLine("---- latencies")
        if (latencies.isEmpty() || profileCount == 0) {
            sb.appendLine("(no probes recorded)")
        } else {
            (0 until profileCount).forEach { idx ->
                val s = latencies[idx]
                val text = when (s) {
                    null, LatencyState.Idle -> "—"
                    LatencyState.InFlight -> "in flight"
                    is LatencyState.Result -> s.ms?.let { "$it ms" } ?: "unreachable"
                }
                sb.appendLine("[$idx] $text")
            }
        }
        sb.appendLine()
    }

    private suspend fun appendLogcat(sb: StringBuilder, lines: Int) {
        sb.appendLine("---- logcat (last $lines lines, redacted)")
        val raw = LogcatTail.read(lines)
        sb.appendLine(Redactor.redactText(raw))
    }
}
