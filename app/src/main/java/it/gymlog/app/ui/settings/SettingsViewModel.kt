package it.gymlog.app.ui.settings

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.backup.BackupExporter
import it.gymlog.app.data.backup.RestoreException
import it.gymlog.app.data.backup.RestoreNeedsRestart
import it.gymlog.app.data.backup.WorkoutCsvExporter
import it.gymlog.app.data.hevy.HevyFormatException
import it.gymlog.app.data.hevy.HevyImporter
import it.gymlog.app.data.hevy.ImportReport
import it.gymlog.app.data.settings.Accent
import it.gymlog.app.data.settings.AppIcon
import it.gymlog.app.data.settings.AppIcons
import it.gymlog.app.data.settings.OneRmFormula
import it.gymlog.app.data.settings.Sex
import it.gymlog.app.data.settings.ThemeMode
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportState {
    data object Idle : ImportState
    data object Running : ImportState
    data class Done(val report: ImportReport) : ImportState
    data class Failed(val message: String) : ImportState
}

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val importer: HevyImporter,
    private val backup: BackupExporter,
    private val csv: WorkoutCsvExporter,
) : ViewModel() {
    val settings: StateFlow<Settings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _import = MutableStateFlow<ImportState>(ImportState.Idle)
    val import: StateFlow<ImportState> = _import.asStateFlow()

    private val _busy = MutableStateFlow(false)
    /** Operazione lunga in corso (backup, ripristino, CSV). */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setStep(kg: Double) = viewModelScope.launch {
        repo.update { it.copy(defaultLoadStepKg = kg.coerceIn(0.25, 20.0)) }
    }
    fun setFormula(f: OneRmFormula) = viewModelScope.launch { repo.update { it.copy(oneRmFormula = f) } }
    fun setVibration(on: Boolean) = viewModelScope.launch { repo.update { it.copy(timerVibration = on) } }
    fun setSound(on: Boolean) = viewModelScope.launch { repo.update { it.copy(timerSound = on) } }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.update { it.copy(themeMode = mode) } }
    fun setAccent(accent: Accent) = viewModelScope.launch { repo.update { it.copy(accent = accent) } }
    fun setKeepScreenOn(on: Boolean) = viewModelScope.launch { repo.update { it.copy(keepScreenOn = on) } }
    fun setAutoWarmup(on: Boolean) = viewModelScope.launch { repo.update { it.copy(autoWarmup = on) } }
    fun setSex(sex: Sex) = viewModelScope.launch { repo.update { it.copy(sex = sex) } }
    fun setHeight(cm: Double?) = viewModelScope.launch { repo.update { it.copy(heightCm = cm?.takeIf { v -> v in 100.0..250.0 }) } }
    fun setBirthYear(year: Int?) = viewModelScope.launch {
        repo.update { it.copy(birthYear = year?.takeIf { v -> v in 1900..2020 }) }
    }
    fun setAppIcon(icon: AppIcon, context: android.content.Context) = viewModelScope.launch {
        repo.update { it.copy(appIcon = icon) }
        withContext(Dispatchers.IO) { AppIcons.apply(context, icon) }
    }

    fun importHevy(uri: Uri, resolver: ContentResolver) {
        _import.value = ImportState.Running
        viewModelScope.launch {
            _import.value = try {
                val report = withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: throw HevyFormatException("Impossibile aprire il file.")
                    stream.reader(Charsets.UTF_8).use { importer.import(it) }
                }
                ImportState.Done(report)
            } catch (e: HevyFormatException) {
                ImportState.Failed(e.message ?: "File non valido.")
            } catch (e: java.io.IOException) {
                ImportState.Failed("Errore di lettura: ${e.message}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ImportState.Failed("Importazione non riuscita: ${e.message ?: "errore sconosciuto"}")
            }
        }
    }

    fun dismissImport() { _import.value = ImportState.Idle }

    private val _backup = MutableStateFlow<String?>(null)
    /** Esito dell'ultima operazione di backup (messaggio da mostrare), null se già letto. */
    val backupMessage: StateFlow<String?> = _backup.asStateFlow()

    private val _restart = MutableStateFlow(false)
    /** Dopo un ripristino riuscito l'app va riavviata per riaprire il database. */
    val restartNeeded: StateFlow<Boolean> = _restart.asStateFlow()

    fun exportBackup(uri: Uri, resolver: ContentResolver) = longOperation {
        val files = resolver.openOutputStream(uri)?.use { backup.export(it) }
            ?: throw java.io.IOException("Impossibile scrivere il file.")
        "Backup salvato ($files file)."
    }

    fun restoreBackup(uri: Uri, resolver: ContentResolver) = longOperation {
        val files = resolver.openInputStream(uri)?.use { backup.restore(it) }
            ?: throw java.io.IOException("Impossibile leggere il file.")
        _restart.value = true
        "Backup ripristinato ($files file). L'app verrà riavviata."
    }

    fun exportCsv(uri: Uri, resolver: ContentResolver) = longOperation {
        val rows = resolver.openOutputStream(uri)?.use { csv.export(it) }
            ?: throw java.io.IOException("Impossibile scrivere il file.")
        "CSV salvato ($rows serie)."
    }

    /** Salva la cartella dei backup automatici (permesso persistente) e attiva il backup settimanale. */
    fun setBackupFolder(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        repo.update { it.copy(backupTreeUri = uri.toString(), autoBackup = true) }
    }

    fun setAutoBackup(on: Boolean) = viewModelScope.launch {
        repo.update { it.copy(autoBackup = on) }
    }

    fun backupNow(resolver: ContentResolver) = longOperation {
        val tree = settings.value.backupTreeUri?.let(Uri::parse)
            ?: throw java.io.IOException("Scegli prima una cartella per i backup.")
        val files = backup.autoBackup(tree, resolver)
        repo.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
        "Backup salvato nella cartella scelta ($files file)."
    }

    fun restartApp() = backup.restartApp()

    fun dismissBackup() { _backup.value = null }

    private fun longOperation(block: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            _backup.value = try {
                block()
            } catch (e: RestoreNeedsRestart) {
                _restart.value = true
                "Ripristino non riuscito: ${e.message} L'app verrà riavviata."
            } catch (e: RestoreException) {
                "Ripristino non riuscito: ${e.message}"
            } catch (e: java.io.IOException) {
                "Operazione non riuscita: ${e.message}"
            } catch (e: SecurityException) {
                "Permesso negato: scegli di nuovo la cartella."
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                "Operazione non riuscita: ${e.message ?: "errore sconosciuto"}"
            } finally {
                _busy.value = false
            }
        }
    }
}
