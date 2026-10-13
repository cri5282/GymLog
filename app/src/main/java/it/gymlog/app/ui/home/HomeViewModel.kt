package it.gymlog.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.domain.DayMarker
import it.gymlog.app.domain.localDate
import it.gymlog.app.domain.weekMarkers
import it.gymlog.app.domain.weekStart
import it.gymlog.app.domain.weekStreak
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeState(
    val loading: Boolean = true,
    val weekStart: LocalDate = LocalDate.MIN,
    val days: List<LocalDate> = emptyList(),
    val markers: List<DayMarker> = emptyList(),
    val selected: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val selectedRoutine: RoutineDay? = null,
    val selectedExerciseCount: Int = 0,
    /** Allenamento concluso nel giorno selezionato (il più recente). */
    val selectedSession: SessionTime? = null,
    val unfinished: WorkoutSession? = null,
    val streak: Int = 0,
    val thisWeekDone: Int = 0,
    val plannedPerWeek: Int = 0,
    val totalWorkouts: Int = 0,
    val last: SessionTime? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val programs: ProgramRepository,
    private val workouts: WorkoutRepository,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val shownWeek = MutableStateFlow(weekStart(today()))
    private val selected = MutableStateFlow(today())

    private val days = programs.observeActive()
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else programs.observeDays(p.id) }

    val state: StateFlow<HomeState> = combine(
        days, workouts.observeFinishedSessionTimes(), workouts.observeUnfinished(), shownWeek, selected,
    ) { routineDays, sessions, unfinished, week, sel -> Inputs(routineDays, sessions, unfinished, week, sel) }
        .mapLatest(::build)
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    private data class Inputs(
        val routineDays: List<RoutineDay>, val sessions: List<SessionTime>, val unfinished: WorkoutSession?,
        val week: LocalDate, val selected: LocalDate,
    )

    private suspend fun build(i: Inputs): HomeState {
        val now = today()
        val dates = i.sessions.map { localDate(it.startedAt, zone) }.toSet()
        val planned = i.routineDays.mapNotNull { it.dayOfWeek }.toSet()
        val routine = i.routineDays.firstOrNull { it.dayOfWeek == i.selected.dayOfWeek.value }
        val currentWeek = weekStart(now)
        return HomeState(
            loading = false,
            weekStart = i.week,
            days = (0L until 7L).map { i.week.plusDays(it) },
            markers = weekMarkers(i.week, now, dates, planned),
            selected = i.selected,
            today = now,
            selectedRoutine = routine,
            selectedExerciseCount = routine?.let { programs.getDayExercises(it.id).size } ?: 0,
            selectedSession = i.sessions.firstOrNull { localDate(it.startedAt, zone) == i.selected },
            unfinished = i.unfinished,
            streak = weekStreak(dates, now),
            thisWeekDone = i.sessions.count { !localDate(it.startedAt, zone).isBefore(currentWeek) },
            plannedPerWeek = planned.size,
            totalWorkouts = i.sessions.size,
            last = i.sessions.firstOrNull(),
        )
    }

    fun previousWeek() = shownWeek.update { it.minusWeeks(1) }
    fun nextWeek() = shownWeek.update { it.plusWeeks(1) }
    fun select(date: LocalDate) { selected.value = date }

    private var starting = false

    /** Avvia o riprende un allenamento; ignora i tocchi ripetuti. */
    fun start(dayId: Long?, open: (Long) -> Unit) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            try { open(workouts.startOrResume(dayId)) } finally { starting = false }
        }
    }
}
