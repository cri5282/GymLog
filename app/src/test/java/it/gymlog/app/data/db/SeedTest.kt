package it.gymlog.app.data.db

import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { db.close() }

    private suspend fun days(): List<RoutineDay> {
        val p = db.programDao().getActive()!!
        return db.programDao().getDays(p.id)
    }

    @Test
    fun seed_creates31ExercisesAndActiveProgram() = runTest {
        SeedData.seedIfEmpty(db)
        assertEquals(31, db.exerciseDao().observeActive().first().size)
        val p = db.programDao().getActive()!!
        assertEquals("Ipertrofia – ottobre 2026", p.name)
        val d = days()
        assertEquals(listOf("Push A", "Pull A", "Legs", "Push B", "Pull B"), d.map { it.name })
        assertEquals(listOf(1, 2, 3, 4, 5), d.map { it.dayOfWeek })
    }

    @Test
    fun seed_routineCounts() = runTest {
        SeedData.seedIfEmpty(db)
        assertEquals(listOf(6, 6, 7, 6, 8), days().map { db.programDao().getDayExercises(it.id).size })
    }

    @Test
    fun seed_pullB_firstExercise() = runTest {
        SeedData.seedIfEmpty(db)
        val first = db.programDao().getDayExercises(days()[4].id).first()
        assertEquals("Single Lat Pulldown (Macchina)", first.exercise.exercise.name)
        with(first.routineExercise) {
            assertEquals(3, targetSets); assertEquals(8, repMin); assertEquals(10, repMax)
            assertEquals(0, targetRirMin); assertEquals(1, targetRirMax); assertEquals(120, restSeconds)
        }
    }

    @Test
    fun seed_legExtensionNotes() = runTest {
        SeedData.seedIfEmpty(db)
        val e = db.exerciseDao().findByName("Single Leg Extensions")!!
        assertEquals("Gamba infortunata: ROM 90°→45°, cuscinetto prossimale sulla tibia", e.executionNotes)
        assertEquals("Single Leg Extensions", e.hevyName)
    }

    @Test
    fun seed_secondaryMuscles() = runTest {
        SeedData.seedIfEmpty(db)
        val e = db.exerciseDao().findByName("Iso-Lateral Low Row")!!
        val sec = db.exerciseDao().getWithDetails(e.id)!!.secondary.map { it.muscle }.toSet()
        assertEquals(setOf(Muscle.TRAPEZI, Muscle.DELTOIDE_POSTERIORE, Muscle.BICIPITI), sec)
    }

    @Test
    fun seed_isNotRepeatedOnReopen() = runTest {
        SeedData.seedIfEmpty(db)
        SeedData.seedIfEmpty(db)
        assertEquals(31, db.exerciseDao().observeActive().first().size)
        assertEquals(1, db.programDao().observePrograms().first().size)
    }
}
