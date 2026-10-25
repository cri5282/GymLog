package it.gymlog.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.AsymmetryPoint
import it.gymlog.app.domain.ExerciseProgress
import it.gymlog.app.domain.asymmetryHistory
import it.gymlog.app.domain.exerciseProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

data class ExerciseStatsState(
    val loading: Boolean = true,
    val progress: ExerciseProgress? = null,
    val asymmetry: List<AsymmetryPoint> = emptyList(),
)

class ExerciseStatsViewModel(
    private val exerciseId: Long,
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    val state: StateFlow<ExerciseStatsState> = combine(
        workouts.observeStatSets(), settings.settings,
    ) { sets, s ->
        val mine = sets.filter { it.exerciseId == exerciseId }
        ExerciseStatsState(
            loading = false,
            progress = exerciseProgress(mine, s.oneRmFormula, zone).firstOrNull(),
            asymmetry = asymmetryHistory(mine, zone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseStatsState())
}
