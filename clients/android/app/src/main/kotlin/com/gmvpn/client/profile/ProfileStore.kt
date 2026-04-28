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
 * the UI. Real profile management (multiple profiles, subscriptions,
 * encrypted credentials) lands once we have a profile screen — for the
 * first live tunnel one URI is enough.
 */
class ProfileStore(private val context: Context) {

    val activeUri: Flow<String?> =
        context.profileDataStore.data.map { it[KEY_URI] }

    suspend fun setActiveUri(uri: String) {
        context.profileDataStore.edit { it[KEY_URI] = uri }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.remove(KEY_URI) }
    }

    companion object {
        private val KEY_URI = stringPreferencesKey("active_profile_uri")
    }
}
