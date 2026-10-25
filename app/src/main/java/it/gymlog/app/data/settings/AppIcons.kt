package it.gymlog.app.data.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Attiva una sola icona della app (activity-alias) e disattiva le altre.
 * DONT_KILL_APP evita di chiudere l'app mentre l'utente è nelle impostazioni.
 */
object AppIcons {
    fun apply(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        val packageName = context.packageName
        AppIcon.entries.forEach { candidate ->
            val component = ComponentName(packageName, packageName + candidate.alias)
            val desired = if (candidate == icon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                if (pm.getComponentEnabledSetting(component) != desired) {
                    pm.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
