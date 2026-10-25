package it.gymlog.app.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class ExerciseWithDetails(
    @Embedded val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val secondary: List<ExerciseSecondaryMuscle>,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val photos: List<ExercisePhoto>,
) {
    val sortedPhotos: List<ExercisePhoto> get() = photos.sortedBy { it.sortOrder }
}

data class RoutineExerciseWithExercise(
    @Embedded val routineExercise: RoutineExercise,
    @Relation(entity = Exercise::class, parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseWithDetails,
)

data class SetWithExercise(
    @Embedded val set: SetEntry,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
)

data class SetWithSession(
    @Embedded val set: SetEntry,
    @Relation(parentColumn = "sessionId", entityColumn = "id")
    val session: WorkoutSession,
)

data class SessionSummary(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val source: SessionSource,
    val setCount: Int,
)

data class SessionTime(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val endedAt: Long?,
    val setCount: Int,
    val source: SessionSource = SessionSource.MANUAL,
)

/** Una serie conclusa con i dati che servono ai grafici. */
data class StatSet(
    val setId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscle: Muscle,
    val single: Boolean,
    val startedAt: Long,
    val setType: SetType,
    val weightKg: Double,
    val weightKgR: Double?,
    val reps: Int,
    val repsR: Int?,
)

/** Riga piatta per l'esportazione CSV. */
data class CsvRow(
    val sessionId: Long,
    val title: String,
    val startedAt: Long,
    val endedAt: Long?,
    val exerciseName: String,
    val setIndex: Int,
    val setType: SetType,
    val weightKg: Double,
    val weightKgR: Double?,
    val reps: Int,
    val repsR: Int?,
    val rir: Int?,
    val rirR: Int?,
    val notes: String,
)

data class DayExerciseCount(val routineDayId: Long, val count: Int)
