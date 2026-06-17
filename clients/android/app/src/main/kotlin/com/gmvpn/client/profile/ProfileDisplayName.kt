package com.gmvpn.client.profile

import java.nio.charset.StandardCharsets
import java.util.Base64

data class ProfileSummary(
    val displayName: String,
    val secondaryLabel: String,
)

fun profileSummary(uri: String, fallbackIndex: Int): ProfileSummary {
    val scheme = uri.substringBefore("://", missingDelimiterValue = "")
        .lowercase()
    val protocol = protocolLabel(scheme)
    val secondary = protocol ?: "Профиль"

    if (protocol != null) {
        val fragmentName = decodedFragment(uri)?.toSafeDisplayName()
        if (fragmentName != null) {
            return ProfileSummary(fragmentName, secondary)
        }
    }

    if (scheme == "vmess") {
        val vmessName = parseVmessPs(uri)?.toSafeDisplayName()
        if (vmessName != null) {
            return ProfileSummary(vmessName, secondary)
        }
    }

    return ProfileSummary(
        displayName = fallbackProfileName(protocol, fallbackIndex),
        secondaryLabel = secondary,
    )
}

private fun protocolLabel(scheme: String): String? = when (scheme) {
    "vless" -> "VLESS"
    "vmess" -> "VMess"
    "trojan" -> "Trojan"
    "ss" -> "Shadowsocks"
    else -> null
}

private fun fallbackProfileName(protocol: String?, fallbackIndex: Int): String =
    when (protocol) {
        "VLESS" -> "VLESS профиль"
        "VMess" -> "VMess профиль"
        "Trojan" -> "Trojan профиль"
        "Shadowsocks" -> "Shadowsocks профиль"
        else -> "Профиль ${fallbackIndex.coerceAtLeast(1)}"
    }

private fun decodedFragment(uri: String): String? {
    val fragment = uri.substringAfter('#', missingDelimiterValue = "")
    if (fragment.isBlank()) return null
    return percentDecode(fragment)
}

private fun String.toSafeDisplayName(): String? {
    val cleaned = replace(Regex("\\p{Cntrl}+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isBlank()) return null
    if (isUnsafeProfileLabel(cleaned)) return null

    val maxChars = 72
    return if (cleaned.length <= maxChars) {
        cleaned
    } else {
        cleaned.take(maxChars - 1).trimEnd() + "…"
    }
}

private fun isUnsafeProfileLabel(value: String): Boolean {
    val cleaned = value.trim()
    val lowered = cleaned.lowercase()

    return cleaned.isBlank() ||
        cleaned.contains("://") ||
        cleaned.contains('@') ||
        uuidRegex.matches(cleaned) ||
        ipv4Regex.matches(cleaned) ||
        bracketedIpv6Regex.matches(cleaned) ||
        looksLikeIpv6(cleaned) ||
        hostnameRegex.matches(cleaned) ||
        hostWithPortRegex.matches(cleaned) ||
        secretAssignmentRegex.containsMatchIn(lowered) ||
        cleaned.looksLikeRawBase64()
}

private fun String.looksLikeRawBase64(): Boolean {
    if (length < 24) return false
    if (any { it.isWhitespace() }) return false
    return rawBase64Regex.matches(this)
}

private fun looksLikeIpv6(value: String): Boolean {
    val unbracketed = value.substringBeforeLast(':')
        .takeIf { value.count { ch -> ch == ':' } > 2 && value.substringAfterLast(':').all(Char::isDigit) }
        ?: value
    return unbracketed.count { it == ':' } >= 2 &&
        unbracketed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' || it == '.' || it == '%' }
}

private fun parseVmessPs(uri: String): String? {
    val body = bodyAfterScheme(uri)
        .substringBefore('#')
        .trim()
    val json = decodeBase64Text(body) ?: return null
    return jsonStringField(json, "ps")
}

private fun bodyAfterScheme(uri: String): String =
    uri.substringAfter("://", missingDelimiterValue = "")

private fun decodeBase64Text(value: String): String? {
    val cleaned = value.filterNot { it.isWhitespace() }
    val padded = cleaned.withBase64Padding()
    val candidates = listOf(cleaned, padded).distinct()
    for (candidate in candidates) {
        for (decoder in listOf(Base64.getUrlDecoder(), Base64.getDecoder())) {
            val bytes = runCatching { decoder.decode(candidate) }.getOrNull() ?: continue
            return runCatching { bytes.toString(StandardCharsets.UTF_8) }.getOrNull()
        }
    }
    return null
}

private fun String.withBase64Padding(): String {
    val remainder = length % 4
    return if (remainder == 0) this else this + "=".repeat(4 - remainder)
}

private fun jsonStringField(json: String, key: String): String? {
    val match = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        .find(json)
        ?: return null
    return unescapeJsonString(match.groupValues[1])
}

private fun unescapeJsonString(value: String): String {
    val out = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch != '\\' || i == value.lastIndex) {
            out.append(ch)
            i += 1
            continue
        }
        when (val escaped = value[i + 1]) {
            '"', '\\', '/' -> out.append(escaped)
            'b' -> out.append('\b')
            'f' -> out.append('\u000C')
            'n' -> out.append('\n')
            'r' -> out.append('\r')
            't' -> out.append('\t')
            'u' -> {
                val hex = value.drop(i + 2).take(4)
                val decoded = hex.toIntOrNull(16)?.toChar()
                if (hex.length == 4 && decoded != null) {
                    out.append(decoded)
                    i += 4
                } else {
                    out.append(escaped)
                }
            }
            else -> out.append(escaped)
        }
        i += 2
    }
    return out.toString()
}

private fun percentDecode(value: String): String =
    runCatching {
        java.net.URLDecoder.decode(
            value.replace("+", "%2B"),
            StandardCharsets.UTF_8.name(),
        )
    }.getOrElse { value }

private val uuidRegex =
    Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

private val ipv4Regex =
    Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?$")

private val bracketedIpv6Regex =
    Regex("(?i)^\\[[0-9a-f:.%]+](?::\\d{1,5})?$")

private val hostnameRegex =
    Regex("(?i)^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9-]{2,}(?::\\d{1,5})?$")

private val hostWithPortRegex =
    Regex("(?i)^[a-z0-9][a-z0-9.-]{1,253}:\\d{1,5}$")

private val secretAssignmentRegex =
    Regex("(?i)\\b(password|token|uuid|id|alterid|pbk|sid|spx)\\s*=")

private val rawBase64Regex =
    Regex("^[A-Za-z0-9+/_-]+={0,2}$")
