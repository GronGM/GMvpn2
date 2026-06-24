package com.gmvpn.client.diagnostics

import com.gmvpn.client.profile.SubscriptionFetchDiagnostics
import com.gmvpn.client.profile.SubscriptionImportException
import com.gmvpn.client.profile.SubscriptionImportFailureCategory
import com.gmvpn.client.profile.subscriptionImportBodyShapeDiagnostics
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
    val bodyAvailable: String = "unknown",
    val bodyLengthBucket: String = "unknown",
    val lineCountBucket: String = "unknown",
    val looksBase64: String = "unknown",
    val base64DecodeLikely: String = "unknown",
    val looksUriList: String = "unknown",
    val looksJson: String = "unknown",
    val looksSip008: String = "unknown",
    val looksHtml: String = "unknown",
    val containsSupportedUriScheme: String = "unknown",
    val supportedUriSchemeCountBucket: String = "unknown",
    val requestedFormat: String = "unknown",
    val decodeFailureKind: String = "unknown",
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
            val bodyShapeDiagnostics = subscriptionImportBodyShapeDiagnostics(error)
            return fromFetchDiagnostics(
                category = category.name,
                importStage = subscriptionImportStage(error),
                failureOrigin = subscriptionImportFailureOrigin(error),
                throwableKind = subscriptionImportThrowableKind(error),
                fetchDiagnostics = fetchDiagnostics,
                bodyShapeDiagnostics = bodyShapeDiagnostics,
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
            bodyShapeDiagnostics: com.gmvpn.client.profile.SubscriptionBodyShapeDiagnostics? = null,
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
                bodyAvailable = bodyShapeDiagnostics?.bodyAvailable?.safeValue
                    ?: previousAttempt?.bodyAvailable
                    ?: "unknown",
                bodyLengthBucket = bodyShapeDiagnostics?.bodyLengthBucket?.safeValue
                    ?: fetchDiagnostics?.bodyLengthBucket?.safeValue
                    ?: previousAttempt?.bodyLengthBucket
                    ?: "unknown",
                lineCountBucket = bodyShapeDiagnostics?.lineCountBucket?.safeValue
                    ?: previousAttempt?.lineCountBucket
                    ?: "unknown",
                looksBase64 = bodyShapeDiagnostics?.looksBase64?.safeValue
                    ?: previousAttempt?.looksBase64
                    ?: "unknown",
                base64DecodeLikely = bodyShapeDiagnostics?.base64DecodeLikely?.safeValue
                    ?: previousAttempt?.base64DecodeLikely
                    ?: "unknown",
                looksUriList = bodyShapeDiagnostics?.looksUriList?.safeValue
                    ?: previousAttempt?.looksUriList
                    ?: "unknown",
                looksJson = bodyShapeDiagnostics?.looksJson?.safeValue
                    ?: previousAttempt?.looksJson
                    ?: "unknown",
                looksSip008 = bodyShapeDiagnostics?.looksSip008?.safeValue
                    ?: previousAttempt?.looksSip008
                    ?: "unknown",
                looksHtml = bodyShapeDiagnostics?.looksHtml?.safeValue
                    ?: previousAttempt?.looksHtml
                    ?: "unknown",
                containsSupportedUriScheme = bodyShapeDiagnostics?.containsSupportedUriScheme?.safeValue
                    ?: previousAttempt?.containsSupportedUriScheme
                    ?: "unknown",
                supportedUriSchemeCountBucket =
                    bodyShapeDiagnostics?.supportedUriSchemeCountBucket?.safeValue
                        ?: previousAttempt?.supportedUriSchemeCountBucket
                        ?: "unknown",
                requestedFormat = bodyShapeDiagnostics?.requestedFormat?.safeValue
                    ?: previousAttempt?.requestedFormat
                    ?: "unknown",
                decodeFailureKind = bodyShapeDiagnostics?.decodeFailureKind?.safeValue
                    ?: previousAttempt?.decodeFailureKind
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
            "bodyAvailable=$bodyAvailable",
            "bodyLengthBucket=$bodyLengthBucket",
            "lineCountBucket=$lineCountBucket",
            "looksBase64=$looksBase64",
            "base64DecodeLikely=$base64DecodeLikely",
            "looksUriList=$looksUriList",
            "looksJson=$looksJson",
            "looksSip008=$looksSip008",
            "looksHtml=$looksHtml",
            "containsSupportedUriScheme=$containsSupportedUriScheme",
            "supportedUriSchemeCountBucket=$supportedUriSchemeCountBucket",
            "requestedFormat=$requestedFormat",
            "decodeFailureKind=$decodeFailureKind",
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
        appendLine("import_body_available: ${attempt.bodyAvailable}")
        appendLine("import_body_length_bucket: ${attempt.bodyLengthBucket}")
        appendLine("import_line_count_bucket: ${attempt.lineCountBucket}")
        appendLine("import_looks_base64: ${attempt.looksBase64}")
        appendLine("import_base64_decode_likely: ${attempt.base64DecodeLikely}")
        appendLine("import_looks_uri_list: ${attempt.looksUriList}")
        appendLine("import_looks_json: ${attempt.looksJson}")
        appendLine("import_looks_sip008: ${attempt.looksSip008}")
        appendLine("import_looks_html: ${attempt.looksHtml}")
        appendLine("import_contains_supported_uri_scheme: ${attempt.containsSupportedUriScheme}")
        appendLine("import_supported_uri_scheme_count_bucket: ${attempt.supportedUriSchemeCountBucket}")
        appendLine("import_requested_format: ${attempt.requestedFormat}")
        appendLine("import_decode_failure_kind: ${attempt.decodeFailureKind}")
        appendLine("import_profiles_count: ${attempt.profilesImported ?: "unknown"}")
    }
}

private fun Throwable.importFailureCategory(): SubscriptionImportFailureCategory =
    com.gmvpn.client.profile.subscriptionImportFailureCategory(this)
