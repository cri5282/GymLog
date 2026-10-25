package it.gymlog.app.testutil

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private val settingsCounter = AtomicInteger()

/**
 * Impostazioni per i test. Ogni chiamata usa un file diverso: due DataStore sullo stesso file
 * (come accade quando un test crea più repository) corromperebbero le preferenze.
 * Il riscaldamento automatico è disattivato per non alterare i test già esistenti; i test che
 * lo riguardano lo riattivano esplicitamente nelle impostazioni.
 */
fun testSettings(dir: File, scope: CoroutineScope): SettingsRepository {
    val file = File(dir, "s-${settingsCounter.incrementAndGet()}.preferences_pb")
    return SettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope) { file },
        defaults = Settings(autoWarmup = false),
    )
}
