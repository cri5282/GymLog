package it.gymlog.app.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.BodyEntry
import it.gymlog.app.data.repo.BodyRepository
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.bodyFatEstimates
import it.gymlog.app.domain.ffmi
import it.gymlog.app.domain.ffmiNormalized
import it.gymlog.app.domain.weightMovingAverage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BodyState(
    val loading: Boolean = true,
    val entries: List<BodyEntry> = emptyList(),
    val latest: BodyEntry? = null,
    val movingAverage: List<Pair<LocalDate, Double>> = emptyList(),
    val estimates: Map<String, Double> = emptyMap(),
    val ffmi: Double? = null,
    val ffmiNormalized: Double? = null,
    val settings: Settings = Settings(),
)

class BodyViewModel(
    private val repo: BodyRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<BodyState> = combine(repo.observeAll(), settingsRepo.settings) { entries, settings ->
        val sorted = entries.sortedBy { it.epochDay }
        val latest = sorted.lastOrNull()
        val estimates = latest?.let { bodyFatEstimates(it, settings.heightCm, settings.birthYear, settings.sex) }.orEmpty()
        val fat = estimates["JP7"] ?: estimates["JP3"] ?: estimates["Navy"]
        BodyState(
            loading = false,
            entries = entries,
            latest = latest,
            movingAverage = weightMovingAverage(sorted),
            estimates = estimates,
            ffmi = ffmi(latest?.weightKg, settings.heightCm, fat),
            ffmiNormalized = ffmiNormalized(latest?.weightKg, settings.heightCm, fat),
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyState())

    fun save(entry: BodyEntry) = viewModelScope.launch { repo.save(entry) }
    fun delete(entry: BodyEntry) = viewModelScope.launch { repo.delete(entry) }
}
