package it.gymlog.app.data.repo

import androidx.room.withTransaction
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Program
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.RoutineExercise
import it.gymlog.app.data.db.RoutineExerciseWithExercise
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ProgramRepository(private val db: AppDatabase) {
    private val dao = db.programDao()

    fun observePrograms(): Flow<List<Program>> = dao.observePrograms()
    fun observeActive(): Flow<Program?> = dao.observeActive()
    suspend fun getProgram(id: Long): Program? = dao.getProgram(id)

    suspend fun create(name: String): Long = dao.insertProgram(Program(name = name.trim()))
    suspend fun rename(id: Long, name: String) {
        dao.getProgram(id)?.let { dao.updateProgram(it.copy(name = name.trim())) }
    }
    suspend fun setActive(id: Long) = dao.setActive(id)
    suspend fun delete(id: Long) {
        dao.getProgram(id)?.let { dao.deleteProgram(it) }
    }

    fun observeDays(programId: Long): Flow<List<RoutineDay>> = dao.observeDays(programId)
    suspend fun getDay(id: Long): RoutineDay? = dao.getDay(id)

    suspend fun addDay(programId: Long, name: String, dayOfWeek: Int?): Long =
        dao.insertDay(RoutineDay(programId = programId, name = name.trim(), dayOfWeek = dayOfWeek,
            sortOrder = dao.getDays(programId).size))

    suspend fun updateDay(day: RoutineDay) = dao.updateDays(listOf(day))
    suspend fun deleteDay(day: RoutineDay) = dao.deleteDay(day)

    suspend fun moveDay(day: RoutineDay, up: Boolean) = db.withTransaction {
        dao.updateDays(moved(dao.getDays(day.programId), { it.id == day.id }, up) { d, i -> d.copy(sortOrder = i) })
    }

    fun observeDayExercises(dayId: Long): Flow<List<RoutineExerciseWithExercise>> = dao.observeDayExercises(dayId)
    suspend fun getDayExercises(dayId: Long): List<RoutineExerciseWithExercise> = dao.getDayExercises(dayId)

    suspend fun addExerciseToDay(dayId: Long, exerciseId: Long): Long =
        dao.insertRoutineExercise(
            RoutineExercise(routineDayId = dayId, exerciseId = exerciseId, sortOrder = dao.getDayExercises(dayId).size)
        )

    suspend fun updateRoutineExercise(re: RoutineExercise) = dao.updateRoutineExercises(listOf(re))
    suspend fun removeRoutineExercise(re: RoutineExercise) = dao.deleteRoutineExercise(re)

    suspend fun moveRoutineExercise(re: RoutineExercise, up: Boolean) = db.withTransaction {
        val list = dao.getDayExercises(re.routineDayId).map { it.routineExercise }
        dao.updateRoutineExercises(moved(list, { it.id == re.id }, up) { r, i -> r.copy(sortOrder = i) })
    }

    /** Assegna [dayOfWeek] alla scheda [routineDayId] (null = Riposo), togliendolo alle altre schede del programma. */
    suspend fun assignDay(programId: Long, dayOfWeek: Int, routineDayId: Long?) = db.withTransaction {
        val days = dao.getDays(programId)
        val updated = days.mapNotNull { d ->
            when {
                d.id == routineDayId -> d.copy(dayOfWeek = dayOfWeek)
                d.dayOfWeek == dayOfWeek -> d.copy(dayOfWeek = null)
                else -> null
            }
        }
        dao.updateDays(updated)
    }

    /** Scheda del programma attivo assegnata al giorno della settimana di [date]. */
    suspend fun todayDay(date: LocalDate): RoutineDay? {
        val active = dao.getActive() ?: return null
        return dao.getDays(active.id).firstOrNull { it.dayOfWeek == date.dayOfWeek.value }
    }
}

/** Sposta di una posizione l'elemento che soddisfa [match] e rinumera l'ordine. */
internal fun <T> moved(list: List<T>, match: (T) -> Boolean, up: Boolean, renumber: (T, Int) -> T): List<T> {
    val i = list.indexOfFirst(match)
    val j = if (up) i - 1 else i + 1
    if (i < 0 || j !in list.indices) return emptyList()
    return list.toMutableList().apply { add(j, removeAt(i)) }.mapIndexed { index, t -> renumber(t, index) }
}
