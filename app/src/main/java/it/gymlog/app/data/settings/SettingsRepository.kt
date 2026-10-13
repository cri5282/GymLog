package it.gymlog.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class OneRmFormula(val label: String) { EPLEY("Epley"), BRZYCKI("Brzycki") }

data class Settings(
    val defaultLoadStepKg: Double = 2.5,
    val oneRmFormula: OneRmFormula = OneRmFormula.EPLEY,
    val timerVibration: Boolean = true,
    val timerSound: Boolean = true,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val step = doublePreferencesKey("default_load_step_kg")
        val formula = stringPreferencesKey("one_rm_formula")
        val vibration = booleanPreferencesKey("timer_vibration")
        val sound = booleanPreferencesKey("timer_sound")
    }

    val settings: Flow<Settings> = dataStore.data.map { p ->
        val d = Settings()
        Settings(
            defaultLoadStepKg = p[Keys.step] ?: d.defaultLoadStepKg,
            oneRmFormula = p[Keys.formula]?.let { runCatching { OneRmFormula.valueOf(it) }.getOrNull() } ?: d.oneRmFormula,
            timerVibration = p[Keys.vibration] ?: d.timerVibration,
            timerSound = p[Keys.sound] ?: d.timerSound,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { p ->
            val next = transform(
                Settings(
                    p[Keys.step] ?: 2.5,
                    p[Keys.formula]?.let { runCatching { OneRmFormula.valueOf(it) }.getOrNull() } ?: OneRmFormula.EPLEY,
                    p[Keys.vibration] ?: true,
                    p[Keys.sound] ?: true,
                )
            )
            p[Keys.step] = next.defaultLoadStepKg
            p[Keys.formula] = next.oneRmFormula.name
            p[Keys.vibration] = next.timerVibration
            p[Keys.sound] = next.timerSound
        }
    }
}
