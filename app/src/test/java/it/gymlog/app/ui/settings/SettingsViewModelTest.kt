package it.gymlog.app.ui.settings

import androidx.test.core.app.ApplicationProvider
import it.gymlog.app.data.backup.BackupExporter
import it.gymlog.app.data.backup.WorkoutCsvExporter
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.hevy.HevyImporter
import it.gymlog.app.data.settings.Accent
import it.gymlog.app.data.settings.ThemeMode
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private suspend fun <T> real(block: suspend () -> T) =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { block() } }

    @Test fun setAccent_persists() = runTest {
        val repo = testSettings(tmp.root, backgroundScope)
        SettingsViewModel(repo, HevyImporter(db), BackupExporter(ApplicationProvider.getApplicationContext(), db), WorkoutCsvExporter(db)).setAccent(Accent.PURPLE)
        assertEquals(Accent.PURPLE, real { repo.settings.first { it.accent == Accent.PURPLE } }.accent)
    }

    @Test fun setThemeMode_persists() = runTest {
        val repo = testSettings(tmp.root, backgroundScope)
        SettingsViewModel(repo, HevyImporter(db), BackupExporter(ApplicationProvider.getApplicationContext(), db), WorkoutCsvExporter(db)).setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, real { repo.settings.first { it.themeMode == ThemeMode.LIGHT } }.themeMode)
    }

    @Test fun setKeepScreenOn_persists() = runTest {
        val repo = testSettings(tmp.root, backgroundScope)
        SettingsViewModel(repo, HevyImporter(db), BackupExporter(ApplicationProvider.getApplicationContext(), db), WorkoutCsvExporter(db)).setKeepScreenOn(false)
        assertEquals(false, real { repo.settings.first { !it.keepScreenOn } }.keepScreenOn)
    }
}
