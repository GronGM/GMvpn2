package com.gmvpn.client.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profiles")

/**
 * Single-active-profile storage. Persists the URI string entered in
 * the UI, **encrypted** with an AES-256-GCM key kept inside Android
 * Keystore (see [KeystoreSecrets]). The raw URI never touches disk.
 *
 * Real profile management (multiple profiles, subscriptions, separate
 * storage of credentials vs. routing rules) lands once we have a
 * proper profile screen — for the first live tunnel one URI is
 * enough.
 */
class ProfileStore(
    private val context: Context,
    private val secrets: KeystoreSecrets = KeystoreSecrets(),
) {

    /** Decrypted URI, or null when nothing is stored / decryption fails. */
    val activeUri: Flow<String?> =
        context.profileDataStore.data.map { prefs ->
            val cipher = prefs[KEY_URI] ?: return@map null
            secrets.decrypt(cipher)
        }

    suspend fun setActiveUri(uri: String) {
        val cipher = secrets.encrypt(uri)
        context.profileDataStore.edit { it[KEY_URI] = cipher }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.remove(KEY_URI) }
    }

    companion object {
        // The "v2" suffix marks the format change to encrypted bytes.
        // Older debug builds wrote the URI in plain text under
        // "active_profile_uri"; that key is intentionally not read so
        // a stale plaintext copy cannot be revived.
        private val KEY_URI = stringPreferencesKey("active_profile_uri_v2")
    }
}
