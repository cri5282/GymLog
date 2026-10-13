package it.gymlog.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM Exercise WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ExerciseWithDetails>>

    @Transaction
    @Query("SELECT * FROM Exercise WHERE id = :id")
    suspend fun getWithDetails(id: Long): ExerciseWithDetails?

    @Transaction
    @Query("SELECT * FROM Exercise WHERE id = :id")
    fun observeWithDetails(id: Long): Flow<ExerciseWithDetails?>

    @Query("SELECT * FROM Exercise WHERE hevyName = :name LIMIT 1")
    suspend fun findByHevyName(name: String): Exercise?

    @Query("SELECT * FROM Exercise WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Exercise?

    @Query("SELECT COUNT(*) FROM Exercise")
    suspend fun count(): Int

    @Insert suspend fun insert(e: Exercise): Long
    @Update suspend fun update(e: Exercise)
    @Delete suspend fun delete(e: Exercise)

    @Query("DELETE FROM ExerciseSecondaryMuscle WHERE exerciseId = :exerciseId")
    suspend fun clearSecondary(exerciseId: Long)

    @Insert suspend fun insertSecondary(rows: List<ExerciseSecondaryMuscle>)

    @Transaction
    suspend fun replaceSecondary(exerciseId: Long, muscles: List<Muscle>) {
        clearSecondary(exerciseId)
        insertSecondary(muscles.distinct().map { ExerciseSecondaryMuscle(exerciseId, it) })
    }

    @Insert suspend fun insertPhoto(p: ExercisePhoto): Long
    @Delete suspend fun deletePhoto(p: ExercisePhoto)
    @Update suspend fun updatePhotos(photos: List<ExercisePhoto>)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM SetEntry WHERE exerciseId = :exerciseId) " +
            "OR EXISTS(SELECT 1 FROM RoutineExercise WHERE exerciseId = :exerciseId)"
    )
    suspend fun isUsed(exerciseId: Long): Boolean
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM Program ORDER BY id")
    fun observePrograms(): Flow<List<Program>>

    @Query("SELECT * FROM Program WHERE id = :id")
    suspend fun getProgram(id: Long): Program?

    @Query("SELECT * FROM Program WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): Program?

    @Query("SELECT * FROM Program WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<Program?>

    @Query("UPDATE Program SET isActive = (id = :programId)")
    suspend fun setActive(programId: Long)

    @Query("SELECT COUNT(*) FROM Program")
    suspend fun countPrograms(): Int

    @Insert suspend fun insertProgram(p: Program): Long
    @Update suspend fun updateProgram(p: Program)
    @Delete suspend fun deleteProgram(p: Program)

    @Query("SELECT * FROM RoutineDay WHERE programId = :programId ORDER BY sortOrder, id")
    fun observeDays(programId: Long): Flow<List<RoutineDay>>

    @Query("SELECT * FROM RoutineDay WHERE programId = :programId ORDER BY sortOrder, id")
    suspend fun getDays(programId: Long): List<RoutineDay>

    @Query("SELECT * FROM RoutineDay WHERE id = :id")
    suspend fun getDay(id: Long): RoutineDay?

    @Insert suspend fun insertDay(d: RoutineDay): Long
    @Update suspend fun updateDays(days: List<RoutineDay>)
    @Delete suspend fun deleteDay(d: RoutineDay)

    @Transaction
    @Query("SELECT * FROM RoutineExercise WHERE routineDayId = :dayId ORDER BY sortOrder, id")
    fun observeDayExercises(dayId: Long): Flow<List<RoutineExerciseWithExercise>>

    @Transaction
    @Query("SELECT * FROM RoutineExercise WHERE routineDayId = :dayId ORDER BY sortOrder, id")
    suspend fun getDayExercises(dayId: Long): List<RoutineExerciseWithExercise>

    @Insert suspend fun insertRoutineExercise(re: RoutineExercise): Long
    @Update suspend fun updateRoutineExercises(list: List<RoutineExercise>)
    @Delete suspend fun deleteRoutineExercise(re: RoutineExercise)
}

@Dao
interface WorkoutDao {
    @Insert suspend fun insertSession(s: WorkoutSession): Long
    @Update suspend fun updateSession(s: WorkoutSession)
    @Delete suspend fun deleteSession(s: WorkoutSession)

    @Query("SELECT * FROM WorkoutSession WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSession?

    @Query("SELECT * FROM WorkoutSession WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUnfinished(): WorkoutSession?

    @Query("SELECT * FROM WorkoutSession WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeUnfinished(): Flow<WorkoutSession?>

    @Query(
        "SELECT s.id, s.title, s.startedAt, s.source, " +
            "(SELECT COUNT(*) FROM SetEntry e WHERE e.sessionId = s.id) AS setCount " +
            "FROM WorkoutSession s WHERE s.endedAt IS NOT NULL ORDER BY s.startedAt DESC"
    )
    fun observeFinishedSessions(): Flow<List<SessionSummary>>

    @Query("SELECT EXISTS(SELECT 1 FROM WorkoutSession WHERE title = :title AND startedAt = :startedAt)")
    suspend fun existsSession(title: String, startedAt: Long): Boolean

    @Transaction
    @Query("SELECT * FROM SetEntry WHERE sessionId = :sessionId ORDER BY exerciseOrder, setIndex, id")
    fun observeSets(sessionId: Long): Flow<List<SetWithExercise>>

    @Query("SELECT * FROM SetEntry WHERE sessionId = :sessionId ORDER BY exerciseOrder, setIndex, id")
    suspend fun getSets(sessionId: Long): List<SetEntry>

    @Insert suspend fun insertSet(s: SetEntry): Long
    @Insert suspend fun insertSets(s: List<SetEntry>)
    @Update suspend fun updateSet(s: SetEntry)
    @Delete suspend fun deleteSet(s: SetEntry)

    /** Serie dell'ultima sessione conclusa (diversa da [excludeSessionId]) che contiene l'esercizio. */
    @Query(
        "SELECT * FROM SetEntry WHERE exerciseId = :exerciseId AND sessionId = (" +
            "SELECT s.id FROM WorkoutSession s JOIN SetEntry e ON e.sessionId = s.id " +
            "WHERE e.exerciseId = :exerciseId AND s.endedAt IS NOT NULL AND s.id != :excludeSessionId " +
            "ORDER BY s.startedAt DESC LIMIT 1) ORDER BY setIndex, id"
    )
    suspend fun lastFinishedSetsFor(exerciseId: Long, excludeSessionId: Long): List<SetEntry>

    @Transaction
    @Query(
        "SELECT e.* FROM SetEntry e JOIN WorkoutSession s ON s.id = e.sessionId " +
            "WHERE e.exerciseId = :exerciseId AND s.endedAt IS NOT NULL ORDER BY s.startedAt DESC, e.setIndex"
    )
    fun historyFor(exerciseId: Long): Flow<List<SetWithSession>>
}
