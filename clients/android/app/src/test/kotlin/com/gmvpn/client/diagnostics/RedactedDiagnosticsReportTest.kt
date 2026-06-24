package com.gmvpn.client.diagnostics

import com.gmvpn.client.profile.SubscriptionFetchException
import com.gmvpn.client.profile.SubscriptionBodyLengthBucket
import com.gmvpn.client.profile.SubscriptionDiagnosticTriState
import com.gmvpn.client.profile.SubscriptionFetchDiagnostics
import com.gmvpn.client.profile.SubscriptionHttpStatusClass
import com.gmvpn.client.profile.SubscriptionImportException
import com.gmvpn.client.profile.SubscriptionImportFailureCategory
import com.gmvpn.client.tunnel.TunnelStatus
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactedDiagnosticsReportTest {

    @Test
    fun `report omits profile uri endpoint uuid password and raw logs`() {
        val report = RedactedDiagnosticsReport.render(
            RedactedDiagnosticsInput(
                appVersion = "1.0.0-rc.5",
                versionCode = 1000005,
                packageName = "com.gmvpn.client",
                androidRelease = "12",
                androidSdk = 31,
                deviceManufacturer = null,
                deviceModel = null,
                status = TunnelStatus.Error,
                lastErrorCategory = RedactedDiagnosticsReport.categorizeLastError(
                    "invalid trojan://sample-secret@1.2.3.4:443?security=tls",
                ),
                selectedProtocolType = "VLESS",
                profileCount = 2,
                timestampUtc = "2026-06-17T00:00:00Z",
            ),
        )

        assertTrue(report.contains("last_error_category: invalid_profile"))
        assertTrue(report.contains("selected_protocol_type: VLESS"))
        assertTrue(report.contains("device: omitted"))
        assertFalse(report.contains("vless://"))
        assertFalse(report.contains("1.2.3.4"))
        assertFalse(report.contains("00000000-0000-0000-0000-000000000000"))
        assertFalse(report.contains("sample-secret"))
        assertFalse(report.contains("logcat"))
    }

    @Test
    fun `device identity is included only when provided`() {
        val report = RedactedDiagnosticsReport.render(
            RedactedDiagnosticsInput(
                appVersion = "1.0.0-rc.5",
                versionCode = 1000005,
                packageName = "com.gmvpn.client",
                androidRelease = "12",
                androidSdk = 31,
                deviceManufacturer = "TECNO",
                deviceModel = "LG8n",
                status = TunnelStatus.Idle,
                lastErrorCategory = "none",
                selectedProtocolType = null,
                profileCount = 0,
                timestampUtc = "2026-06-17T00:00:00Z",
            ),
        )

        assertTrue(report.contains("device: TECNO LG8n"))
    }

    @Test
    fun `failed subscription fetch is reported with safe import diagnostics only`() {
        val rawUrl = "https://subscription.example.invalid/private/path?marker=raw-sample#frag"
        val rawBody = "vless://00000000-0000-0000-0000-000000000000@host.example.invalid:443"
        val fetchDiagnostics = SubscriptionFetchDiagnostics.fromInput(rawUrl).copy(
            httpStatusClass = SubscriptionHttpStatusClass.Status4xx,
            cleartextBlockedLikely = SubscriptionDiagnosticTriState.No,
            tlsFailureLikely = SubscriptionDiagnosticTriState.Yes,
            dnsFailureLikely = SubscriptionDiagnosticTriState.No,
            timeoutLikely = SubscriptionDiagnosticTriState.No,
            redirectObserved = SubscriptionDiagnosticTriState.Yes,
            bodyLengthBucket = SubscriptionBodyLengthBucket.Small,
        )

        val report = RedactedDiagnosticsReport.render(
            baseInput(
                lastImportAttempt = RedactedImportDiagnostics.failure(
                    category = SubscriptionImportFailureCategory.FetchFailed,
                    fetchDiagnostics = fetchDiagnostics,
                ),
            ),
        )

        assertTrue(report.contains("Last import attempt:"))
        assertTrue(report.contains("import_category: FetchFailed"))
        assertTrue(report.contains("import_stage: fetch_failed"))
        assertTrue(report.contains("import_failure_origin: fetch"))
        assertTrue(report.contains("import_has_typed_cause: true"))
        assertTrue(report.contains("import_has_fetch_diagnostics: true"))
        assertTrue(report.contains("import_url_scheme: https"))
        assertTrue(report.contains("import_http_status_class: 4xx"))
        assertTrue(report.contains("import_cleartext_blocked_likely: no"))
        assertTrue(report.contains("import_tls_failure_likely: yes"))
        assertTrue(report.contains("import_dns_failure_likely: no"))
        assertTrue(report.contains("import_timeout_likely: no"))
        assertTrue(report.contains("import_redirect_observed: yes"))
        assertTrue(report.contains("import_body_length_bucket: small"))
        assertTrue(report.contains("import_has_query: true"))
        assertTrue(report.contains("import_has_fragment: true"))
        assertFalse(report.contains(rawUrl))
        assertFalse(report.contains("subscription.example.invalid"))
        assertFalse(report.contains("/private/path"))
        assertFalse(report.contains("marker=raw-sample"))
        assertFalse(report.contains("raw-sample"))
        assertFalse(report.contains(rawBody))
        assertFalse(report.contains("00000000-0000-0000-0000-000000000000"))
        assertFalse(report.contains("host.example.invalid"))
    }

    @Test
    fun `successful import report replaces previous fetch failure safely`() {
        val report = RedactedDiagnosticsReport.render(
            baseInput(
                lastImportAttempt = RedactedImportDiagnostics.success(
                    profilesImported = 4,
                ),
            ),
        )

        assertTrue(report.contains("import_category: Success"))
        assertTrue(report.contains("import_stage: success"))
        assertTrue(report.contains("import_profiles_count: 4"))
        assertFalse(report.contains("import_category: FetchFailed"))
        assertFalse(report.contains("ui_failure_catch"))
        assertFalse(report.contains("import_url_scheme: https"))
        assertFalse(report.contains("import_http_status_class: 4xx"))
    }

    @Test
    fun `save failure is distinguishable from fetch failure`() {
        val fetchReport = RedactedDiagnosticsReport.render(
            baseInput(
                lastImportAttempt = RedactedImportDiagnostics.failure(
                    category = SubscriptionImportFailureCategory.FetchFailed,
                    fetchDiagnostics = SubscriptionFetchDiagnostics.fromInput("https://example.invalid/sub"),
                ),
            ),
        )
        val saveReport = RedactedDiagnosticsReport.render(
            baseInput(
                lastImportAttempt = RedactedImportDiagnostics.failure(
                    category = SubscriptionImportFailureCategory.SaveFailed,
                ),
            ),
        )

        assertTrue(fetchReport.contains("import_category: FetchFailed"))
        assertTrue(fetchReport.contains("import_stage: fetch_failed"))
        assertTrue(saveReport.contains("import_category: SaveFailed"))
        assertTrue(saveReport.contains("import_stage: save_failed"))
        assertFalse(saveReport.contains("import_category: FetchFailed"))
    }

    @Test
    fun `unknown throwable preserves previous safe input diagnostics`() {
        val previous = RedactedImportDiagnostics.inFlight(
            SubscriptionFetchDiagnostics.fromInput(SYNTHETIC_SUBSCRIPTION_URL),
        )

        val diagnostic = RedactedImportDiagnostics.failureFromThrowable(
            error = RuntimeException("synthetic generic failure with raw URL omitted"),
            previousAttempt = previous,
        )
        val report = RedactedDiagnosticsReport.render(
            baseInput(lastImportAttempt = diagnostic),
        )

        assertTrue(report.contains("import_category: Unknown"))
        assertTrue(report.contains("import_stage: ui_failure_catch"))
        assertTrue(report.contains("import_failure_origin: ui"))
        assertTrue(report.contains("import_throwable_kind: unknown_exception"))
        assertTrue(report.contains("import_has_typed_cause: false"))
        assertTrue(report.contains("import_has_fetch_diagnostics: false"))
        assertTrue(report.contains("import_url_scheme: https"))
        assertTrue(report.contains("import_has_query: true"))
        assertTrue(report.contains("import_input_length_bucket: short"))
        assertNoSyntheticSubscriptionLeak(report)
    }

    @Test
    fun `illegal argument before fetch maps to controlled safe diagnostic`() {
        val diagnostic = RedactedImportDiagnostics.failureFromThrowable(
            error = IllegalArgumentException("synthetic invalid input"),
            previousAttempt = RedactedImportDiagnostics.inFlight(
                SubscriptionFetchDiagnostics.fromInput(SYNTHETIC_SUBSCRIPTION_URL),
            ),
        )
        val report = RedactedDiagnosticsReport.render(
            baseInput(lastImportAttempt = diagnostic),
        )

        assertTrue(report.contains("import_category: Unknown"))
        assertTrue(report.contains("import_stage: ui_failure_catch"))
        assertTrue(report.contains("import_throwable_kind: illegal_argument_exception"))
        assertNoSyntheticSubscriptionLeak(report)
    }

    @Test
    fun `subscription fetch exception maps to fetch stage with diagnostics`() {
        val diagnostics = SubscriptionFetchDiagnostics.fromInput(SYNTHETIC_SUBSCRIPTION_URL)
            .copy(httpStatusClass = SubscriptionHttpStatusClass.Status5xx)
        val diagnostic = RedactedImportDiagnostics.failureFromThrowable(
            error = SubscriptionFetchException(
                message = "synthetic fetch failure",
                diagnostics = diagnostics,
            ),
            previousAttempt = null,
        )
        val report = RedactedDiagnosticsReport.render(
            baseInput(lastImportAttempt = diagnostic),
        )

        assertTrue(report.contains("import_category: FetchFailed"))
        assertTrue(report.contains("import_stage: fetch_failed"))
        assertTrue(report.contains("import_failure_origin: fetch"))
        assertTrue(report.contains("import_throwable_kind: subscription_fetch_exception"))
        assertTrue(report.contains("import_has_typed_cause: true"))
        assertTrue(report.contains("import_has_fetch_diagnostics: true"))
        assertTrue(report.contains("import_http_status_class: 5xx"))
        assertNoSyntheticSubscriptionLeak(report)
    }

    @Test
    fun `wrapped network exceptions map to controlled throwable buckets`() {
        assertThrowableKind(IOException("synthetic"), "io_exception")
        assertThrowableKind(SSLException("synthetic"), "ssl_exception")
        assertThrowableKind(UnknownHostException("synthetic"), "unknown_host_exception")
        assertThrowableKind(SocketTimeoutException("synthetic"), "timeout_exception")
    }

    private fun assertThrowableKind(
        cause: Throwable,
        expectedKind: String,
    ) {
        val diagnostic = RedactedImportDiagnostics.failureFromThrowable(
            error = SubscriptionImportException(
                category = SubscriptionImportFailureCategory.FetchFailed,
                fetchDiagnostics = SubscriptionFetchDiagnostics.fromInput(
                    SYNTHETIC_SUBSCRIPTION_URL,
                ),
                cause = cause,
            ),
            previousAttempt = null,
        )

        val report = RedactedDiagnosticsReport.render(
            baseInput(lastImportAttempt = diagnostic),
        )

        assertTrue(report.contains("import_throwable_kind: $expectedKind"))
        assertTrue(report.contains("import_category: FetchFailed"))
        assertNoSyntheticSubscriptionLeak(report)
    }

    private fun assertNoSyntheticSubscriptionLeak(report: String) {
        assertFalse(report.contains(SYNTHETIC_SUBSCRIPTION_URL))
        assertFalse(report.contains("example.invalid"))
        assertFalse(report.contains("/subscription"))
        assertFalse(report.contains("token=redacted-test-token"))
        assertFalse(report.contains("redacted-test-token"))
    }

    private fun baseInput(
        lastImportAttempt: RedactedImportDiagnostics?,
    ): RedactedDiagnosticsInput =
        RedactedDiagnosticsInput(
            appVersion = "1.1.0-rc.1",
            versionCode = 1010001,
            packageName = "com.gmvpn.client",
            androidRelease = "12",
            androidSdk = 31,
            deviceManufacturer = null,
            deviceModel = null,
            status = TunnelStatus.Idle,
            lastErrorCategory = "none",
            selectedProtocolType = null,
            profileCount = 0,
            lastImportAttempt = lastImportAttempt,
            timestampUtc = "2026-06-17T00:00:00Z",
        )

    private companion object {
        const val SYNTHETIC_SUBSCRIPTION_URL =
            "https://example.invalid/subscription?token=redacted-test-token"
    }
}
