package it.gymlog.app.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface StartAction {
    data class Resume(val sessionId: Long) : StartAction
    data class StartDay(val dayId: Long) : StartAction
    data object Pick : StartAction
}

/** Cosa fa il pulsante Start: riprende, avvia la scheda di oggi o chiede quale scheda fare. */
fun startAction(unfinished: WorkoutSession?, today: RoutineDay?): StartAction = when {
    unfinished != null -> StartAction.Resume(unfinished.id)
    today != null -> StartAction.StartDay(today.id)
    else -> StartAction.Pick
}

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModel(
    private val programs: ProgramRepository,
    private val workouts: WorkoutRepository,
    private val date: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val unfinished: StateFlow<WorkoutSession?> =
        workouts.observeUnfinished().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val resuming: StateFlow<Boolean> = unfinished.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val days: StateFlow<List<RoutineDay>> = programs.observeActive()
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else programs.observeDays(p.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var starting = false

    fun onStart(open: (Long) -> Unit, pick: () -> Unit) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            try {
                when (val a = startAction(workouts.getUnfinished(), programs.todayDay(date()))) {
                    is StartAction.Resume -> open(a.sessionId)
                    is StartAction.StartDay -> open(workouts.startOrResume(a.dayId))
                    StartAction.Pick -> pick()
                }
            } finally { starting = false }
        }
    }

    /** Avvia la scheda scelta (null = allenamento libero); ignora i tocchi ripetuti. */
    fun startDay(dayId: Long?, open: (Long) -> Unit) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            try { open(workouts.startOrResume(dayId)) } finally { starting = false }
        }
    }
}
