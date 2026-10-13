package it.gymlog.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.hevy.HevyImporter
import it.gymlog.app.data.repo.ExerciseRepository
import it.gymlog.app.data.repo.PhotoStore
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.timer.RestTimerImpl
import it.gymlog.app.timer.RestTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Crea e collega a mano database, repository e servizi dell'app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: AppDatabase = AppDatabase.build(appContext)
    val settings = SettingsRepository(appContext.settingsStore)
    val hevyImporter = HevyImporter(db)
    val photos = PhotoStore(appContext.filesDir)
    val exercises = ExerciseRepository(db, photos)
    val programs = ProgramRepository(db)
    val workouts = WorkoutRepository(db, settings)
    val restTimer: RestTimer = RestTimerImpl(onActive = { RestTimerService.start(appContext) })

    init {
        appScope.launch { SeedData.seedIfEmpty(db) }
    }
}
