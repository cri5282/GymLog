package it.gymlog.app.data.repo

import androidx.room.withTransaction
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.ExerciseWithDetails
import it.gymlog.app.data.db.SessionSummary
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetWithExercise
import it.gymlog.app.data.db.SetWithSession
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.db.StatSet
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.domain.Target
import it.gymlog.app.domain.prefill
import it.gymlog.app.domain.suggestLoad
import it.gymlog.app.domain.warmupSets
import kotlinx.coroutines.flow.Flow

data class PlannedExercise(
    val exercise: ExerciseWithDetails,
    /** Posizione nella sessione (= exerciseOrder delle serie). */
    val order: Int,
    /** null per gli esercizi aggiunti solo oggi o negli allenamenti liberi. */
    val target: Target?,
    val restSeconds: Int,
    val prefill: List<SetDraft>,
    val suggestedKg: Double?,
    val lastTime: List<SetDraft>,
    /** Serie già confermate in questa sessione. */
    val done: List<SetEntry>,
    /** Carico massimo nelle sessioni concluse (record prima di oggi). */
    val bestKg: Double? = null,
    /** Numero di superserie nella scheda (null = esercizio singolo). */
    val supersetGroup: Int? = null,
)

data class SessionRecap(val durationMin: Long, val workingSets: Int, val volumeKg: Double)

const val DEFAULT_REST_SECONDS = 120
const val FREE_WORKOUT_TITLE = "Allenamento libero"

fun SetEntry.toDraft() = SetDraft(weightKg, reps, rir, setType, weightKgR, repsR, rirR)

class WorkoutRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.workoutDao()

    fun observeUnfinished(): Flow<WorkoutSession?> = dao.observeUnfinished()
    suspend fun getUnfinished(): WorkoutSession? = dao.getUnfinished()

    /** Riprende l'allenamento in corso se esiste, altrimenti ne inizia uno nuovo. */
    suspend fun startOrResume(dayId: Long?): Long = db.withTransaction {
        dao.getUnfinished()?.id ?: run {
            val title = dayId?.let { db.programDao().getDay(it)?.name } ?: FREE_WORKOUT_TITLE
            dao.insertSession(WorkoutSession(routineDayId = dayId, title = title, startedAt = clock()))
        }
    }

    suspend fun getSession(id: Long): WorkoutSession? = dao.getSession(id)

    /**
     * Se un allenamento è rimasto aperto da molte ore (tipicamente da un giorno precedente),
     * sposta l'inizio a ora: le serie già fatte restano, ma la durata non conta i giorni di pausa.
     */
    suspend fun normalizeStaleStart(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        if (session.endedAt != null) return
        val now = clock()
        if (now - session.startedAt > 6 * 60 * 60 * 1000L) {
            dao.updateSession(session.copy(startedAt = now))
        }
    }
    fun observeSession(id: Long): Flow<WorkoutSession?> = dao.observeSession(id)
    fun observeSets(sessionId: Long): Flow<List<SetWithExercise>> = dao.observeSets(sessionId)
    fun observeFinishedSessions(): Flow<List<SessionSummary>> = dao.observeFinishedSessions()
    fun observeFinishedSessionTimes(): Flow<List<SessionTime>> = dao.observeFinishedSessionTimes()
    fun countWorkingSetsSince(fromMillis: Long): Flow<Int> = dao.countWorkingSetsSince(fromMillis)
    suspend fun bestWeight(exerciseId: Long): Double? = dao.bestWeight(exerciseId)
    fun historyFor(exerciseId: Long): Flow<List<SetWithSession>> = dao.historyFor(exerciseId)

    /** Esercizi della scheda + esercizi con serie in questa sessione non previsti + [extraExerciseIds]. */
    suspend fun plan(sessionId: Long, extraExerciseIds: List<Long> = emptyList()): List<PlannedExercise> {
        val session = dao.getSession(sessionId) ?: return emptyList()
        val current = settings.current()
        val step = current.defaultLoadStepKg
        val autoWarmup = current.autoWarmup
        val done = dao.getSets(sessionId)
        val dayExercises = session.routineDayId?.let { db.programDao().getDayExercises(it) }.orEmpty()

        val groups = done.groupBy { it.exerciseOrder to it.exerciseId }
        val used = mutableSetOf<Pair<Int, Long>>()
        val result = mutableListOf<PlannedExercise>()
        dayExercises.forEachIndexed { i, re ->
            val r = re.routineExercise
            val target = Target(r.targetSets, r.repMin, r.repMax, r.targetRirMin, r.targetRirMax)
            val original = i to r.exerciseId
            // Se la posizione non ha serie dell'esercizio previsto ma di un altro, è stato sostituito solo per oggi.
            val substitute = groups.keys.firstOrNull { it.first == i && it != original && it !in used }
            if (original !in groups && substitute != null) {
                val details = db.exerciseDao().getWithDetails(substitute.second)
                if (details != null) {
                    used += substitute
                    result += planned(
                        sessionId, details, i, target, r.restSeconds, step, groups.getValue(substitute),
                        supersetGroup = r.supersetGroup, autoWarmup = autoWarmup,
                    )
                    return@forEachIndexed
                }
            }
            used += original
            result += planned(
                sessionId, re.exercise, i, target, r.restSeconds, step, groups[original].orEmpty(),
                supersetGroup = r.supersetGroup, autoWarmup = autoWarmup,
            )
        }
        for ((key, sets) in groups.entries.filter { it.key !in used }.sortedBy { it.key.first }) {
            val details = db.exerciseDao().getWithDetails(key.second) ?: continue
            result += planned(sessionId, details, key.first, null, DEFAULT_REST_SECONDS, step, sets, autoWarmup = autoWarmup)
        }
        var nextOrder = (result.maxOfOrNull { it.order } ?: -1) + 1
        for (id in extraExerciseIds) {
            val details = db.exerciseDao().getWithDetails(id) ?: continue
            result += planned(sessionId, details, nextOrder++, null, DEFAULT_REST_SECONDS, step, emptyList(), autoWarmup = autoWarmup)
        }
        return result
    }

    /** Pianifica un singolo esercizio aggiunto o sostituito solo per questa sessione. */
    suspend fun planSingle(
        sessionId: Long, exerciseId: Long, order: Int, target: Target?, restSeconds: Int,
        supersetGroup: Int? = null,
    ): PlannedExercise? {
        val details = db.exerciseDao().getWithDetails(exerciseId) ?: return null
        val current = settings.current()
        val done = dao.getSets(sessionId).filter { it.exerciseOrder == order && it.exerciseId == exerciseId }
        return planned(
            sessionId, details, order, target, restSeconds, current.defaultLoadStepKg, done,
            supersetGroup = supersetGroup, autoWarmup = current.autoWarmup,
        )
    }

    private suspend fun planned(
        sessionId: Long, exercise: ExerciseWithDetails, order: Int, target: Target?, rest: Int,
        defaultStep: Double, done: List<SetEntry>, supersetGroup: Int? = null, autoWarmup: Boolean = true,
    ): PlannedExercise {
        val last = dao.lastFinishedSetsFor(exercise.exercise.id, sessionId).map { it.toDraft() }
        val working = last.filter { it.type != SetType.WARMUP }
        val targetSets = target?.sets ?: working.count { it.type == SetType.WORKING }.takeIf { it > 0 } ?: 3
        val step = exercise.exercise.loadStepKg ?: defaultStep
        val top = working.filter { it.type == SetType.WORKING }
            .maxOfOrNull { maxOf(it.weightKg, it.weightKgR ?: it.weightKg) } ?: 0.0
        // Le serie di riscaldamento si ricalcolano dai carichi dell'ultima volta a ogni nuovo allenamento.
        val warmups = if (autoWarmup && working.isNotEmpty()) warmupSets(top, step, exercise.exercise.single) else emptyList()
        return PlannedExercise(
            exercise = exercise, order = order, target = target, restSeconds = rest,
            prefill = warmups + prefill(working, targetSets),
            suggestedKg = target?.let { suggestLoad(working, it, step) },
            lastTime = working, done = done.sortedBy { it.setIndex },
            bestKg = dao.bestWeight(exercise.exercise.id),
            supersetGroup = supersetGroup,
        )
    }

    suspend fun confirmSet(
        sessionId: Long, exerciseId: Long, exerciseOrder: Int, setIndex: Int, draft: SetDraft, notes: String,
    ): Long = dao.insertSet(
        SetEntry(
            sessionId = sessionId, exerciseId = exerciseId, exerciseOrder = exerciseOrder, setIndex = setIndex,
            setType = draft.type, weightKg = draft.weightKg, reps = draft.reps, rir = draft.rir,
            weightKgR = draft.weightKgR, repsR = draft.repsR, rirR = draft.rirR,
            notes = notes.trim(), completedAt = clock(),
        )
    )

    suspend fun getSet(id: Long): SetEntry? = dao.getSet(id)
    suspend fun updateSet(set: SetEntry) = dao.updateSet(set)
    suspend fun deleteSet(set: SetEntry) = dao.deleteSet(set)
    suspend fun deleteSession(session: WorkoutSession) = dao.deleteSession(session)

    fun observeStatSets(): Flow<List<StatSet>> = dao.observeStatSets()

    /**
     * Chiude un allenamento rimasto aperto: fine = ultima serie confermata. Se non è stata
     * registrata alcuna serie, l'allenamento viene eliminato invece di lasciare una sessione vuota.
     */
    suspend fun closeOpenSession(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        if (session.endedAt != null) return
        val end = dao.lastCompletedAt(sessionId)
        if (end == null) {
            dao.deleteSession(session)
        } else {
            dao.updateSession(session.copy(endedAt = maxOf(end, session.startedAt)))
        }
    }

    suspend fun finish(sessionId: Long): SessionRecap {
        val session = dao.getSession(sessionId) ?: return SessionRecap(0, 0, 0.0)
        val end = clock()
        dao.updateSession(session.copy(endedAt = end))
        val counted = dao.getSets(sessionId).filter { it.setType != SetType.WARMUP }
        return SessionRecap(
            durationMin = (end - session.startedAt).coerceAtLeast(0) / 60_000,
            workingSets = counted.size,
            volumeKg = counted.sumOf { it.weightKg * it.reps + (it.weightKgR ?: 0.0) * (it.repsR ?: 0) },
        )
    }
}
