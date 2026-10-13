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
