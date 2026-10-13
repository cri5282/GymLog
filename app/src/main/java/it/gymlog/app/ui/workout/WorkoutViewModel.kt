package it.gymlog.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.repo.ExerciseRepository
import it.gymlog.app.data.repo.PlannedExercise
import it.gymlog.app.data.repo.SessionRecap
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.domain.activeRowIndex
import it.gymlog.app.domain.recordsBeaten
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class RowState(
    val weightKg: Double,
    val reps: Int,
    val rir: Int?,
    val type: SetType,
    val notes: String = "",
    /** Serie salvata nel database; null finché non viene confermata con ✓. */
    val entry: SetEntry? = null,
) {
    val done: Boolean get() = entry != null
}

data class PageState(val planned: PlannedExercise, val rows: List<RowState>, val stepKg: Double)

data class WorkoutUiState(
    val loading: Boolean = true,
    val title: String = "",
    val startedAt: Long = 0,
    val pages: List<PageState> = emptyList(),
    val recap: SessionRecap? = null,
    /** Esercizi con un nuovo record di carico, calcolati alla fine. */
    val records: List<String> = emptyList(),
    /** Riga scelta a mano per pagina (pagina → riga). */
    val selectedRow: Map<Int, Int> = emptyMap(),
)

class WorkoutViewModel(
    private val sessionId: Long,
    private val workouts: WorkoutRepository,
    private val settings: SettingsRepository,
    private val timer: RestTimer,
    private val exercises: ExerciseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()
    val timerState: StateFlow<TimerState> get() = timer.state

    init {
        viewModelScope.launch {
            val session = workouts.getSession(sessionId)
            val pages = workouts.plan(sessionId).map { page(it) }
            _state.value = WorkoutUiState(
                loading = false, title = session?.title.orEmpty(), startedAt = session?.startedAt ?: 0, pages = pages,
            )
        }
    }

    private suspend fun page(p: PlannedExercise): PageState {
        // Una riga per posizione di serie (setIndex): le serie confermate restano al loro posto anche se fatte fuori ordine.
        val bySlot = p.done.associateBy { it.setIndex }
        val count = maxOf(p.prefill.size, (bySlot.keys.maxOrNull() ?: -1) + 1)
        val rows = (0 until count).map { i ->
            bySlot[i]?.let { RowState(it.weightKg, it.reps, it.rir, it.setType, it.notes, entry = it) }
                ?: (p.prefill.getOrNull(i) ?: p.prefill.lastOrNull())
                    ?.let { RowState(it.weightKg, it.reps, it.rir, it.type) }
                ?: RowState(0.0, 0, null, SetType.WORKING)
        }
        val step = p.exercise.exercise.loadStepKg ?: settings.current().defaultLoadStepKg
        return PageState(p, rows, step)
    }

    /** Modifica una riga; se la serie è già salvata, aggiorna subito anche il database. */
    private fun editAndPersist(page: Int, row: Int, transform: (RowState) -> RowState) {
        editRow(page, row, transform)
        val r = _state.value.pages.getOrNull(page)?.rows?.getOrNull(row) ?: return
        val entry = r.entry ?: return
        val updated = entry.copy(weightKg = r.weightKg, reps = r.reps, rir = r.rir, setType = r.type, notes = r.notes.trim())
        if (updated == entry) return
        editRow(page, row) { it.copy(entry = updated) }
        viewModelScope.launch { workouts.updateSet(updated) }
    }

    private fun editRow(page: Int, row: Int, transform: (RowState) -> RowState) = _state.update { s ->
        val pages = s.pages.toMutableList()
        val p = pages.getOrNull(page) ?: return@update s
        val rows = p.rows.toMutableList()
        rows.getOrNull(row)?.let { rows[row] = transform(it) } ?: return@update s
        pages[page] = p.copy(rows = rows)
        s.copy(pages = pages)
    }

    fun changeWeight(page: Int, row: Int, steps: Int) {
        val step = _state.value.pages.getOrNull(page)?.stepKg ?: return
        editAndPersist(page, row) { it.copy(weightKg = max(0.0, it.weightKg + steps * step)) }
    }
    fun setWeight(page: Int, row: Int, kg: Double) = editAndPersist(page, row) { it.copy(weightKg = kg.coerceIn(0.0, 1000.0)) }
    fun changeReps(page: Int, row: Int, delta: Int) = editAndPersist(page, row) { it.copy(reps = (it.reps + delta).coerceIn(0, 100)) }
    fun setReps(page: Int, row: Int, reps: Int) = editAndPersist(page, row) { it.copy(reps = reps.coerceIn(0, 100)) }
    fun setRir(page: Int, row: Int, rir: Int?) = editAndPersist(page, row) { it.copy(rir = rir?.coerceIn(0, 5)) }
    fun setType(page: Int, row: Int, type: SetType) = editAndPersist(page, row) { it.copy(type = type) }
    fun setNote(page: Int, row: Int, notes: String) = editAndPersist(page, row) { it.copy(notes = notes) }

    /** Righe in fase di salvataggio: un secondo tocco su ✓ durante il salvataggio viene ignorato. */
    private val saving = mutableSetOf<Pair<Int, Int>>()

    /** Conferma la serie e avvia il recupero; su una serie già salvata non fa nulla (le modifiche sono già salvate). */
    fun confirm(page: Int, row: Int) {
        val p = _state.value.pages.getOrNull(page) ?: return
        val r = p.rows.getOrNull(row) ?: return
        if (r.done || !saving.add(page to row)) return
        val draft = SetDraft(r.weightKg, r.reps, r.rir, r.type)
        val order = p.planned.order
        val exerciseId = p.planned.exercise.exercise.id
        viewModelScope.launch {
            try {
                val id = workouts.confirmSet(sessionId, exerciseId, order, row, draft, r.notes)
                val entry = workouts.getSet(id)
                editRow(page, row) { it.copy(entry = entry) }
                _state.update { it.copy(selectedRow = it.selectedRow - page) }
                timer.start(p.planned.restSeconds)
            } finally {
                saving.remove(page to row)
            }
        }
    }

    /** Riga espansa della pagina. */
    fun activeRow(page: Int): Int? {
        val s = _state.value
        val p = s.pages.getOrNull(page) ?: return null
        return activeRowIndex(p.rows.map { it.done }, s.selectedRow[page], allowDone = true)
    }

    fun selectRow(page: Int, row: Int) = _state.update { it.copy(selectedRow = it.selectedRow + (page to row)) }

    /** Aggiunge una foto all'esercizio della pagina (scattata in palestra) e la mostra subito. */
    fun addPhoto(page: Int, file: java.io.File) = viewModelScope.launch {
        val p = _state.value.pages.getOrNull(page) ?: return@launch
        val id = p.planned.exercise.exercise.id
        exercises.addPhoto(id, file)
        val details = exercises.get(id) ?: return@launch
        _state.update { s ->
            val pages = s.pages.toMutableList()
            pages.getOrNull(page)?.let { pages[page] = it.copy(planned = it.planned.copy(exercise = details)) }
            s.copy(pages = pages)
        }
    }

    fun applySuggestion(page: Int) {
        val kg = _state.value.pages.getOrNull(page)?.planned?.suggestedKg ?: return
        _state.value.pages[page].rows.forEachIndexed { i, r ->
            if (!r.done && r.type == SetType.WORKING) editRow(page, i) { it.copy(weightKg = kg) }
        }
    }

    fun addSet(page: Int) = _state.update { s ->
        val pages = s.pages.toMutableList()
        val p = pages.getOrNull(page) ?: return@update s
        val last = p.rows.lastOrNull()
        val row = RowState(last?.weightKg ?: 0.0, last?.reps ?: 0, last?.rir, last?.type ?: SetType.WORKING)
        pages[page] = p.copy(rows = p.rows + row)
        s.copy(pages = pages)
    }

    fun addExercise(exerciseId: Long) = viewModelScope.launch {
        val order = (_state.value.pages.maxOfOrNull { it.planned.order } ?: -1) + 1
        val planned = workouts.planSingle(sessionId, exerciseId, order, null, it.gymlog.app.data.repo.DEFAULT_REST_SECONDS) ?: return@launch
        val newPage = page(planned)
        _state.update { it.copy(pages = it.pages + newPage) }
    }

    /** Sostituisce l'esercizio di una pagina solo per oggi, mantenendo target e recupero. */
    fun replaceExercise(page: Int, exerciseId: Long) = viewModelScope.launch {
        val old = _state.value.pages.getOrNull(page) ?: return@launch
        if (old.rows.any { it.done }) return@launch
        val planned = workouts.planSingle(sessionId, exerciseId, old.planned.order, old.planned.target, old.planned.restSeconds)
            ?: return@launch
        val newPage = page(planned)
        _state.update { s -> s.copy(pages = s.pages.toMutableList().also { it[page] = newPage }) }
    }

    fun addRestSeconds(seconds: Int) = timer.addSeconds(seconds)
    fun skipRest() = timer.skip()

    fun finish() = viewModelScope.launch {
        timer.skip()
        val recap = workouts.finish(sessionId)
        val pages = _state.value.pages
        val sessionBest = linkedMapOf<String, Double>()
        val previous = mutableMapOf<String, Double?>()
        for (p in pages) {
            val name = p.planned.exercise.exercise.name
            val best = p.rows.filter { it.done && it.type != SetType.WARMUP }.maxOfOrNull { it.weightKg } ?: continue
            sessionBest[name] = maxOf(best, sessionBest[name] ?: 0.0)
            previous[name] = p.planned.bestKg
        }
        _state.update { it.copy(recap = recap, records = recordsBeaten(sessionBest, previous)) }
    }
}
