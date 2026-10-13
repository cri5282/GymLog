package it.gymlog.app.data.db

import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { db.close() }

    private suspend fun newExercise(name: String) =
        db.exerciseDao().insert(Exercise(name = name, primaryMuscle = Muscle.PETTO))

    private suspend fun finishedSession(start: Long) =
        db.workoutDao().insertSession(WorkoutSession(title = "S$start", startedAt = start, endedAt = start + 1))

    @Test
    fun setActive_leavesExactlyOneActiveProgram() = runTest {
        val dao = db.programDao()
        val a = dao.insertProgram(Program(name = "A", isActive = true))
        val b = dao.insertProgram(Program(name = "B"))
        dao.setActive(b)
        val programs = dao.observePrograms().first()
        assertEquals(listOf(b), programs.filter { it.isActive }.map { it.id })
        assertEquals(b, dao.getActive()?.id)
        assertFalse(programs.first { it.id == a }.isActive)
    }

    @Test
    fun deletingProgram_cascadesDaysAndRoutineExercises() = runTest {
        val dao = db.programDao()
        val ex = newExercise("Panca")
        val p = dao.insertProgram(Program(name = "P"))
        val d = dao.insertDay(RoutineDay(programId = p, name = "Push A", dayOfWeek = 1, sortOrder = 0))
        dao.insertRoutineExercise(RoutineExercise(routineDayId = d, exerciseId = ex, sortOrder = 0))
        dao.deleteProgram(dao.getProgram(p)!!)
        assertTrue(dao.observeDays(p).first().isEmpty())
        assertTrue(dao.getDayExercises(d).isEmpty())
    }

    @Test
    fun archivedExercise_stillJoinedInHistory() = runTest {
        val ex = newExercise("Pec Deck")
        val s = finishedSession(1000)
        db.workoutDao().insertSet(SetEntry(sessionId = s, exerciseId = ex, exerciseOrder = 0, setIndex = 0,
            setType = SetType.WORKING, weightKg = 50.0, reps = 10, completedAt = 1000))
        val e = db.exerciseDao().getWithDetails(ex)!!.exercise
        db.exerciseDao().update(e.copy(archived = true))
        assertTrue(db.exerciseDao().observeActive().first().none { it.exercise.id == ex })
        val sets = db.workoutDao().observeSets(s).first()
        assertEquals("Pec Deck", sets.single().exercise.name)
    }

    @Test
    fun isUsed_trueWhenInSetOrRoutine_falseOtherwise() = runTest {
        val unused = newExercise("Libero")
        val inSet = newExercise("In serie")
        val inRoutine = newExercise("In scheda")
        val s = finishedSession(1)
        db.workoutDao().insertSet(SetEntry(sessionId = s, exerciseId = inSet, exerciseOrder = 0, setIndex = 0,
            setType = SetType.WORKING, weightKg = 1.0, reps = 1, completedAt = 1))
        val p = db.programDao().insertProgram(Program(name = "P"))
        val d = db.programDao().insertDay(RoutineDay(programId = p, name = "D", sortOrder = 0))
        db.programDao().insertRoutineExercise(RoutineExercise(routineDayId = d, exerciseId = inRoutine, sortOrder = 0))
        val dao = db.exerciseDao()
        assertFalse(dao.isUsed(unused))
        assertTrue(dao.isUsed(inSet))
        assertTrue(dao.isUsed(inRoutine))
    }

    @Test
    fun lastFinishedSetsFor_skipsUnfinishedAndExcludedSession() = runTest {
        val ex = newExercise("Panca")
        val w = db.workoutDao()
        val a = finishedSession(1000)
        w.insertSet(SetEntry(sessionId = a, exerciseId = ex, exerciseOrder = 0, setIndex = 1, setType = SetType.WORKING, weightKg = 80.0, reps = 7, completedAt = 1002))
        w.insertSet(SetEntry(sessionId = a, exerciseId = ex, exerciseOrder = 0, setIndex = 0, setType = SetType.WORKING, weightKg = 80.0, reps = 8, completedAt = 1001))
        val b = w.insertSession(WorkoutSession(title = "In corso", startedAt = 5000))
        w.insertSet(SetEntry(sessionId = b, exerciseId = ex, exerciseOrder = 0, setIndex = 0, setType = SetType.WORKING, weightKg = 90.0, reps = 5, completedAt = 5001))
        val last = w.lastFinishedSetsFor(ex, excludeSessionId = b)
        assertEquals(listOf(8, 7), last.map { it.reps })
        assertEquals(b, w.getUnfinished()?.id)
        assertTrue(w.lastFinishedSetsFor(ex, excludeSessionId = a).isEmpty())
    }

    @Test
    fun replaceSecondary_replacesAll() = runTest {
        val ex = newExercise("Panca")
        val dao = db.exerciseDao()
        dao.replaceSecondary(ex, listOf(Muscle.TRICIPITI, Muscle.DELTOIDE_ANTERIORE))
        dao.replaceSecondary(ex, listOf(Muscle.BICIPITI))
        assertEquals(listOf(Muscle.BICIPITI), dao.getWithDetails(ex)!!.secondary.map { it.muscle })
    }

    @Test
    fun findByHevyName_matchesExactly() = runTest {
        db.exerciseDao().insert(Exercise(name = "Panca", hevyName = "Bench Press (Machine)", primaryMuscle = Muscle.PETTO))
        assertEquals("Panca", db.exerciseDao().findByHevyName("Bench Press (Machine)")?.name)
        assertNull(db.exerciseDao().findByHevyName("Bench Press"))
    }
}
