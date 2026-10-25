package it.gymlog.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import it.gymlog.app.data.backup.BackupExporter
import it.gymlog.app.data.backup.WorkoutCsvExporter
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.hevy.HevyImporter
import it.gymlog.app.data.repo.BodyRepository
import it.gymlog.app.data.repo.ExerciseRepository
import it.gymlog.app.data.repo.PhotoStore
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.AppIcons
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.timer.RestTimerImpl
import it.gymlog.app.timer.RestTimerService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Se il file delle preferenze è danneggiato l'app riparte con i valori di default
// invece di bloccarsi all'avvio.
private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Crea e collega a mano database, repository e servizi dell'app. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Log.e("GymLog", "Errore durante l'inizializzazione dell'app", t)
    }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    val db: AppDatabase = AppDatabase.build(appContext)
    val settings = SettingsRepository(appContext.settingsStore)
    val hevyImporter = HevyImporter(db)
    val backup = BackupExporter(appContext, db)
    val csv = WorkoutCsvExporter(db)
    val photos = PhotoStore(appContext.filesDir)
    val exercises = ExerciseRepository(db, photos)
    val body = BodyRepository(db)
    val programs = ProgramRepository(db)
    val workouts = WorkoutRepository(db, settings)
    val restTimer: RestTimer = RestTimerImpl(onActive = { RestTimerService.start(appContext) })

    init {
        appScope.launch {
            SeedData.seedIfEmpty(db)
            // Allinea l'icona del launcher alle impostazioni (utile dopo un ripristino del backup).
            runCatching { AppIcons.apply(appContext, settings.current().appIcon) }
            autoBackupIfDue()
        }
    }

    /** Un backup a settimana nella cartella scelta, se l'utente l'ha attivato. */
    private suspend fun autoBackupIfDue() {
        val current = settings.current()
        if (!current.autoBackup) return
        val tree = current.backupTreeUri?.let(Uri::parse) ?: return
        val week = 7L * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() - current.lastBackupAt < week) return
        runCatching { backup.autoBackup(tree, appContext.contentResolver) }
            .onSuccess { settings.update { s -> s.copy(lastBackupAt = System.currentTimeMillis()) } }
    }
}
