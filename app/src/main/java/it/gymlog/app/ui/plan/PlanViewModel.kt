package it.gymlog.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.Program
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.repo.ProgramRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineSummary(val day: RoutineDay, val exerciseCount: Int)

data class PlanState(
    val program: Program? = null,
    val programs: List<Program> = emptyList(),
    /** 7 voci, indice 0 = lunedì; null = riposo. */
    val week: List<RoutineDay?> = List(7) { null },
    val routines: List<RoutineSummary> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(private val repo: ProgramRepository) : ViewModel() {
    // Giorni ricavati dallo stesso flusso dei programmi: programma attivo e settimana restano sempre coerenti.
    val state: StateFlow<PlanState> = repo.observePrograms().flatMapLatest { programs ->
        val active = programs.firstOrNull { it.isActive }
        val days = if (active == null) flowOf(emptyList()) else repo.observeDays(active.id)
        combine(days, repo.observeExerciseCounts()) { routineDays, counts ->
            PlanState(
                program = active,
                programs = programs,
                week = (1..7).map { dow -> routineDays.firstOrNull { it.dayOfWeek == dow } },
                routines = routineDays.map { RoutineSummary(it, counts[it.id] ?: 0) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlanState())

    fun assign(dayOfWeek: Int, routineDayId: Long?) = viewModelScope.launch {
        state.value.program?.let { repo.assignDay(it.id, dayOfWeek, routineDayId) }
    }

    fun createRoutine(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val p = state.value.program ?: return@launch
        onCreated(repo.addDay(p.id, name, null))
    }

    fun selectProgram(id: Long) = viewModelScope.launch { repo.setActive(id) }

    /** Crea un programma e lo rende attivo. */
    fun createProgram(name: String) = viewModelScope.launch { repo.setActive(repo.create(name)) }

    fun renameProgram(id: Long, name: String) = viewModelScope.launch { repo.rename(id, name) }

    fun deleteProgram(id: Long) = viewModelScope.launch {
        val wasActive = state.value.program?.id == id
        repo.delete(id)
        if (wasActive) state.value.programs.firstOrNull { it.id != id }?.let { repo.setActive(it.id) }
    }
}
