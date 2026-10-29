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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    /** Lato destro degli esercizi unilaterali (weightKg/reps/rir sono il sinistro); null = riga bilaterale. */
    val weightKgR: Double? = null,
    val repsR: Int? = null,
    val rirR: Int? = null,
) {
    val done: Boolean get() = entry != null
}

data class PageState(val planned: PlannedExercise, val rows: List<RowState>, val stepKg: Double) {
    val single: Boolean get() = planned.exercise.exercise.single
}

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

sealed interface WorkoutEvent {
    data class RowRemoved(val message: String) : WorkoutEvent
}

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

    private val _events = MutableSharedFlow<WorkoutEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WorkoutEvent> = _events.asSharedFlow()

    private data class RemovedRow(val page: Int, val index: Int, val row: RowState, val selectedBefore: Int?)
    private var lastRemoved: RemovedRow? = null
    private var lastRemovalJob: Job? = null

    init {
        viewModelScope.launch {
            workouts.normalizeStaleStart(sessionId)
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
        val single = p.exercise.exercise.single
        val rows = (0 until count).map { i ->
            val row = bySlot[i]?.let {
                RowState(it.weightKg, it.reps, it.rir, it.setType, it.notes, entry = it, weightKgR = it.weightKgR, repsR = it.repsR, rirR = it.rirR)
            } ?: (p.prefill.getOrNull(i) ?: p.prefill.lastOrNull())
                ?.let { RowState(it.weightKg, it.reps, it.rir, it.type, weightKgR = it.weightKgR, repsR = it.repsR, rirR = it.rirR) }
                ?: RowState(0.0, 0, null, SetType.WORKING)
            row.forSide(single)
        }
        val step = p.exercise.exercise.loadStepKg ?: settings.current().defaultLoadStepKg
        return PageState(p, rows, step)
    }

    /** Modifica una riga; se la serie è già salvata, aggiorna subito anche il database. */
    private fun editAndPersist(page: Int, row: Int, transform: (RowState) -> RowState) {
        editRow(page, row, transform)
        val r = _state.value.pages.getOrNull(page)?.rows?.getOrNull(row) ?: return
        val entry = r.entry ?: return
        val updated = entry.copy(
            weightKg = r.weightKg, reps = r.reps, rir = r.rir, setType = r.type, notes = r.notes.trim(),
            weightKgR = r.weightKgR, repsR = r.repsR, rirR = r.rirR,
        )
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

    /** [right] = true modifica il lato destro di una riga unilaterale. */
    fun changeWeight(page: Int, row: Int, steps: Int, right: Boolean = false) {
        val step = _state.value.pages.getOrNull(page)?.stepKg ?: return
        editAndPersist(page, row) {
            if (right) it.copy(weightKgR = max(0.0, (it.weightKgR ?: it.weightKg) + steps * step))
            else it.copy(weightKg = max(0.0, it.weightKg + steps * step))
        }
    }
    fun setWeight(page: Int, row: Int, kg: Double, right: Boolean = false) = editAndPersist(page, row) {
        val v = kg.coerceIn(0.0, 1000.0)
        if (right) it.copy(weightKgR = v) else it.copy(weightKg = v)
    }
    fun changeReps(page: Int, row: Int, delta: Int, right: Boolean = false) = editAndPersist(page, row) {
        if (right) it.copy(repsR = ((it.repsR ?: it.reps) + delta).coerceIn(0, 100))
        else it.copy(reps = (it.reps + delta).coerceIn(0, 100))
    }
    fun setReps(page: Int, row: Int, reps: Int, right: Boolean = false) = editAndPersist(page, row) {
        val v = reps.coerceIn(0, 100)
        if (right) it.copy(repsR = v) else it.copy(reps = v)
    }
    fun setRir(page: Int, row: Int, rir: Int?, right: Boolean = false) = editAndPersist(page, row) {
        val v = rir?.coerceIn(0, 5)
        if (right) it.copy(rirR = v) else it.copy(rir = v)
    }
    fun setType(page: Int, row: Int, type: SetType) = editAndPersist(page, row) { it.copy(type = type) }
    fun setNote(page: Int, row: Int, notes: String) = editAndPersist(page, row) { it.copy(notes = notes) }

    /** Righe in fase di salvataggio: un secondo tocco su ✓ durante il salvataggio viene ignorato. */
    private val saving = mutableSetOf<Pair<Int, Int>>()

    /** Righe la cui serie salvata è in fase di cancellazione: non si può riconfermarle subito. */
    private val deleting = mutableSetOf<Pair<Int, Int>>()

    /** Conferma la serie e avvia il recupero; su una serie già salvata non fa nulla (le modifiche sono già salvate). */
    fun confirm(page: Int, row: Int) {
        val p = _state.value.pages.getOrNull(page) ?: return
        val r = p.rows.getOrNull(row) ?: return
        if (r.done || (page to row) in deleting || !saving.add(page to row)) return
        val draft = SetDraft(r.weightKg, r.reps, r.rir, r.type, r.weightKgR, r.repsR, r.rirR)
        val order = p.planned.order
        val exerciseId = p.planned.exercise.exercise.id
        viewModelScope.launch {
            try {
                val id = workouts.confirmSet(sessionId, exerciseId, order, row, draft, r.notes)
                val entry = workouts.getSet(id)
                editRow(page, row) { it.copy(entry = entry) }
                _state.update { it.copy(selectedRow = it.selectedRow - page) }
                // In superserie il recupero parte solo dopo l'ultimo esercizio del gruppo.
                val group = p.planned.supersetGroup
                // In superserie si riposa solo dopo l'ultimo esercizio del gruppo che ha ancora serie da fare.
                val laterInGroup = group != null && _state.value.pages.any { other ->
                    other.planned.order > order && other.planned.supersetGroup == group &&
                        other.rows.any { !it.done }
                }
                if (!laterInGroup) timer.start(p.planned.restSeconds)
            } finally {
                saving.remove(page to row)
            }
        }
    }

    /** Annulla la conferma di una serie già salvata: la riga torna modificabile e il dato viene rimosso. */
    fun unconfirm(page: Int, row: Int) {
        val p = _state.value.pages.getOrNull(page) ?: return
        val entry = p.rows.getOrNull(row)?.entry ?: return
        if (!deleting.add(page to row)) return
        editRow(page, row) { it.copy(entry = null) }
        viewModelScope.launch {
            try {
                workouts.deleteSet(entry)
            } finally {
                deleting.remove(page to row)
            }
        }
    }

    /** Rimuove una riga dalla tabella e, se la serie era già confermata, anche dal database. */
    fun removeRow(page: Int, row: Int) {
        val p = _state.value.pages.getOrNull(page) ?: return
        if (row !in p.rows.indices || (page to row) in saving) return
        val removed = p.rows[row]
        val remaining = p.rows.filterIndexed { i, _ -> i != row }
        // Gli indici delle serie salvate restano contigui, così una nuova serie non ne duplica uno.
        val rows = remaining.mapIndexed { i, r ->
            r.entry?.let { e -> if (e.setIndex == i) r else r.copy(entry = e.copy(setIndex = i)) } ?: r
        }
        val reindexed = remaining.mapIndexedNotNull { i, r ->
            r.entry?.takeIf { it.setIndex != i }?.copy(setIndex = i)
        }
        val selectedBefore = _state.value.selectedRow[page]
        _state.update { s ->
            val pages = s.pages.toMutableList()
            val current = pages.getOrNull(page) ?: return@update s
            if (row !in current.rows.indices || current.rows.size != p.rows.size) return@update s
            pages[page] = current.copy(rows = rows)
            val selected = s.selectedRow.toMutableMap()
            selected[page]?.let { sel ->
                when {
                    sel == row -> selected[page] = row.coerceAtMost(rows.lastIndex.coerceAtLeast(0))
                    sel > row -> selected[page] = sel - 1
                }
            }
            s.copy(pages = pages, selectedRow = selected)
        }
        lastRemoved = RemovedRow(page, row, removed, selectedBefore = selectedBefore)
        lastRemovalJob = viewModelScope.launch {
            removed.entry?.let { workouts.deleteSet(it) }
            reindexed.forEach { workouts.updateSet(it) }
        }
        _events.tryEmit(WorkoutEvent.RowRemoved("Serie rimossa"))
    }

    /** Ripristina l'ultima serie rimossa, conservando id e posizione. */
    fun undoRemove() {
        val undo = lastRemoved ?: return
        lastRemoved = null
        val removal = lastRemovalJob
        lastRemovalJob = null
        viewModelScope.launch {
            removal?.join()
            val p = _state.value.pages.getOrNull(undo.page) ?: return@launch
            val index = undo.index.coerceIn(0, p.rows.size)
            val inserted = p.rows.toMutableList().apply { add(index, undo.row) }
            val rows = inserted.mapIndexed { i, r ->
                r.entry?.let { e -> if (e.setIndex == i) r else r.copy(entry = e.copy(setIndex = i)) } ?: r
            }
            _state.update { s ->
                val pages = s.pages.toMutableList()
                val current = pages.getOrNull(undo.page) ?: return@update s
                pages[undo.page] = current.copy(rows = rows)
                val selected = s.selectedRow.toMutableMap()
                val before = undo.selectedBefore
                if (before != null) {
                    selected[undo.page] = before
                } else {
                    selected[undo.page]?.let { sel -> if (sel >= index) selected[undo.page] = sel + 1 }
                }
                s.copy(pages = pages, selectedRow = selected)
            }
            undo.row.entry?.let { workouts.restoreSet(it.copy(setIndex = index)) }
            rows.forEach { r -> r.entry?.let { workouts.updateSet(it) } }
        }
    }

    /** Duplica una serie subito sotto, lasciandola non confermata. */
    fun duplicateRow(page: Int, row: Int) {
        val p = _state.value.pages.getOrNull(page) ?: return
        val source = p.rows.getOrNull(row) ?: return
        insertRow(page, row + 1, source.copy(entry = null))
    }

    private fun insertRow(page: Int, index: Int, row: RowState) {
        val p = _state.value.pages.getOrNull(page) ?: return
        val at = index.coerceIn(0, p.rows.size)
        val inserted = p.rows.toMutableList().apply { add(at, row) }
        val rows = inserted.mapIndexed { i, r ->
            r.entry?.let { e -> if (e.setIndex == i) r else r.copy(entry = e.copy(setIndex = i)) } ?: r
        }
        _state.update { s ->
            val pages = s.pages.toMutableList()
            val current = pages.getOrNull(page) ?: return@update s
            pages[page] = current.copy(rows = rows)
            val selected = s.selectedRow.toMutableMap()
            selected[page]?.let { sel -> if (sel >= at) selected[page] = sel + 1 }
            s.copy(pages = pages, selectedRow = selected)
        }
        viewModelScope.launch { rows.forEach { r -> r.entry?.let { workouts.updateSet(it) } } }
    }

    /** Riga espansa della pagina. */
    fun activeRow(page: Int): Int? {
        val s = _state.value
        val p = s.pages.getOrNull(page) ?: return null
        return activeRowIndex(p.rows.map { it.done }, s.selectedRow[page], allowDone = true)
    }

    fun selectRow(page: Int, row: Int) = _state.update { it.copy(selectedRow = it.selectedRow + (page to row)) }

    /** Ricarica i dettagli dell'esercizio dopo averlo modificato dalla schermata di allenamento. */
    fun refreshExercise(page: Int) = viewModelScope.launch {
        val id = _state.value.pages.getOrNull(page)?.planned?.exercise?.exercise?.id ?: return@launch
        val details = exercises.get(id) ?: return@launch
        _state.update { s ->
            val pages = s.pages.toMutableList()
            pages.getOrNull(page)?.let { current ->
                val single = details.exercise.single
                pages[page] = current.copy(
                    planned = current.planned.copy(exercise = details),
                    stepKg = details.exercise.loadStepKg ?: settings.current().defaultLoadStepKg,
                    // Se l'utente ha appena reso l'esercizio unilaterale, prepara subito il lato destro.
                    rows = current.rows.map { it.forSide(single) },
                )
            }
            s.copy(pages = pages)
        }
    }

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
            if (!r.done && r.type == SetType.WORKING) {
                editRow(page, i) { it.copy(weightKg = kg, weightKgR = it.weightKgR?.let { kg }) }
            }
        }
    }

    fun addSet(page: Int) = _state.update { s ->
        val pages = s.pages.toMutableList()
        val p = pages.getOrNull(page) ?: return@update s
        val last = p.rows.lastOrNull()
        val row = RowState(
            last?.weightKg ?: 0.0, last?.reps ?: 0, last?.rir, last?.type ?: SetType.WORKING,
            weightKgR = last?.weightKgR, repsR = last?.repsR, rirR = last?.rirR,
        ).forSide(p.single)
        pages[page] = p.copy(rows = p.rows + row)
        s.copy(pages = pages)
    }

    fun addExercise(exerciseId: Long) = viewModelScope.launch {
        val order = (_state.value.pages.maxOfOrNull { it.planned.order } ?: -1) + 1
        val rest = settings.current().defaultRestSeconds
        val planned = workouts.planSingle(sessionId, exerciseId, order, null, rest) ?: return@launch
        val newPage = page(planned)
        _state.update { it.copy(pages = it.pages + newPage) }
    }

    /** Sostituisce l'esercizio di una pagina solo per oggi, mantenendo target e recupero. */
    fun replaceExercise(page: Int, exerciseId: Long) = viewModelScope.launch {
        val old = _state.value.pages.getOrNull(page) ?: return@launch
        if (old.rows.any { it.done }) return@launch
        val planned = workouts.planSingle(
            sessionId, exerciseId, old.planned.order, old.planned.target, old.planned.restSeconds,
            old.planned.supersetGroup,
        ) ?: return@launch
        val newPage = page(planned)
        _state.update { s -> s.copy(pages = s.pages.toMutableList().also { it[page] = newPage }) }
    }

    fun addRestSeconds(seconds: Int) = timer.addSeconds(seconds)
    fun pauseRest() = timer.pause()
    fun resumeRest() = timer.resume()
    fun setRest(seconds: Int) = timer.start(seconds)

    /** Cambia il recupero del solo esercizio corrente per le prossime serie. */
    fun setExerciseRest(page: Int, seconds: Int) {
        val rest = seconds.coerceIn(5, 1800)
        _state.update { s ->
            val pages = s.pages.toMutableList()
            val current = pages.getOrNull(page) ?: return@update s
            pages[page] = current.copy(planned = current.planned.copy(restSeconds = rest))
            s.copy(pages = pages)
        }
    }

    fun skipRest() = timer.skip()

    fun finish() = viewModelScope.launch {
        timer.skip()
        val recap = workouts.finish(sessionId)
        val pages = _state.value.pages
        val sessionBest = linkedMapOf<String, Double>()
        val previous = mutableMapOf<String, Double?>()
        for (p in pages) {
            val name = p.planned.exercise.exercise.name
            val best = p.rows.filter { it.done && it.type != SetType.WARMUP }
                .maxOfOrNull { maxOf(it.weightKg, it.weightKgR ?: it.weightKg) } ?: continue
            sessionBest[name] = maxOf(best, sessionBest[name] ?: 0.0)
            previous[name] = p.planned.bestKg
        }
        _state.update { it.copy(recap = recap, records = recordsBeaten(sessionBest, previous)) }
    }
}

/** Esercizio unilaterale: il lato destro parte come copia del sinistro; altrimenti una serie non salvata è bilaterale. */
private fun RowState.forSide(single: Boolean): RowState = when {
    single && weightKgR == null -> copy(weightKgR = weightKg, repsR = reps, rirR = rir)
    !single && !done -> copy(weightKgR = null, repsR = null, rirR = null)
    else -> this
}
