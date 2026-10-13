package it.gymlog.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetWithSession
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.repo.ExerciseRepository
import it.gymlog.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(workouts: WorkoutRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionTime>> =
        workouts.observeFinishedSessionTimes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ExerciseGroup(val exercise: Exercise, val sets: List<SetEntry>)
data class SessionDetailState(val session: WorkoutSession? = null, val groups: List<ExerciseGroup> = emptyList())

class SessionDetailViewModel(private val sessionId: Long, private val workouts: WorkoutRepository) : ViewModel() {
    val state: StateFlow<SessionDetailState> = combine(
        workouts.observeSession(sessionId), workouts.observeSets(sessionId),
    ) { session, sets ->
        SessionDetailState(
            session,
            sets.groupBy { it.set.exerciseOrder to it.exercise.id }.entries.sortedBy { it.key.first }
                .map { (_, list) -> ExerciseGroup(list.first().exercise, list.map { it.set }.sortedBy { it.setIndex }) },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionDetailState())

    fun updateSet(set: SetEntry) = viewModelScope.launch { workouts.updateSet(set) }
    fun deleteSet(set: SetEntry) = viewModelScope.launch { workouts.deleteSet(set) }
    fun deleteSession(onDone: () -> Unit) = viewModelScope.launch {
        workouts.getSession(sessionId)?.let { workouts.deleteSession(it) }
        onDone()
    }
}

data class HistoryDay(val session: WorkoutSession, val sets: List<SetEntry>)

class ExerciseHistoryViewModel(exerciseId: Long, workouts: WorkoutRepository, exercises: ExerciseRepository) : ViewModel() {
    val name: StateFlow<String> = exercises.observe(exerciseId).map { it?.exercise?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val days: StateFlow<List<HistoryDay>> = workouts.historyFor(exerciseId).map { rows: List<SetWithSession> ->
        rows.groupBy { it.session.id }.values.map { g -> HistoryDay(g.first().session, g.map { it.set }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
