package com.gmvpn.client.diagnostics

import com.gmvpn.client.profile.SubscriptionFetchDiagnostics
import com.gmvpn.client.profile.SubscriptionImportException
import com.gmvpn.client.profile.SubscriptionImportFailureCategory
import com.gmvpn.client.profile.subscriptionImportFetchDiagnostics
import com.gmvpn.client.profile.subscriptionImportFailureOrigin
import com.gmvpn.client.profile.subscriptionImportHasTypedCause
import com.gmvpn.client.profile.subscriptionImportStage
import com.gmvpn.client.profile.subscriptionImportThrowableKind
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
    val importStage: String = "unknown",
    val failureOrigin: String = "unknown",
    val throwableKind: String = "unknown_exception",
    val hasTypedCause: Boolean = false,
    val hasFetchDiagnostics: Boolean = false,
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
                importStage = "fetch_start",
                fetchDiagnostics = inputDiagnostics,
                hasFetchDiagnostics = true,
            )

        fun decodeSuccess(
            profilesImported: Int,
            previousAttempt: RedactedImportDiagnostics? = null,
        ): RedactedImportDiagnostics =
            fromFetchDiagnostics(
                category = "DecodeSuccess",
                importStage = "decode_success",
                previousAttempt = previousAttempt,
            ).copy(
                profilesImported = profilesImported.coerceAtLeast(0),
            )

        fun saveStart(
            previousAttempt: RedactedImportDiagnostics? = null,
        ): RedactedImportDiagnostics =
            fromFetchDiagnostics(
                category = "InFlight",
                importStage = "save_start",
                failureOrigin = "save",
                previousAttempt = previousAttempt,
            )

        fun success(
            profilesImported: Int,
            previousAttempt: RedactedImportDiagnostics? = null,
        ): RedactedImportDiagnostics =
            fromFetchDiagnostics(
                category = "Success",
                importStage = "success",
                previousAttempt = previousAttempt,
            ).copy(
                profilesImported = profilesImported.coerceAtLeast(0),
            )

        fun failure(
            category: SubscriptionImportFailureCategory,
            fetchDiagnostics: SubscriptionFetchDiagnostics? = null,
            previousAttempt: RedactedImportDiagnostics? = null,
        ): RedactedImportDiagnostics =
            failureFromThrowable(
                error = SubscriptionImportException(
                    category = category,
                    fetchDiagnostics = fetchDiagnostics,
                ),
                previousAttempt = previousAttempt,
            )

        fun failureFromThrowable(
            error: Throwable,
            previousAttempt: RedactedImportDiagnostics?,
        ): RedactedImportDiagnostics {
            val category = error.importFailureCategory()
            val fetchDiagnostics = subscriptionImportFetchDiagnostics(error)
            return fromFetchDiagnostics(
                category = category.name,
                importStage = subscriptionImportStage(error),
                failureOrigin = subscriptionImportFailureOrigin(error),
                throwableKind = subscriptionImportThrowableKind(error),
                fetchDiagnostics = fetchDiagnostics,
                previousAttempt = previousAttempt,
                hasTypedCause = subscriptionImportHasTypedCause(error),
                hasFetchDiagnostics = fetchDiagnostics != null,
            )
        }

        private fun fromFetchDiagnostics(
            category: String,
            importStage: String = "unknown",
            failureOrigin: String = "unknown",
            throwableKind: String = "unknown_exception",
            fetchDiagnostics: SubscriptionFetchDiagnostics? = null,
            previousAttempt: RedactedImportDiagnostics? = null,
            hasTypedCause: Boolean = false,
            hasFetchDiagnostics: Boolean = false,
        ): RedactedImportDiagnostics =
            RedactedImportDiagnostics(
                category = category,
                importStage = importStage,
                failureOrigin = failureOrigin,
                throwableKind = throwableKind,
                hasTypedCause = hasTypedCause,
                hasFetchDiagnostics = hasFetchDiagnostics,
                urlScheme = fetchDiagnostics?.urlScheme?.safeValue
                    ?: previousAttempt?.urlScheme
                    ?: "unknown",
                hasQuery = fetchDiagnostics?.hasQuery ?: previousAttempt?.hasQuery ?: false,
                hasFragment = fetchDiagnostics?.hasFragment
                    ?: previousAttempt?.hasFragment
                    ?: false,
                inputLengthBucket = fetchDiagnostics?.inputLengthBucket?.safeValue
                    ?: previousAttempt?.inputLengthBucket
                    ?: "unknown",
                httpStatusClass = fetchDiagnostics?.httpStatusClass?.safeValue
                    ?: previousAttempt?.httpStatusClass
                    ?: "unknown",
                cleartextBlockedLikely = fetchDiagnostics?.cleartextBlockedLikely?.safeValue
                    ?: previousAttempt?.cleartextBlockedLikely
                    ?: "unknown",
                tlsFailureLikely = fetchDiagnostics?.tlsFailureLikely?.safeValue
                    ?: previousAttempt?.tlsFailureLikely
                    ?: "unknown",
                dnsFailureLikely = fetchDiagnostics?.dnsFailureLikely?.safeValue
                    ?: previousAttempt?.dnsFailureLikely
                    ?: "unknown",
                timeoutLikely = fetchDiagnostics?.timeoutLikely?.safeValue
                    ?: previousAttempt?.timeoutLikely
                    ?: "unknown",
                redirectObserved = fetchDiagnostics?.redirectObserved?.safeValue
                    ?: previousAttempt?.redirectObserved
                    ?: "unknown",
                bodyLengthBucket = fetchDiagnostics?.bodyLengthBucket?.safeValue
                    ?: previousAttempt?.bodyLengthBucket
                    ?: "unknown",
            )
    }

    fun toSafeLogString(safeMessageKey: String): String =
        listOf(
            "category=$category",
            "importStage=$importStage",
            "failureOrigin=$failureOrigin",
            "throwableKind=$throwableKind",
            "hasTypedCause=$hasTypedCause",
            "hasFetchDiagnostics=$hasFetchDiagnostics",
            "urlScheme=$urlScheme",
            "hasQuery=$hasQuery",
            "hasFragment=$hasFragment",
            "inputLengthBucket=$inputLengthBucket",
            "httpStatusClass=$httpStatusClass",
            "cleartextBlockedLikely=$cleartextBlockedLikely",
            "tlsFailureLikely=$tlsFailureLikely",
            "dnsFailureLikely=$dnsFailureLikely",
            "timeoutLikely=$timeoutLikely",
            "redirectObserved=$redirectObserved",
            "bodyLengthBucket=$bodyLengthBucket",
            "safeMessageKey=$safeMessageKey",
        ).joinToString(separator = " ")
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
        appendLine("import_stage: ${attempt.importStage}")
        appendLine("import_failure_origin: ${attempt.failureOrigin}")
        appendLine("import_throwable_kind: ${attempt.throwableKind}")
        appendLine("import_has_typed_cause: ${attempt.hasTypedCause}")
        appendLine("import_has_fetch_diagnostics: ${attempt.hasFetchDiagnostics}")
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

private fun Throwable.importFailureCategory(): SubscriptionImportFailureCategory =
    com.gmvpn.client.profile.subscriptionImportFailureCategory(this)
