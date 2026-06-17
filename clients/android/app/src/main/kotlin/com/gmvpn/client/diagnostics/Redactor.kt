package com.gmvpn.client.diagnostics

import java.util.Locale

/**
 * Pure-Kotlin redaction primitives. Live in their own file so they
 * can be unit-tested in JVM without bringing the Android SDK along.
 *
 * Redactions are intentionally aggressive: we err on the side of
 * dropping information rather than leaking a secret in a shared log.
 *
 * - UUIDs (8-4-4-4-12 hex) → `<uuid>`
 * - VLESS / VMess / Trojan / Shadowsocks profile URIs →
 *   `<redacted-profile-uri>`
 * - `password=…`, `pwd=…`, `pw=…` query params → `<pw>` value
 * - Lines that include `Authorization:` headers → drop value
 */
object Redactor {

    private val uuidRegex = Regex(
        "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b",
    )
    private val authQueryRegex = Regex(
        "(?i)\\b(password|pwd|pw|secret|token|key|pbk|sid|spx)=([^&\\s\"#]+)",
    )
    private val authHeaderRegex = Regex(
        "(?im)(authorization|x-api-key|cookie)\\s*[:=]\\s*[^\\r\\n]*",
    )
    private val profileUriTokenRegex = Regex("(?i)\\b(vless|vmess|trojan|ss)://\\S+")
    private val httpUrlRegex = Regex("(?i)\\bhttps?://\\S+")
    private val ipv4Regex = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
    private val hostContextRegex = Regex(
        "(?i)\\b(dial tcp|lookup|server|address|host|destination)\\s+" +
            "([A-Za-z0-9_.-]+)(:\\d+)?",
    )

    /** Redact a single profile URI to a shareable label. */
    fun redactProfileUri(uri: String): String {
        val scheme = uri
            .substringBefore("://", missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        return when (scheme) {
            "vless", "vmess", "trojan", "ss" -> "$scheme://<redacted-profile-uri>"
            else -> redactGeneric(uri)
        }
    }

    private fun redactGeneric(uri: String): String {
        var safe = uuidRegex.replace(uri, "<uuid>")
        safe = authQueryRegex.replace(safe) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
        return safe
    }

    /** Redact a free-form text blob (logs, error messages, etc.). */
    fun redactText(text: String): String {
        var out = profileUriTokenRegex.replace(text) { m ->
            redactProfileUri(m.value)
        }
        out = httpUrlRegex.replace(out, "<redacted-url>")
        out = uuidRegex.replace(out, "<uuid>")
        out = authQueryRegex.replace(out) { m ->
            "${m.groupValues[1]}=<redacted>"
        }
        out = authHeaderRegex.replace(out) { m ->
            "${m.groupValues[1]}: <redacted>"
        }
        out = ipv4Regex.replace(out, "<ipv4>")
        out = hostContextRegex.replace(out) { m ->
            "${m.groupValues[1]} <redacted-host>${m.groupValues[3]}"
        }
        return out
    }
}
