package it.gymlog.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(scope: kotlinx.coroutines.CoroutineScope) = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("settings.preferences_pb") }
    )

    @Test fun defaults_whenEmpty() = runTest {
        assertEquals(
            Settings(2.5, OneRmFormula.EPLEY, timerVibration = true, timerSound = true,
                themeMode = ThemeMode.DARK, accent = Accent.GREEN, keepScreenOn = true),
            repo(backgroundScope).settings.first(),
        )
    }

    @Test fun update_persistsStep() = runTest {
        val r = repo(backgroundScope)
        r.update { it.copy(defaultLoadStepKg = 3.0, oneRmFormula = OneRmFormula.BRZYCKI, timerSound = false) }
        val s = r.settings.first()
        assertEquals(3.0, s.defaultLoadStepKg, 1e-9)
        assertEquals(OneRmFormula.BRZYCKI, s.oneRmFormula)
        assertEquals(false, s.timerSound)
    }

    @Test fun update_persistsAppearance() = runTest {
        val r = repo(backgroundScope)
        r.update { it.copy(themeMode = ThemeMode.LIGHT, accent = Accent.PURPLE, keepScreenOn = false) }
        val s = r.settings.first()
        assertEquals(ThemeMode.LIGHT, s.themeMode)
        assertEquals(Accent.PURPLE, s.accent)
        assertEquals(false, s.keepScreenOn)
    }

    @Test fun upgradeFrom100_keepsOldValuesAndNewDefaults() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.root.resolve("old.preferences_pb") }
        store.edit {
            it[doublePreferencesKey("default_load_step_kg")] = 5.0
            it[booleanPreferencesKey("timer_sound")] = false
        }
        val s = SettingsRepository(store).settings.first()
        assertEquals(5.0, s.defaultLoadStepKg, 1e-9)
        assertEquals(false, s.timerSound)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(Accent.GREEN, s.accent)
        assertEquals(true, s.keepScreenOn)
    }

    @Test fun unknownAccent_fallsBackToGreen() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.root.resolve("bad.preferences_pb") }
        store.edit { it[stringPreferencesKey("accent")] = "ROSA" }
        assertEquals(Accent.GREEN, SettingsRepository(store).settings.first().accent)
    }
}
