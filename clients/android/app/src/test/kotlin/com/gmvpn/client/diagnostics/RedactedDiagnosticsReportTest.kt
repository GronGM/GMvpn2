package com.gmvpn.client.diagnostics

import com.gmvpn.client.tunnel.TunnelStatus
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
}
