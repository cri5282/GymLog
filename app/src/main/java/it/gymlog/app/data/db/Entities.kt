package it.gymlog.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Exercise",
    indices = [Index(value = ["name"], unique = true), Index(value = ["hevyName"], unique = true)],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: Muscle,
    val lengthBias: LengthBias = LengthBias.NONE,
    val executionNotes: String = "",
    val hevyName: String? = null,
    val loadStepKg: Double? = null,
    val archived: Boolean = false,
    val needsReview: Boolean = false,
    /** Esercizio unilaterale: peso, reps e RIR separati tra lato sinistro e destro. */
    @ColumnInfo(defaultValue = "0") val single: Boolean = false,
)

@Entity(
    tableName = "ExerciseSecondaryMuscle",
    primaryKeys = ["exerciseId", "muscle"],
    foreignKeys = [ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class ExerciseSecondaryMuscle(
    val exerciseId: Long,
    val muscle: Muscle,
)

@Entity(
    tableName = "ExercisePhoto",
    indices = [Index("exerciseId")],
    foreignKeys = [ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class ExercisePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val filePath: String,
    val sortOrder: Int,
)

@Entity(tableName = "Program")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = false,
)

@Entity(
    tableName = "RoutineDay",
    indices = [Index("programId")],
    foreignKeys = [ForeignKey(entity = Program::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE)],
)
data class RoutineDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val name: String,
    /** 1 = lunedì … 7 = domenica; null = nessun giorno fisso. */
    val dayOfWeek: Int? = null,
    val sortOrder: Int,
)

@Entity(
    tableName = "RoutineExercise",
    indices = [Index("routineDayId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(entity = RoutineDay::class, parentColumns = ["id"], childColumns = ["routineDayId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineDayId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val targetSets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val targetRirMin: Int = 1,
    val targetRirMax: Int = 2,
    val restSeconds: Int = 120,
    val notes: String = "",
    /** Superserie: esercizi della stessa scheda con lo stesso numero (1, 2, …) si alternano. */
    val supersetGroup: Int? = null,
)

@Entity(
    tableName = "WorkoutSession",
    indices = [Index("routineDayId"), Index(value = ["title", "startedAt"])],
    foreignKeys = [ForeignKey(entity = RoutineDay::class, parentColumns = ["id"], childColumns = ["routineDayId"], onDelete = ForeignKey.SET_NULL)],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineDayId: Long? = null,
    val title: String,
    val startedAt: Long,
    /** null = allenamento in corso. */
    val endedAt: Long? = null,
    val notes: String = "",
    val source: SessionSource = SessionSource.MANUAL,
)

@Entity(
    tableName = "SetEntry",
    indices = [Index("sessionId"), Index("exerciseId")],
    foreignKeys = [
        ForeignKey(entity = WorkoutSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    /** Posizione dell'esercizio nella sessione. */
    val exerciseOrder: Int,
    val setIndex: Int,
    val setType: SetType,
    val weightKg: Double,
    val reps: Int,
    val rir: Int? = null,
    val notes: String = "",
    val completedAt: Long,
    /** Lato destro di una serie unilaterale (weightKg/reps/rir sono il sinistro); null = serie bilaterale. */
    val weightKgR: Double? = null,
    val repsR: Int? = null,
    val rirR: Int? = null,
)

@Entity(
    tableName = "BodyEntry",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class BodyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Giorno della misurazione, come epoch day (LocalDate.toEpochDay). */
    val epochDay: Long,
    val weightKg: Double? = null,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val calfCm: Double? = null,
    /** Pliche cutanee in mm (Jackson-Pollock 7). */
    val skinChest: Double? = null,
    val skinMidaxillary: Double? = null,
    val skinTriceps: Double? = null,
    val skinSubscapular: Double? = null,
    val skinAbdomen: Double? = null,
    val skinSuprailiac: Double? = null,
    val skinThigh: Double? = null,
    val notes: String = "",
    val measuredAt: Long = 0,
) {
    /** Almeno un valore registrato: le voci sono tutte facoltative. */
    val hasData: Boolean get() = listOf(
        weightKg, neckCm, chestCm, waistCm, hipsCm, armCm, thighCm, calfCm,
        skinChest, skinMidaxillary, skinTriceps, skinSubscapular, skinAbdomen, skinSuprailiac, skinThigh,
    ).any { it != null }
}
