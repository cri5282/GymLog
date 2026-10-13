package it.gymlog.app.data.repo

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ProgramRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ProgramRepository

    @Before fun setUp() = runTest {
        db = inMemoryDb(); SeedData.seedIfEmpty(db); repo = ProgramRepository(db)
    }
    @After fun tearDown() { db.close() }

    @Test fun todayDay_monday_returnsPushA_sunday_returnsNull() = runTest {
        assertEquals("Push A", repo.todayDay(LocalDate.of(2026, 10, 12))?.name) // lunedì
        assertEquals("Pull B", repo.todayDay(LocalDate.of(2026, 10, 16))?.name) // venerdì
        assertNull(repo.todayDay(LocalDate.of(2026, 10, 18))) // domenica
    }

    @Test fun moveRoutineExercise_swapsOrder() = runTest {
        val day = repo.todayDay(LocalDate.of(2026, 10, 12))!!
        val before = repo.observeDayExercises(day.id).first()
        repo.moveRoutineExercise(before[1].routineExercise, up = true)
        val after = repo.observeDayExercises(day.id).first().map { it.exercise.exercise.name }
        assertEquals(listOf(before[1], before[0]).map { it.exercise.exercise.name }, after.take(2))
        repo.moveRoutineExercise(repo.observeDayExercises(day.id).first().first().routineExercise, up = true) // già in cima: nessun effetto
        assertEquals(after, repo.observeDayExercises(day.id).first().map { it.exercise.exercise.name })
    }

    @Test fun setActive_switchesProgram() = runTest {
        val id = repo.create("Nuovo")
        repo.setActive(id)
        assertEquals("Nuovo", db.programDao().getActive()?.name)
        assertNull(repo.todayDay(LocalDate.of(2026, 10, 12)))
    }

    @Test fun addExerciseToDay_usesDefaultsAndAppends() = runTest {
        val day = repo.todayDay(LocalDate.of(2026, 10, 12))!!
        val exId = db.exerciseDao().findByName("Lateral Raise (Cable)")!!.id
        repo.addExerciseToDay(day.id, exId)
        val last = repo.observeDayExercises(day.id).first().last().routineExercise
        assertEquals(exId, last.exerciseId)
        assertEquals(listOf(3, 8, 12, 1, 2, 120), with(last) { listOf(targetSets, repMin, repMax, targetRirMin, targetRirMax, restSeconds) })
    }
}
