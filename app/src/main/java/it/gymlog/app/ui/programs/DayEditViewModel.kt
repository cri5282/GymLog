package it.gymlog.app.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.RoutineExercise
import it.gymlog.app.data.db.RoutineExerciseWithExercise
import it.gymlog.app.data.repo.ProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayEditViewModel(private val dayId: Long, private val repo: ProgramRepository) : ViewModel() {
    private val _day = MutableStateFlow<RoutineDay?>(null)
    val day: StateFlow<RoutineDay?> = _day.asStateFlow()
    val exercises: StateFlow<List<RoutineExerciseWithExercise>> =
        repo.observeDayExercises(dayId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { _day.value = repo.getDay(dayId) } }

    fun updateDay(transform: (RoutineDay) -> RoutineDay) = viewModelScope.launch {
        val d = _day.value ?: return@launch
        val next = transform(d)
        repo.updateDay(next)
        _day.value = next
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { _day.value?.let { repo.deleteDay(it) }; onDone() }
    fun add(exerciseId: Long) = viewModelScope.launch { repo.addExerciseToDay(dayId, exerciseId) }
    fun update(re: RoutineExercise) = viewModelScope.launch { repo.updateRoutineExercise(re) }
    fun remove(re: RoutineExercise) = viewModelScope.launch { repo.removeRoutineExercise(re) }
    fun move(re: RoutineExercise, up: Boolean) = viewModelScope.launch { repo.moveRoutineExercise(re, up) }
}
