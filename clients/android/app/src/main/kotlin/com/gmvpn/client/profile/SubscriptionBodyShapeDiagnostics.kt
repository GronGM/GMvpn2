package com.gmvpn.client.profile

import java.nio.charset.StandardCharsets
import java.util.Base64
import uniffi.gmvpn_ffi.FfiSubscriptionFormat

enum class SubscriptionBodyAvailability(val safeValue: String) {
    Yes("yes"),
    No("no"),
    Unknown("unknown"),
}

enum class SubscriptionBodyShapeLengthBucket(val safeValue: String) {
    Empty("empty"),
    Tiny("tiny"),
    Small("small"),
    Medium("medium"),
    Large("large"),
    VeryLarge("veryLarge"),
    Unknown("unknown"),
}

enum class SubscriptionLineCountBucket(val safeValue: String) {
    One("one"),
    Few("few"),
    Many("many"),
    Unknown("unknown"),
}

enum class SubscriptionSupportedUriSchemeCountBucket(val safeValue: String) {
    Zero("zero"),
    One("one"),
    Few("few"),
    Many("many"),
    Unknown("unknown"),
}

enum class SubscriptionDecodedControlCharBucket(val safeValue: String) {
    None("none"),
    Few("few"),
    Many("many"),
    Unknown("unknown"),
}

enum class SubscriptionRequestedFormatBucket(val safeValue: String) {
    DefaultBase64("default_base64"),
    UriList("uri_list"),
    Sip008("sip008"),
    Unknown("unknown"),
}

enum class SubscriptionDecodeFailureKind(val safeValue: String) {
    UnsupportedFormat("unsupported_format"),
    EmptyBody("empty_body"),
    NoProfiles("no_profiles"),
    MalformedBase64("malformed_base64"),
    MalformedJson("malformed_json"),
    DecodedNoSupportedUriScheme("decoded_no_supported_uri_scheme"),
    DecodedContainsSupportedUriSchemeButFfiFailed("decoded_contains_supported_uri_scheme_but_ffi_failed"),
    DecodedClashYamlUnsupported("decoded_clash_yaml_unsupported"),
    DecodedSingBoxJsonUnsupported("decoded_singbox_json_unsupported"),
    DecodedSip008JsonUnsupported("decoded_sip008_json_unsupported"),
    DecodedHtmlError("decoded_html_error"),
    DecodedEmpty("decoded_empty"),
    DoubleBase64Likely("double_base64_likely"),
    FfiDecodeFailed("ffi_decode_failed"),
    Unknown("unknown"),
}

data class SubscriptionBodyShapeDiagnostics(
    val bodyAvailable: SubscriptionBodyAvailability = SubscriptionBodyAvailability.Unknown,
    val bodyLengthBucket: SubscriptionBodyShapeLengthBucket = SubscriptionBodyShapeLengthBucket.Unknown,
    val lineCountBucket: SubscriptionLineCountBucket = SubscriptionLineCountBucket.Unknown,
    val looksBase64: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val base64DecodeLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val looksUriList: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val looksJson: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val looksSip008: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val looksHtml: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val containsSupportedUriScheme: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val supportedUriSchemeCountBucket: SubscriptionSupportedUriSchemeCountBucket =
        SubscriptionSupportedUriSchemeCountBucket.Unknown,
    val requestedFormat: SubscriptionRequestedFormatBucket = SubscriptionRequestedFormatBucket.Unknown,
    val decodeFailureKind: SubscriptionDecodeFailureKind = SubscriptionDecodeFailureKind.Unknown,
    val decodedBodyAvailable: SubscriptionBodyAvailability = SubscriptionBodyAvailability.Unknown,
    val decodedBodyLengthBucket: SubscriptionBodyShapeLengthBucket = SubscriptionBodyShapeLengthBucket.Unknown,
    val decodedLineCountBucket: SubscriptionLineCountBucket = SubscriptionLineCountBucket.Unknown,
    val decodedLooksUriList: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedContainsSupportedUriScheme: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedSupportedUriSchemeCountBucket: SubscriptionSupportedUriSchemeCountBucket =
        SubscriptionSupportedUriSchemeCountBucket.Unknown,
    val decodedLooksJson: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksSip008: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksHtml: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksYaml: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksClash: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksSingBox: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedLooksBase64Again: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedPrintableTextLikely: SubscriptionDiagnosticTriState = SubscriptionDiagnosticTriState.Unknown,
    val decodedControlCharBucket: SubscriptionDecodedControlCharBucket =
        SubscriptionDecodedControlCharBucket.Unknown,
) {
    fun withDecodeFailureKind(kind: SubscriptionDecodeFailureKind): SubscriptionBodyShapeDiagnostics =
        copy(decodeFailureKind = kind)

    companion object {
        val Unknown = SubscriptionBodyShapeDiagnostics()

        fun fromBody(
            body: ByteArray,
            requestedFormat: FfiSubscriptionFormat,
        ): SubscriptionBodyShapeDiagnostics {
            val text = body.toString(StandardCharsets.UTF_8)
            val trimmed = text.trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
            val compact = trimmed.filterNot { it.isWhitespace() }
            val lower = trimmed.lowercase()
            val supportedUriCount = supportedUriRegex.findAll(text).count()
            val looksBase64 = compact.isNotEmpty() &&
                compact.length % 4 == 0 &&
                compact.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
            val decodedPayload = if (looksBase64) {
                decodeBase64Payload(compact)
            } else {
                null
            }
            val base64DecodeLikely = decodedPayload != null
            val looksJson = trimmed.startsWith("{") || trimmed.startsWith("[")
            val looksHtml = lower.startsWith("<!doctype html") ||
                lower.startsWith("<html") ||
                "<html" in lower
            val lineCount = trimmed
                .lineSequence()
                .filter { it.isNotBlank() }
                .take(11)
                .count()

            return SubscriptionBodyShapeDiagnostics(
                bodyAvailable = SubscriptionBodyAvailability.Yes,
                bodyLengthBucket = body.size.toBodyLengthBucket(),
                lineCountBucket = lineCount.toLineCountBucket(),
                looksBase64 = looksBase64.toTriState(),
                base64DecodeLikely = if (looksBase64) {
                    base64DecodeLikely.toTriState()
                } else {
                    SubscriptionDiagnosticTriState.Unknown
                },
                looksUriList = (supportedUriCount > 0 || trimmed.lines().size > 1)
                    .toTriState(),
                looksJson = looksJson.toTriState(),
                looksSip008 = (looksJson && "\"servers\"" in lower).toTriState(),
                looksHtml = looksHtml.toTriState(),
                containsSupportedUriScheme = (supportedUriCount > 0).toTriState(),
                supportedUriSchemeCountBucket = supportedUriCount.toSupportedUriCountBucket(),
                requestedFormat = requestedFormat.toRequestedFormatBucket(),
                decodedBodyAvailable = decodedPayload.toDecodedAvailability(requestedFormat, looksBase64),
                decodedBodyLengthBucket = decodedPayload?.size?.toBodyLengthBucket()
                    ?: SubscriptionBodyShapeLengthBucket.Unknown,
                decodedLineCountBucket = decodedPayload?.toUtf8Text()?.nonBlankLineCount()?.toLineCountBucket()
                    ?: SubscriptionLineCountBucket.Unknown,
                decodedLooksUriList = decodedPayload?.toUtf8Text()?.looksUriListTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedContainsSupportedUriScheme = decodedPayload?.toUtf8Text()
                    ?.containsSupportedUriSchemeTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedSupportedUriSchemeCountBucket = decodedPayload?.toUtf8Text()
                    ?.supportedUriCount()
                    ?.toSupportedUriCountBucket()
                    ?: SubscriptionSupportedUriSchemeCountBucket.Unknown,
                decodedLooksJson = decodedPayload?.toUtf8Text()?.looksJsonTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksSip008 = decodedPayload?.toUtf8Text()?.looksSip008TriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksHtml = decodedPayload?.toUtf8Text()?.looksHtmlTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksYaml = decodedPayload?.toUtf8Text()?.looksYamlTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksClash = decodedPayload?.toUtf8Text()?.looksClashTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksSingBox = decodedPayload?.toUtf8Text()?.looksSingBoxTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedLooksBase64Again = decodedPayload?.toUtf8Text()?.looksBase64AgainTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedPrintableTextLikely = decodedPayload?.printableTextLikelyTriState()
                    ?: SubscriptionDiagnosticTriState.Unknown,
                decodedControlCharBucket = decodedPayload?.toUtf8Text()?.controlCharCount()
                    ?.toDecodedControlCharBucket()
                    ?: SubscriptionDecodedControlCharBucket.Unknown,
            )
        }

        private val supportedUriRegex = Regex(
            pattern = "(?i)\\b(?:vless|vmess|trojan|ss)://",
        )

    }
}

private val yamlKeyRegex = Regex(
    pattern = "(?m)^\\s*[A-Za-z0-9_-]+\\s*:",
)

private fun decodeBase64Payload(compact: String): ByteArray? {
    val padded = compact.padEnd(compact.length + ((4 - compact.length % 4) % 4), '=')
    return runCatching { Base64.getDecoder().decode(padded) }
        .recoverCatching { Base64.getUrlDecoder().decode(padded) }
        .getOrNull()
}

private fun ByteArray?.toDecodedAvailability(
    requestedFormat: FfiSubscriptionFormat,
    rawLooksBase64: Boolean,
): SubscriptionBodyAvailability =
    when {
        requestedFormat != FfiSubscriptionFormat.BASE64_URI_LIST -> SubscriptionBodyAvailability.Unknown
        this != null -> SubscriptionBodyAvailability.Yes
        rawLooksBase64 -> SubscriptionBodyAvailability.No
        else -> SubscriptionBodyAvailability.Unknown
    }

private fun ByteArray.toUtf8Text(): String =
    toString(StandardCharsets.UTF_8)

private fun ByteArray.printableTextLikelyTriState(): SubscriptionDiagnosticTriState {
    if (isEmpty()) return SubscriptionDiagnosticTriState.Yes
    val text = toUtf8Text()
    val replacementCount = text.count { it == '\uFFFD' }
    val controlCount = text.controlCharCount()
    return (replacementCount <= 1 && controlCount <= 10).toTriState()
}

private fun String.nonBlankLineCount(): Int =
    trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
        .lineSequence()
        .filter { it.isNotBlank() }
        .take(11)
        .count()

private fun String.supportedUriCount(): Int =
    Regex("(?i)\\b(?:vless|vmess|trojan|ss)://").findAll(this).count()

private fun String.containsSupportedUriSchemeTriState(): SubscriptionDiagnosticTriState =
    (supportedUriCount() > 0).toTriState()

private fun String.looksUriListTriState(): SubscriptionDiagnosticTriState {
    val trimmed = trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
    return (supportedUriCount() > 0 || trimmed.lineSequence().filter { it.isNotBlank() }.take(2).count() > 1)
        .toTriState()
}

private fun String.looksJsonTriState(): SubscriptionDiagnosticTriState {
    val trimmed = trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
    return (trimmed.startsWith("{") || trimmed.startsWith("[")).toTriState()
}

private fun String.looksSip008TriState(): SubscriptionDiagnosticTriState {
    val lower = lowercase()
    return ((looksJsonTriState() == SubscriptionDiagnosticTriState.Yes) && "\"servers\"" in lower).toTriState()
}

private fun String.looksHtmlTriState(): SubscriptionDiagnosticTriState {
    val lower = trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }.lowercase()
    return (lower.startsWith("<!doctype html") || lower.startsWith("<html") || "<html" in lower).toTriState()
}

private fun String.looksYamlTriState(): SubscriptionDiagnosticTriState {
    val trimmed = trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
    val lower = trimmed.lowercase()
    val looksStructuredYaml = yamlKeyRegex.containsMatchIn(trimmed)
    val looksNotJsonOrHtml = !lower.startsWith("{") && !lower.startsWith("[") && !lower.startsWith("<")
    return (looksNotJsonOrHtml && looksStructuredYaml).toTriState()
}

private fun String.looksClashTriState(): SubscriptionDiagnosticTriState {
    val lower = lowercase()
    return (
        looksYamlTriState() == SubscriptionDiagnosticTriState.Yes &&
            ("proxies:" in lower || "proxy-groups:" in lower || "proxy-providers:" in lower)
        ).toTriState()
}

private fun String.looksSingBoxTriState(): SubscriptionDiagnosticTriState {
    val lower = lowercase()
    return (
        looksJsonTriState() == SubscriptionDiagnosticTriState.Yes &&
            ("\"outbounds\"" in lower || "\"inbounds\"" in lower || "\"route\"" in lower)
        ).toTriState()
}

private fun String.looksBase64AgainTriState(): SubscriptionDiagnosticTriState {
    val compact = trim { it.isWhitespace() || it.isInvisibleEdgeCharacter() }
        .filterNot { it.isWhitespace() }
    val looksBase64 = compact.isNotEmpty() &&
        compact.length % 4 == 0 &&
        compact.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
    return (looksBase64 && decodeBase64Payload(compact) != null).toTriState()
}

private fun String.controlCharCount(): Int =
    count { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }

private fun Int.toDecodedControlCharBucket(): SubscriptionDecodedControlCharBucket =
    when {
        this <= 0 -> SubscriptionDecodedControlCharBucket.None
        this <= 10 -> SubscriptionDecodedControlCharBucket.Few
        else -> SubscriptionDecodedControlCharBucket.Many
    }

private fun Int.toBodyLengthBucket(): SubscriptionBodyShapeLengthBucket =
    when {
        this == 0 -> SubscriptionBodyShapeLengthBucket.Empty
        this <= 64 -> SubscriptionBodyShapeLengthBucket.Tiny
        this <= 4 * 1024 -> SubscriptionBodyShapeLengthBucket.Small
        this <= 128 * 1024 -> SubscriptionBodyShapeLengthBucket.Medium
        this <= 1024 * 1024 -> SubscriptionBodyShapeLengthBucket.Large
        else -> SubscriptionBodyShapeLengthBucket.VeryLarge
    }

private fun Int.toLineCountBucket(): SubscriptionLineCountBucket =
    when {
        this <= 0 -> SubscriptionLineCountBucket.Unknown
        this == 1 -> SubscriptionLineCountBucket.One
        this <= 10 -> SubscriptionLineCountBucket.Few
        else -> SubscriptionLineCountBucket.Many
    }

private fun Int.toSupportedUriCountBucket(): SubscriptionSupportedUriSchemeCountBucket =
    when {
        this <= 0 -> SubscriptionSupportedUriSchemeCountBucket.Zero
        this == 1 -> SubscriptionSupportedUriSchemeCountBucket.One
        this <= 10 -> SubscriptionSupportedUriSchemeCountBucket.Few
        else -> SubscriptionSupportedUriSchemeCountBucket.Many
    }

private fun Boolean.toTriState(): SubscriptionDiagnosticTriState =
    if (this) SubscriptionDiagnosticTriState.Yes else SubscriptionDiagnosticTriState.No

private fun FfiSubscriptionFormat.toRequestedFormatBucket(): SubscriptionRequestedFormatBucket =
    when (this) {
        FfiSubscriptionFormat.BASE64_URI_LIST -> SubscriptionRequestedFormatBucket.DefaultBase64
        FfiSubscriptionFormat.URI_LIST -> SubscriptionRequestedFormatBucket.UriList
        FfiSubscriptionFormat.SIP008 -> SubscriptionRequestedFormatBucket.Sip008
    }

private fun Char.isInvisibleEdgeCharacter(): Boolean =
    this == '\u200B' ||
        this == '\u200C' ||
        this == '\u200D' ||
        this == '\u2060' ||
        this == '\uFEFF'
