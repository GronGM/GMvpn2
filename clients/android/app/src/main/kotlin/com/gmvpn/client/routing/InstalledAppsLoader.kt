package com.gmvpn.client.routing

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lightweight view of an installed package, suitable for a UI list. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

object InstalledAppsLoader {

    /**
     * Returns the list of user-visible installed apps, sorted by
     * label. Self is filtered out so the UI never lets the user
     * tunnel GMvpn through itself. Icons are intentionally not
     * loaded here — Compose can fetch them lazily per visible row
     * if needed later.
     */
    suspend fun load(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val self = context.packageName
        pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != self }
            .map { ai ->
                val label = runCatching { pm.getApplicationLabel(ai).toString() }
                    .getOrDefault(ai.packageName)
                val isSystem = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                InstalledApp(ai.packageName, label, isSystem)
            }
            // Drop pure-system stuff that the user has no reason to
            // route — but keep "updated system apps" since those are
            // browsers etc. that ship preinstalled.
            .filter {
                !it.isSystem || isUpdatedSystemApp(pm, it.packageName)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun isUpdatedSystemApp(pm: PackageManager, pkg: String): Boolean = try {
        val ai = pm.getApplicationInfo(pkg, 0)
        (ai.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
