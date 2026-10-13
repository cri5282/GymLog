package it.gymlog.app.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.Program
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.repo.ProgramRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgramsViewModel(private val repo: ProgramRepository) : ViewModel() {
    val programs: StateFlow<List<Program>> =
        repo.observePrograms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch { onCreated(repo.create(name)) }
}

class ProgramEditViewModel(private val programId: Long, private val repo: ProgramRepository) : ViewModel() {
    val program: StateFlow<Program?> = repo.observePrograms().map { list -> list.firstOrNull { it.id == programId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val days: StateFlow<List<RoutineDay>> =
        repo.observeDays(programId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(name: String) = viewModelScope.launch { repo.rename(programId, name) }
    fun setActive() = viewModelScope.launch { repo.setActive(programId) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(programId); onDone() }
    fun addDay(name: String, onCreated: (Long) -> Unit) =
        viewModelScope.launch { onCreated(repo.addDay(programId, name, null)) }
    fun moveDay(day: RoutineDay, up: Boolean) = viewModelScope.launch { repo.moveDay(day, up) }
}
