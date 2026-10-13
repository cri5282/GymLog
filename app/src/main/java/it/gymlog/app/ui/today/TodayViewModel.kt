package it.gymlog.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.Program
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayState(
    val program: Program? = null,
    val today: RoutineDay? = null,
    val otherDays: List<RoutineDay> = emptyList(),
    val unfinished: WorkoutSession? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    programs: ProgramRepository,
    private val workouts: WorkoutRepository,
    date: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val state: StateFlow<TodayState> = combine(
        programs.observeActive().flatMapLatest { p ->
            if (p == null) flowOf(null to emptyList()) else programs.observeDays(p.id).let { days ->
                combine(flowOf(p), days) { prog, d -> prog to d }
            }
        },
        workouts.observeUnfinished(),
    ) { (program, days), unfinished ->
        val dow = date().dayOfWeek.value
        val today = days.firstOrNull { it.dayOfWeek == dow }
        TodayState(program as Program?, today, days.filter { it != today }, unfinished)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    fun start(dayId: Long?, onStarted: (Long) -> Unit) = viewModelScope.launch {
        onStarted(workouts.startOrResume(dayId))
    }
}
