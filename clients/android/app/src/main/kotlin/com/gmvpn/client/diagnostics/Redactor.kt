package com.gmvpn.client.diagnostics

/**
 * Pure-Kotlin redaction primitives. Live in their own file so they
 * can be unit-tested in JVM without bringing the Android SDK along.
 *
 * Redactions are intentionally aggressive: we err on the side of
 * dropping information rather than leaking a secret in a shared log.
 *
 * - UUIDs (8-4-4-4-12 hex) → `<uuid>`
 * - VLESS / VMess auth fragments inside URIs (the userinfo before
 *   `@`, when it parses as a UUID) → `<uuid>`
 * - Trojan passwords inside URIs (the userinfo before `@`, when it
 *   does NOT parse as a UUID) → `<pw>`
 * - Shadowsocks userinfo (base64 before `@`) → `<ss-userinfo>`
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
    private val profileUriTokenRegex = Regex("(vless|vmess|trojan|ss)://\\S+")

    /** Redact a single profile URI to a shareable label. */
    fun redactProfileUri(uri: String): String {
        val scheme = uri.substringBefore("://", missingDelimiterValue = "")
        return when (scheme) {
            "vless" -> redactVlessLike(uri)
            "vmess" -> "vmess://<base64-redacted>"
            "trojan" -> redactTrojan(uri)
            "ss" -> redactShadowsocks(uri)
            else -> redactGeneric(uri)
        }
    }

    private fun redactVlessLike(uri: String): String {
        // vless://<uuid>@<host>:<port>?params#remark
        val withoutScheme = uri.removePrefix("vless://")
        val (userinfo, rest) = withoutScheme.splitAtFirstOrNull('@') ?: return uri
        if (!uuidRegex.matches(userinfo)) return uri
        val safeRest = redactQueryString(rest)
        return "vless://<uuid>@$safeRest"
    }

    private fun redactTrojan(uri: String): String {
        val withoutScheme = uri.removePrefix("trojan://")
        val (_, rest) = withoutScheme.splitAtFirstOrNull('@') ?: return uri
        val safeRest = redactQueryString(rest)
        return "trojan://<pw>@$safeRest"
    }

    private fun redactShadowsocks(uri: String): String {
        val withoutScheme = uri.removePrefix("ss://")
        val (_, rest) = withoutScheme.splitAtFirstOrNull('@')
            ?: return "ss://<ss-userinfo>"
        return "ss://<ss-userinfo>@${redactQueryString(rest)}"
    }

    private fun redactGeneric(uri: String): String {
        var safe = uuidRegex.replace(uri, "<uuid>")
        safe = authQueryRegex.replace(safe) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
        return safe
    }

    private fun redactQueryString(rest: String): String {
        var out = rest
        // Reality public key and short id are not strictly secret but
        // collapse them anyway — they identify the server precisely
        // and leak the choice of upstream to anyone reading the log.
        out = out.replace(Regex("(?i)([?&])(pbk|sid|spx)=[^&#]*")) { m ->
            "${m.groupValues[1]}${m.groupValues[2]}=<redacted>"
        }
        out = authQueryRegex.replace(out) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
        return out
    }

    /** Redact a free-form text blob (logs, error messages, etc.). */
    fun redactText(text: String): String {
        var out = profileUriTokenRegex.replace(text) { m ->
            redactProfileUri(m.value)
        }
        out = uuidRegex.replace(out, "<uuid>")
        out = authQueryRegex.replace(out) { m ->
            "${m.groupValues[1]}=<redacted>"
        }
        out = authHeaderRegex.replace(out) { m ->
            "${m.groupValues[1]}: <redacted>"
        }
        return out
    }

    private fun String.splitAtFirstOrNull(c: Char): Pair<String, String>? {
        val idx = indexOf(c)
        if (idx < 0) return null
        return substring(0, idx) to substring(idx + 1)
    }
}
