package it.gymlog.app.data.hevy

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SessionSource
import it.gymlog.app.data.db.SetType
import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
class HevyImporterTest {
    private lateinit var db: AppDatabase
    private lateinit var importer: HevyImporter

    @Before fun setUp() = runTest {
        db = inMemoryDb()
        SeedData.seedIfEmpty(db)
        importer = HevyImporter(db)
    }
    @After fun tearDown() { db.close() }

    private fun sample() = javaClass.getResourceAsStream("/hevy_sample.csv")!!.reader()

    @Test fun import_createsSessionsAndSets() = runTest {
        val report = importer.import(sample())
        assertEquals(2, report.sessionsImported)
        val sessions = db.workoutDao().observeFinishedSessions().first()
        assertEquals(listOf("Push", "Pull"), sessions.map { it.title })
        assertTrue(sessions.all { it.source == SessionSource.HEVY_IMPORT })
        val pushSets = db.workoutDao().observeSets(sessions[0].id).first()
        assertEquals(4, pushSets.size)
        assertEquals("Chest Fly (Machine)", pushSets[0].exercise.name)
        assertEquals(listOf(0, 0, 1, 2), pushSets.map { it.set.exerciseOrder })
        assertEquals(SetType.WORKING, pushSets[0].set.setType)
    }

    @Test fun import_unknownExercise_createdWithNeedsReview() = runTest {
        val report = importer.import(sample())
        assertEquals(1, report.exercisesCreated)
        val e = db.exerciseDao().findByHevyName("Cable Crossover")!!
        assertEquals(Muscle.ADDOME, e.primaryMuscle)
        assertTrue(e.needsReview)
        assertEquals("Cable Crossover", e.name)
    }

    @Test fun import_twice_isIdempotent() = runTest {
        importer.import(sample())
        val second = importer.import(sample())
        assertEquals(0, second.sessionsImported)
        assertEquals(2, second.sessionsSkipped)
        assertEquals(0, second.exercisesCreated)
        val sessions = db.workoutDao().observeFinishedSessions().first()
        assertEquals(listOf(4, 3), sessions.map { it.setCount })
    }

    @Test fun import_badFile_writesNothing() = runTest {
        try {
            importer.import(StringReader("\"a\",\"b\"\n1,2"))
        } catch (_: HevyFormatException) { }
        assertTrue(db.workoutDao().observeFinishedSessions().first().isEmpty())
        assertEquals(31, db.exerciseDao().count())
    }
}
