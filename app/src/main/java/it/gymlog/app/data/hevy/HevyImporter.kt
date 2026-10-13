package it.gymlog.app.data.hevy

import androidx.room.withTransaction
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SessionSource
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.WorkoutSession
import java.io.Reader

data class ImportReport(
    val sessionsImported: Int,
    val sessionsSkipped: Int,
    val exercisesCreated: Int,
    val invalidLines: List<Int>,
)

class HevyImporter(private val db: AppDatabase) {

    /** Importa lo storico: lancia [HevyFormatException] senza scrivere nulla se il file non è valido. */
    suspend fun import(input: Reader): ImportReport {
        val parsed = HevyCsvParser.parse(input)
        val workoutDao = db.workoutDao()
        val exerciseDao = db.exerciseDao()
        return db.withTransaction {
            var imported = 0
            var skipped = 0
            var created = 0
            val exerciseIds = mutableMapOf<String, Long>()

            suspend fun exerciseId(hevyName: String): Long = exerciseIds.getOrPut(hevyName) {
                exerciseDao.findByHevyName(hevyName)?.id
                    ?: exerciseDao.findByName(hevyName)?.id
                    ?: exerciseDao.insert(
                        Exercise(name = hevyName, hevyName = hevyName, primaryMuscle = Muscle.ADDOME, needsReview = true)
                    ).also { created++ }
            }

            val sessions = parsed.rows.groupBy { it.title to it.startTime }
            for ((key, rows) in sessions) {
                val (title, start) = key
                if (workoutDao.existsSession(title, start)) { skipped++; continue }
                val first = rows.first()
                val sessionId = workoutDao.insertSession(
                    WorkoutSession(
                        title = title, startedAt = start, endedAt = first.endTime ?: start,
                        source = SessionSource.HEVY_IMPORT,
                    )
                )
                val order = rows.map { it.exerciseTitle }.distinct().withIndex().associate { (i, n) -> n to i }
                workoutDao.insertSets(rows.map { r ->
                    SetEntry(
                        sessionId = sessionId, exerciseId = exerciseId(r.exerciseTitle),
                        exerciseOrder = order.getValue(r.exerciseTitle), setIndex = r.setIndex,
                        setType = r.setType, weightKg = r.weightKg, reps = r.reps, rir = r.rir,
                        notes = r.notes, completedAt = first.endTime ?: start,
                    )
                })
                imported++
            }
            ImportReport(imported, skipped, created, parsed.invalidLines)
        }
    }
}
