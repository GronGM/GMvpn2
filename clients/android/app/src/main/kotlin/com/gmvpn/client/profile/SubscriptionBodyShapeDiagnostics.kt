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
            val base64DecodeLikely = if (looksBase64) {
                runCatching { Base64.getDecoder().decode(compact) }.isSuccess
            } else {
                false
            }
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
            )
        }

        private val supportedUriRegex = Regex(
            pattern = "(?i)\\b(?:vless|vmess|trojan|ss)://",
        )
    }
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
