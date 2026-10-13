package it.gymlog.app.data.db

import androidx.room.withTransaction
import it.gymlog.app.data.db.LengthBias.LENGTHENED
import it.gymlog.app.data.db.LengthBias.NONE
import it.gymlog.app.data.db.LengthBias.SHORTENED
import it.gymlog.app.data.db.Muscle.*

/** Libreria e programma precaricati al primo avvio (spec §7). */
object SeedData {

    data class SeedExercise(
        val name: String,
        val primary: Muscle,
        val secondary: List<Muscle> = emptyList(),
        val bias: LengthBias = NONE,
        val notes: String = "",
    )

    data class SeedSlot(
        val exercise: String,
        val sets: Int,
        val repMin: Int,
        val repMax: Int,
        val rirMin: Int,
        val rirMax: Int,
        val restSeconds: Int,
    )

    data class SeedDay(val name: String, val dayOfWeek: Int, val slots: List<SeedSlot>)

    const val PROGRAM_NAME = "Ipertrofia – ottobre 2026"

    private val PRESS = listOf(TRICIPITI, DELTOIDE_ANTERIORE)
    private val ROW = listOf(TRAPEZI, DELTOIDE_POSTERIORE, BICIPITI)

    val exercises: List<SeedExercise> = listOf(
        SeedExercise("Incline Chest Press (Machine)", PETTO, PRESS),
        SeedExercise("Panca Piana (Macchina)", PETTO, PRESS),
        SeedExercise("Chest Press Declinata (Macchina)", PETTO, PRESS),
        SeedExercise("Decline Bench Press (Machine)", PETTO, PRESS),
        SeedExercise("Chest Fly (Machine)", PETTO, listOf(DELTOIDE_ANTERIORE), LENGTHENED, "Pausa 1 s in allungamento"),
        SeedExercise("Croci Petto Alto (Macchina)", PETTO, listOf(DELTOIDE_ANTERIORE), LENGTHENED, "Pausa 1 s in allungamento"),
        SeedExercise("Overhead Triceps Extension (Cable)", TRICIPITI, bias = LENGTHENED),
        SeedExercise("Triceps Extension (Cable)", TRICIPITI, bias = SHORTENED),
        SeedExercise("Single Arm Triceps Pushdown (Cable)", TRICIPITI, bias = SHORTENED),
        SeedExercise("Shoulder Press (Machine Plates)", DELTOIDE_ANTERIORE, listOf(DELTOIDE_LATERALE, TRICIPITI)),
        SeedExercise("Lateral Raise (Cable)", DELTOIDE_LATERALE, bias = LENGTHENED, notes = "Cavo dietro il corpo"),
        SeedExercise("Crunch (Machine)", ADDOME),
        SeedExercise("Lat Pulldown (Machine)", DORSALI, listOf(BICIPITI)),
        SeedExercise("Single Lat Pulldown (Macchina)", DORSALI, listOf(BICIPITI), LENGTHENED, "Enfasi sull'allungamento in alto"),
        SeedExercise("Iso-Lateral Low Row", DORSALI, ROW),
        SeedExercise("Seated Cable Row - Bar Grip", DORSALI, ROW),
        SeedExercise("Shrug (Machine)", TRAPEZI, listOf(AVAMBRACCI), notes = "Pausa 1-2 s in alto"),
        SeedExercise("Rear Delt Reverse Fly (Machine)", DELTOIDE_POSTERIORE, listOf(TRAPEZI)),
        SeedExercise("Seated Incline Curl (Dumbbell)", BICIPITI, bias = LENGTHENED),
        SeedExercise("Single Hammer Curl (Cavo)", BICIPITI, listOf(AVAMBRACCI)),
        SeedExercise("Preacher Curl (Machine)", BICIPITI, bias = SHORTENED),
        SeedExercise("Behind the Back Curl (Cable)", BICIPITI, bias = LENGTHENED),
        SeedExercise("Estensione Polsi alla Lat Machine (Macchina)", AVAMBRACCI),
        SeedExercise("Single Leg Extensions", QUADRICIPITI, notes = "Gamba infortunata: ROM 90°→45°, cuscinetto prossimale sulla tibia"),
        SeedExercise("Single Leg Press (Machine)", QUADRICIPITI, listOf(GLUTEI, ADDUTTORI), notes = "ROM 10°→90° di flessione, niente blocco in estensione, piede stabile"),
        SeedExercise("Seated Leg Curl (Machine)", FEMORALI, bias = LENGTHENED),
        SeedExercise("Lying Leg Curl (Machine)", FEMORALI),
        SeedExercise("Calf Press (Machine)", POLPACCI, bias = LENGTHENED, notes = "Pausa 1 s in allungamento"),
        SeedExercise("Standing Calf Raise (Machine)", POLPACCI, bias = LENGTHENED),
        SeedExercise("Hip Abduction (Machine)", ABDUTTORI, listOf(GLUTEI)),
        SeedExercise("Hip Adduction (Machine)", ADDUTTORI),
    )

    private fun s(name: String, sets: Int, reps: IntRange, rir: IntRange, rest: Int) =
        SeedSlot(name, sets, reps.first, reps.last, rir.first, rir.last, rest)

    val days: List<SeedDay> = listOf(
        SeedDay("Push A", 1, listOf(
            s("Incline Chest Press (Machine)", 3, 6..8, 1..2, 150),
            s("Panca Piana (Macchina)", 3, 6..8, 1..1, 150),
            s("Chest Fly (Machine)", 2, 8..12, 0..1, 120),
            s("Overhead Triceps Extension (Cable)", 2, 8..12, 0..1, 120),
            s("Shoulder Press (Machine Plates)", 2, 6..8, 1..2, 150),
            s("Crunch (Machine)", 2, 8..12, 1..1, 90),
        )),
        SeedDay("Pull A", 2, listOf(
            s("Lat Pulldown (Machine)", 3, 6..8, 1..2, 150),
            s("Iso-Lateral Low Row", 3, 6..8, 1..1, 150),
            s("Single Lat Pulldown (Macchina)", 2, 8..10, 0..1, 120),
            s("Shrug (Machine)", 3, 8..12, 0..1, 120),
            s("Rear Delt Reverse Fly (Machine)", 2, 10..15, 0..1, 90),
            s("Seated Incline Curl (Dumbbell)", 2, 8..10, 0..1, 120),
        )),
        SeedDay("Legs", 3, listOf(
            s("Single Leg Extensions", 3, 10..12, 2..2, 120),
            s("Single Leg Press (Machine)", 3, 8..12, 1..2, 150),
            s("Seated Leg Curl (Machine)", 3, 8..12, 0..1, 120),
            s("Calf Press (Machine)", 3, 8..12, 0..1, 90),
            s("Hip Abduction (Machine)", 2, 10..15, 1..1, 90),
            s("Hip Adduction (Machine)", 2, 10..15, 1..1, 90),
            s("Crunch (Machine)", 2, 8..12, 1..1, 90),
        )),
        SeedDay("Push B", 4, listOf(
            s("Incline Chest Press (Machine)", 3, 6..8, 1..2, 150),
            s("Chest Press Declinata (Macchina)", 2, 6..8, 1..1, 150),
            s("Croci Petto Alto (Macchina)", 2, 8..12, 0..1, 120),
            s("Overhead Triceps Extension (Cable)", 2, 8..12, 0..1, 120),
            s("Lateral Raise (Cable)", 2, 10..15, 0..1, 90),
            s("Crunch (Machine)", 2, 8..12, 1..1, 90),
        )),
        SeedDay("Pull B", 5, listOf(
            s("Single Lat Pulldown (Macchina)", 3, 8..10, 0..1, 120),
            s("Seated Cable Row - Bar Grip", 3, 6..8, 1..1, 150),
            s("Lat Pulldown (Machine)", 2, 6..8, 1..2, 150),
            s("Shrug (Machine)", 3, 8..12, 0..1, 120),
            s("Rear Delt Reverse Fly (Machine)", 2, 10..15, 0..1, 90),
            s("Single Hammer Curl (Cavo)", 2, 8..10, 0..1, 90),
            s("Single Leg Extensions", 2, 10..12, 2..2, 90),
            s("Seated Leg Curl (Machine)", 2, 10..12, 2..2, 90),
        )),
    )

    /** Inserisce i dati solo se il database non contiene né esercizi né programmi. */
    suspend fun seedIfEmpty(db: AppDatabase) = db.withTransaction {
        val exDao = db.exerciseDao()
        val prDao = db.programDao()
        if (exDao.count() > 0 || prDao.countPrograms() > 0) return@withTransaction
        val ids = exercises.associate { e ->
            val id = exDao.insert(
                Exercise(name = e.name, primaryMuscle = e.primary, lengthBias = e.bias,
                    executionNotes = e.notes, hevyName = e.name)
            )
            exDao.replaceSecondary(id, e.secondary)
            e.name to id
        }
        val programId = prDao.insertProgram(Program(name = PROGRAM_NAME, isActive = true))
        days.forEachIndexed { dayIndex, day ->
            val dayId = prDao.insertDay(RoutineDay(programId = programId, name = day.name,
                dayOfWeek = day.dayOfWeek, sortOrder = dayIndex))
            day.slots.forEachIndexed { i, slot ->
                prDao.insertRoutineExercise(
                    RoutineExercise(
                        routineDayId = dayId, exerciseId = ids.getValue(slot.exercise), sortOrder = i,
                        targetSets = slot.sets, repMin = slot.repMin, repMax = slot.repMax,
                        targetRirMin = slot.rirMin, targetRirMax = slot.rirMax, restSeconds = slot.restSeconds,
                    )
                )
            }
        }
    }
}
