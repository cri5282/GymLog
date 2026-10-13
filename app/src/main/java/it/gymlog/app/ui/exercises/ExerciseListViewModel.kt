package it.gymlog.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.ExerciseWithDetails
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.repo.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModel(repo: ExerciseRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val muscle = MutableStateFlow<Muscle?>(null)

    val exercises: StateFlow<List<ExerciseWithDetails>> =
        combine(query, muscle) { q, m -> q to m }
            .flatMapLatest { (q, m) -> repo.observeLibrary(q, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
