package com.gmvpn.client.diagnostics

import com.gmvpn.client.profile.SubscriptionFetchDiagnostics
import com.gmvpn.client.profile.SubscriptionImportFailureCategory
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
    val lastImportAttempt: RedactedImportDiagnostics? = null,
    val timestampUtc: String = RedactedDiagnosticsReport.nowUtc(),
)

data class RedactedImportDiagnostics(
    val category: String,
    val urlScheme: String = "unknown",
    val hasQuery: Boolean = false,
    val hasFragment: Boolean = false,
    val inputLengthBucket: String = "unknown",
    val httpStatusClass: String = "unknown",
    val cleartextBlockedLikely: String = "unknown",
    val tlsFailureLikely: String = "unknown",
    val dnsFailureLikely: String = "unknown",
    val timeoutLikely: String = "unknown",
    val redirectObserved: String = "unknown",
    val bodyLengthBucket: String = "unknown",
    val profilesImported: Int? = null,
) {
    companion object {
        fun inFlight(inputDiagnostics: SubscriptionFetchDiagnostics): RedactedImportDiagnostics =
            fromFetchDiagnostics(
                category = "InFlight",
                fetchDiagnostics = inputDiagnostics,
            )

        fun success(profilesImported: Int): RedactedImportDiagnostics =
            RedactedImportDiagnostics(
                category = "Success",
                profilesImported = profilesImported.coerceAtLeast(0),
            )

        fun failure(
            category: SubscriptionImportFailureCategory,
            fetchDiagnostics: SubscriptionFetchDiagnostics? = null,
        ): RedactedImportDiagnostics =
            fromFetchDiagnostics(
                category = category.name,
                fetchDiagnostics = fetchDiagnostics,
            )

        private fun fromFetchDiagnostics(
            category: String,
            fetchDiagnostics: SubscriptionFetchDiagnostics?,
        ): RedactedImportDiagnostics =
            RedactedImportDiagnostics(
                category = category,
                urlScheme = fetchDiagnostics?.urlScheme?.safeValue ?: "unknown",
                hasQuery = fetchDiagnostics?.hasQuery ?: false,
                hasFragment = fetchDiagnostics?.hasFragment ?: false,
                inputLengthBucket = fetchDiagnostics?.inputLengthBucket?.safeValue ?: "unknown",
                httpStatusClass = fetchDiagnostics?.httpStatusClass?.safeValue ?: "unknown",
                cleartextBlockedLikely = fetchDiagnostics?.cleartextBlockedLikely?.safeValue
                    ?: "unknown",
                tlsFailureLikely = fetchDiagnostics?.tlsFailureLikely?.safeValue ?: "unknown",
                dnsFailureLikely = fetchDiagnostics?.dnsFailureLikely?.safeValue ?: "unknown",
                timeoutLikely = fetchDiagnostics?.timeoutLikely?.safeValue ?: "unknown",
                redirectObserved = fetchDiagnostics?.redirectObserved?.safeValue ?: "unknown",
                bodyLengthBucket = fetchDiagnostics?.bodyLengthBucket?.safeValue ?: "unknown",
            )
    }
}

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
            appendLastImportAttempt(input.lastImportAttempt)
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

    private fun StringBuilder.appendLastImportAttempt(
        attempt: RedactedImportDiagnostics?,
    ) {
        if (attempt == null) return
        appendLine("Last import attempt:")
        appendLine("import_category: ${attempt.category}")
        appendLine("import_url_scheme: ${attempt.urlScheme}")
        appendLine("import_has_query: ${attempt.hasQuery}")
        appendLine("import_has_fragment: ${attempt.hasFragment}")
        appendLine("import_input_length_bucket: ${attempt.inputLengthBucket}")
        appendLine("import_http_status_class: ${attempt.httpStatusClass}")
        appendLine("import_cleartext_blocked_likely: ${attempt.cleartextBlockedLikely}")
        appendLine("import_tls_failure_likely: ${attempt.tlsFailureLikely}")
        appendLine("import_dns_failure_likely: ${attempt.dnsFailureLikely}")
        appendLine("import_timeout_likely: ${attempt.timeoutLikely}")
        appendLine("import_redirect_observed: ${attempt.redirectObserved}")
        appendLine("import_body_length_bucket: ${attempt.bodyLengthBucket}")
        appendLine("import_profiles_count: ${attempt.profilesImported ?: "unknown"}")
    }
}
