package it.gymlog.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.hevy.HevyFormatException
import it.gymlog.app.data.hevy.HevyImporter
import it.gymlog.app.data.hevy.ImportReport
import it.gymlog.app.data.settings.Accent
import it.gymlog.app.data.settings.OneRmFormula
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
) : ViewModel() {
    val settings: StateFlow<Settings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _import = MutableStateFlow<ImportState>(ImportState.Idle)
    val import: StateFlow<ImportState> = _import.asStateFlow()

    fun setStep(kg: Double) = viewModelScope.launch {
        repo.update { it.copy(defaultLoadStepKg = kg.coerceIn(0.25, 20.0)) }
    }
    fun setFormula(f: OneRmFormula) = viewModelScope.launch { repo.update { it.copy(oneRmFormula = f) } }
    fun setVibration(on: Boolean) = viewModelScope.launch { repo.update { it.copy(timerVibration = on) } }
    fun setSound(on: Boolean) = viewModelScope.launch { repo.update { it.copy(timerSound = on) } }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.update { it.copy(themeMode = mode) } }
    fun setAccent(accent: Accent) = viewModelScope.launch { repo.update { it.copy(accent = accent) } }
    fun setKeepScreenOn(on: Boolean) = viewModelScope.launch { repo.update { it.copy(keepScreenOn = on) } }

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
            }
        }
    }

    fun dismissImport() { _import.value = ImportState.Idle }
}
