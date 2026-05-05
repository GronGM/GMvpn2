package com.gmvpn.client.routing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.perAppRoutingDataStore by preferencesDataStore(name = "per_app_routing")

/**
 * Persists [PerAppRouting] under a single string preference. The wire
 * format is line-delimited:
 *
 *   <mode>\n
 *   <package_1>\n
 *   <package_2>\n
 *   …
 *
 * Mode is the enum name (`Off` / `IncludeOnly` / `ExcludeListed`); an
 * unknown value is treated as `Off`. Package names are not secret, so
 * we keep them in plain DataStore (unlike [ProfileStore], which goes
 * through KeystoreSecrets).
 */
class PerAppRoutingStore(private val context: Context) {

    val routing: Flow<PerAppRouting> =
        context.perAppRoutingDataStore.data.map { prefs ->
            decode(prefs[KEY] ?: "")
        }

    suspend fun setMode(mode: PerAppMode) {
        context.perAppRoutingDataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            prefs[KEY] = encode(current.copy(mode = mode))
        }
    }

    suspend fun togglePackage(pkg: String) {
        context.perAppRoutingDataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            val next = if (pkg in current.packages) {
                current.packages - pkg
            } else {
                current.packages + pkg
            }
            prefs[KEY] = encode(current.copy(packages = next))
        }
    }

    suspend fun clearPackages() {
        context.perAppRoutingDataStore.edit { prefs ->
            val current = decode(prefs[KEY] ?: "")
            prefs[KEY] = encode(current.copy(packages = emptySet()))
        }
    }

    /** One-shot read used from `GmvpnVpnService.establishTun`. */
    suspend fun snapshot(): PerAppRouting = routing.first()

    companion object {
        private val KEY = stringPreferencesKey("per_app_routing_v1")

        internal fun decode(blob: String): PerAppRouting {
            if (blob.isBlank()) return PerAppRouting()
            val lines = blob.split('\n')
            val mode = lines.firstOrNull()
                ?.let { runCatching { PerAppMode.valueOf(it) }.getOrNull() }
                ?: PerAppMode.Off
            val pkgs = lines.drop(1).filter { it.isNotEmpty() }.toSet()
            return PerAppRouting(mode, pkgs)
        }

        internal fun encode(routing: PerAppRouting): String =
            buildString {
                append(routing.mode.name)
                append('\n')
                routing.packages.sorted().forEach { append(it).append('\n') }
            }
    }
}
