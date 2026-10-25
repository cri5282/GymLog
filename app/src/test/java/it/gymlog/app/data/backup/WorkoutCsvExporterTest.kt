package it.gymlog.app.data.backup

import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class WorkoutCsvExporterTest {
    @Test fun exportsHeaderAndEscapesCommasQuotesAndNewlines() = runTest {
        val db = inMemoryDb()
        try {
            val exerciseId = db.exerciseDao().insert(
                Exercise(name = "Panca, \"speciale\"", primaryMuscle = Muscle.PETTO),
            )
            val sessionId = db.workoutDao().insertSession(
                WorkoutSession(
                    title = "Push, \"A\"", startedAt = 1_700_000_000_000,
                    endedAt = 1_700_003_600_000, notes = "",
                ),
            )
            db.workoutDao().insertSet(
                SetEntry(
                    sessionId = sessionId, exerciseId = exerciseId, exerciseOrder = 0, setIndex = 0,
                    setType = SetType.WORKING, weightKg = 82.5, reps = 8, rir = 2,
                    notes = "nota, \"ok\"\nseconda riga", completedAt = 1_700_000_100_000,
                ),
            )
            db.workoutDao().insertSet(
                SetEntry(
                    sessionId = sessionId, exerciseId = exerciseId, exerciseOrder = 0, setIndex = 1,
                    setType = SetType.WORKING, weightKg = 20.0, reps = 10,
                    weightKgR = 22.0, repsR = 9, rirR = 1, completedAt = 1_700_000_200_000,
                ),
            )

            val out = ByteArrayOutputStream()
            val rows = WorkoutCsvExporter(db).export(out)
            val text = out.toString(Charsets.UTF_8)
            assertEquals(2, rows)
            assertTrue(text.startsWith("sessione_id,data,ora,titolo,esercizio,tipo_serie,kg_sx,reps_sx,rir_sx,kg_dx,reps_dx,rir_dx,note\n"))
            assertTrue(text.contains("\"Push, \"\"A\"\"\""))
            assertTrue(text.contains("\"Panca, \"\"speciale\"\"\""))
            assertTrue(text.contains("\"nota, \"\"ok\"\"\nseconda riga\""))
            assertTrue(text.contains(",82.5,8,2,,,"))
            assertTrue(text.contains(",20,10,,22,9,1,"))
        } finally {
            db.close()
        }
    }
}
