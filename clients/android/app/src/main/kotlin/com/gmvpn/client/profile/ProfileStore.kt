package com.gmvpn.client.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profiles")

/**
 * Multi-profile library storage. Profiles are URI strings encrypted
 * with [KeystoreSecrets] (AES-256-GCM, key in AndroidKeyStore); the
 * raw URI never lives on disk. The wire format inside DataStore is a
 * single string preference `library_v3`:
 *
 * ```
 * <active_index>\n
 * <encrypted_uri_1>\n
 * <encrypted_uri_2>\n
 * …
 * ```
 *
 * `active_index = -1` means "no profile selected". A blank value is
 * treated as an empty library.
 *
 * When `library_v3` is absent but the legacy single-URI key
 * `active_profile_uri_v2` from the previous storage shape exists,
 * the first read migrates that value into the library as the active
 * entry.
 */
class ProfileStore(
    private val context: Context,
    private val secrets: KeystoreSecrets = KeystoreSecrets(),
) {

    /** Decrypted profile entries, in storage order. */
    val entries: Flow<List<ProfileEntry>> =
        context.profileDataStore.data.map { prefs ->
            decode(prefs).items.mapNotNull { cipher ->
                secrets.decrypt(cipher)
                    ?.let(StoredProfileEntry::decode)
                    ?.takeIf { it.uri.isNotBlank() }
            }
        }

    /** Decrypted URIs, in storage order. */
    val library: Flow<List<String>> =
        entries.map { profiles -> profiles.map { it.uri } }

    /** Index of the active profile in [library], or -1 if none. */
    val activeIndex: Flow<Int> =
        context.profileDataStore.data.map { prefs -> decode(prefs).active }

    /** Convenience flow: decrypted URI of the active profile, or null. */
    val activeUri: Flow<String?> =
        context.profileDataStore.data.map { prefs ->
            val state = decode(prefs)
            val cipher = state.items.getOrNull(state.active) ?: return@map null
            secrets.decrypt(cipher)
                ?.let(StoredProfileEntry::decode)
                ?.uri
                ?.takeIf { it.isNotBlank() }
        }

    /** Append a URI to the library; returns its new index. Becomes
     *  active if it is the first profile. */
    suspend fun addUri(
        uri: String,
        source: ProfileSource = ProfileSource.MANUAL,
        customName: String? = null,
    ): Int {
        val now = System.currentTimeMillis()
        val entry = ProfileEntry(
            uri = uri,
            customName = customName?.let(::sanitizeCustomProfileName),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            source = source,
        )
        val cipher = secrets.encrypt(StoredProfileEntry.encode(entry))
        var newIndex = 0
        context.profileDataStore.edit { prefs ->
            val state = decode(prefs)
            val items = state.items + cipher
            newIndex = items.size - 1
            val active = if (state.active < 0) newIndex else state.active
            prefs[KEY_LIB] = encode(active, items)
            prefs.remove(LEGACY_KEY)
        }
        return newIndex
    }

    /** Replace the library wholesale (e.g. after a subscription
     *  import). The first entry becomes active; returns the count. */
    suspend fun replaceAll(uris: List<String>): Int =
        replaceAllEntries(
            uris.map { ProfileEntryInput(uri = it, source = ProfileSource.SUBSCRIPTION) },
        )

    suspend fun replaceAllEntries(profiles: List<ProfileEntryInput>): Int {
        val now = System.currentTimeMillis()
        val ciphers = profiles.map { input ->
            val entry = ProfileEntry(
                uri = input.uri,
                customName = input.customName?.let(::sanitizeCustomProfileName),
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                source = input.source,
            )
            secrets.encrypt(StoredProfileEntry.encode(entry))
        }
        context.profileDataStore.edit { prefs ->
            val active = if (ciphers.isEmpty()) -1 else 0
            prefs[KEY_LIB] = encode(active, ciphers)
            prefs.remove(LEGACY_KEY)
        }
        return ciphers.size
    }

    suspend fun renameAt(index: Int, rawName: String): Boolean {
        val safeName = sanitizeCustomProfileName(rawName) ?: return false
        var updated = false
        context.profileDataStore.edit { prefs ->
            val state = decode(prefs)
            val cipher = state.items.getOrNull(index) ?: return@edit
            val entry = secrets.decrypt(cipher)
                ?.let(StoredProfileEntry::decode)
                ?: return@edit
            val items = state.items.toMutableList()
            items[index] = secrets.encrypt(
                StoredProfileEntry.encode(
                    entry.copy(
                        customName = safeName,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                ),
            )
            prefs[KEY_LIB] = encode(state.active, items)
            updated = true
        }
        return updated
    }

    suspend fun setActive(index: Int) {
        context.profileDataStore.edit { prefs ->
            val state = decode(prefs)
            val bounded = if (index in state.items.indices) index else -1
            prefs[KEY_LIB] = encode(bounded, state.items)
        }
    }

    suspend fun removeAt(index: Int) {
        context.profileDataStore.edit { prefs ->
            val state = decode(prefs)
            if (index !in state.items.indices) return@edit
            val items = state.items.toMutableList().also { it.removeAt(index) }
            val active = activeIndexAfterRemoval(state.active, index, state.items.size)
            prefs[KEY_LIB] = encode(active, items)
        }
    }

    suspend fun clearAll() {
        context.profileDataStore.edit { prefs ->
            prefs.remove(KEY_LIB)
            prefs.remove(LEGACY_KEY)
        }
    }

    /** Backwards-compatible single-URI setter. If the URI is already
     *  in the library it is selected; otherwise it is appended and
     *  becomes active. */
    suspend fun setActiveUri(uri: String) {
        val existing = entries.first().indexOfFirst { it.uri == uri }
        if (existing >= 0) {
            setActive(existing)
        } else {
            addUri(uri).also { setActive(it) }
        }
    }

    private fun decode(prefs: Preferences): State {
        // Migrate the legacy single-URI key on first read.
        val raw = prefs[KEY_LIB]
        if (raw != null) return parse(raw)
        val legacy = prefs[LEGACY_KEY]
        return if (legacy.isNullOrBlank()) {
            State(-1, emptyList())
        } else {
            State(0, listOf(legacy))
        }
    }

    private data class State(val active: Int, val items: List<String>)

    companion object {
        private val KEY_LIB = stringPreferencesKey("library_v3")
        private val LEGACY_KEY = stringPreferencesKey("active_profile_uri_v2")

        private fun parse(blob: String): State {
            if (blob.isBlank()) return State(-1, emptyList())
            val lines = blob.split('\n')
            val active = lines.firstOrNull()?.toIntOrNull() ?: -1
            val items = lines.drop(1).filter { it.isNotEmpty() }
            val bounded = if (active in items.indices) active else -1
            return State(bounded, items)
        }

        private fun encode(active: Int, items: List<String>): String =
            buildString {
                append(if (items.isEmpty()) -1 else active)
                append('\n')
                items.forEach { append(it).append('\n') }
            }
    }
}
