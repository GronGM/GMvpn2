package com.gmvpn.client.profile

import java.nio.charset.StandardCharsets
import java.util.Base64

data class ProfileEntry(
    val uri: String,
    val customName: String?,
    val createdAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long?,
    val source: ProfileSource,
)

data class ProfileEntryInput(
    val uri: String,
    val customName: String? = null,
    val source: ProfileSource = ProfileSource.MANUAL,
)

enum class ProfileSource {
    MANUAL,
    SUBSCRIPTION,
    IMPORT,
    LEGACY,
}

data class ProfileImportPreview(
    val uri: String,
    val suggestedName: String,
    val protocolLabel: String,
)

data class ProfileImportPlan(
    val profiles: List<ProfileImportPreview>,
    val duplicateUriCount: Int,
)

fun profileDisplaySummary(entry: ProfileEntry, fallbackIndex: Int): ProfileSummary {
    val derived = profileSummary(entry.uri, fallbackIndex)
    return ProfileSummary(
        displayName = entry.customName ?: derived.displayName,
        secondaryLabel = derived.secondaryLabel,
    )
}

fun sanitizeCustomProfileName(raw: String): String? =
    sanitizeProfileDisplayName(raw)

fun activeIndexAfterRemoval(activeIndex: Int, removedIndex: Int, itemCount: Int): Int {
    if (removedIndex !in 0 until itemCount) return activeIndex
    val remaining = itemCount - 1
    return when {
        remaining <= 0 -> -1
        activeIndex == removedIndex -> 0
        activeIndex > removedIndex -> activeIndex - 1
        activeIndex in 0 until itemCount -> activeIndex
        else -> -1
    }
}

fun buildProfileImportPlan(uris: List<String>): ProfileImportPlan {
    val uniqueUris = uris.distinct()
    val usedNames = mutableMapOf<String, Int>()
    val previews = uniqueUris.mapIndexed { index, uri ->
        val summary = profileSummary(uri, index + 1)
        val baseName = summary.displayName
        val ordinal = (usedNames[baseName] ?: 0) + 1
        usedNames[baseName] = ordinal
        ProfileImportPreview(
            uri = uri,
            suggestedName = if (ordinal == 1) baseName else "$baseName ($ordinal)",
            protocolLabel = summary.secondaryLabel,
        )
    }
    return ProfileImportPlan(
        profiles = previews,
        duplicateUriCount = uris.size - uniqueUris.size,
    )
}

internal object StoredProfileEntry {
    private const val PREFIX = "gmvpn-profile-v1|"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(entry: ProfileEntry): String =
        PREFIX + listOf(
            encodeText(entry.uri),
            encodeText(entry.customName.orEmpty()),
            entry.createdAtEpochMillis?.toString().orEmpty(),
            entry.updatedAtEpochMillis?.toString().orEmpty(),
            entry.source.name,
        ).joinToString("|")

    fun decode(value: String): ProfileEntry {
        if (!value.startsWith(PREFIX)) {
            return ProfileEntry(
                uri = value,
                customName = null,
                createdAtEpochMillis = null,
                updatedAtEpochMillis = null,
                source = ProfileSource.LEGACY,
            )
        }
        val parts = value.removePrefix(PREFIX).split('|')
        val uri = parts.getOrNull(0)?.decodeTextOrNull().orEmpty()
        val customName = parts.getOrNull(1)
            ?.decodeTextOrNull()
            ?.let(::sanitizeCustomProfileName)
        val source = parts.getOrNull(4)
            ?.let { runCatching { ProfileSource.valueOf(it) }.getOrNull() }
            ?: ProfileSource.IMPORT
        return ProfileEntry(
            uri = uri,
            customName = customName,
            createdAtEpochMillis = parts.getOrNull(2)?.toLongOrNull(),
            updatedAtEpochMillis = parts.getOrNull(3)?.toLongOrNull(),
            source = source,
        )
    }

    private fun encodeText(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun String.decodeTextOrNull(): String? =
        runCatching {
            decoder.decode(this).toString(StandardCharsets.UTF_8)
        }.getOrNull()
}
