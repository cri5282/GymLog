package it.gymlog.app.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
        assertEquals(Settings(2.5, OneRmFormula.EPLEY, timerVibration = true, timerSound = true), repo(backgroundScope).settings.first())
    }

    @Test fun update_persistsStep() = runTest {
        val r = repo(backgroundScope)
        r.update { it.copy(defaultLoadStepKg = 3.0, oneRmFormula = OneRmFormula.BRZYCKI, timerSound = false) }
        val s = r.settings.first()
        assertEquals(3.0, s.defaultLoadStepKg, 1e-9)
        assertEquals(OneRmFormula.BRZYCKI, s.oneRmFormula)
        assertEquals(false, s.timerSound)
    }
}
