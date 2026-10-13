package it.gymlog.app.data.repo

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SetType
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var programs: ProgramRepository

    @Before fun setUp() = runTest { db = inMemoryDb(); SeedData.seedIfEmpty(db); programs = ProgramRepository(db) }
    @After fun tearDown() { db.close() }

    private fun kotlinx.coroutines.test.TestScope.repo() =
        WorkoutRepository(db, testSettings(tmp.root, backgroundScope), clock = { 1_000_000L })

    private suspend fun pushA() = programs.todayDay(LocalDate.of(2026, 10, 12))!!.id
    private fun w(kg: Double, reps: Int, rir: Int?) = SetDraft(kg, reps, rir, SetType.WORKING)

    @Test fun startOrResume_returnsUnfinishedSession() = runTest {
        val r = repo()
        val a = r.startOrResume(pushA())
        assertEquals(a, r.startOrResume(pushA()))
        assertEquals(a, r.startOrResume(null))
        r.finish(a)
        assertNotEquals(a, r.startOrResume(pushA()))
    }

    @Test fun startOrResume_titles() = runTest {
        val r = repo()
        val s = r.startOrResume(null)
        assertEquals("Allenamento libero", db.workoutDao().getSession(s)!!.title)
        r.finish(s)
        assertEquals("Push A", db.workoutDao().getSession(r.startOrResume(pushA()))!!.title)
    }

    @Test fun plan_usesLastFinishedSessionAndSuggestion() = runTest {
        val r = repo()
        val first = r.startOrResume(pushA())
        val incline = r.plan(first).first()
        assertEquals("Incline Chest Press (Machine)", incline.exercise.exercise.name)
        assertNull(incline.suggestedKg)
        repeat(3) { i -> r.confirmSet(first, incline.exercise.exercise.id, 0, i, w(80.0, 8, 2), "") }
        r.finish(first)

        val second = r.startOrResume(pushA())
        val p = r.plan(second).first()
        assertEquals(List(3) { w(80.0, 8, 2) }, p.prefill)
        assertEquals(82.5, p.suggestedKg!!, 1e-9)
        assertEquals(150, p.restSeconds)
        assertEquals(3, p.target!!.sets)
    }

    @Test fun plan_includesSetsAlreadyDoneAndExtraExercises() = runTest {
        val r = repo()
        val s = r.startOrResume(pushA())
        val shrug = db.exerciseDao().findByName("Shrug (Machine)")!!.id
        r.confirmSet(s, shrug, 6, 0, w(60.0, 10, 1), "")
        val plan = r.plan(s)
        assertEquals(7, plan.size)
        assertEquals("Shrug (Machine)", plan.last().exercise.exercise.name)
        assertEquals(1, plan.last().done.size)
        assertNull(plan.last().target)
    }

    @Test fun finish_recapCountsOnlyNonWarmup() = runTest {
        val r = repo()
        val s = r.startOrResume(pushA())
        val ex = r.plan(s).first().exercise.exercise.id
        r.confirmSet(s, ex, 0, 0, SetDraft(40.0, 10, null, SetType.WARMUP), "")
        r.confirmSet(s, ex, 0, 1, w(80.0, 8, 2), "")
        r.confirmSet(s, ex, 0, 2, w(80.0, 8, 2), "")
        val recap = r.finish(s)
        assertEquals(2, recap.workingSets)
        assertEquals(1280.0, recap.volumeKg, 1e-9)
    }

    @Test fun plan_afterReplacingSlot_keepsUniqueOrdersAndShowsSubstitute() = runTest {
        val r = repo()
        val s = r.startOrResume(pushA())
        val lateral = db.exerciseDao().findByName("Lateral Raise (Cable)")!!.id
        r.confirmSet(s, lateral, 5, 0, w(10.0, 12, 1), "") // Crunch (posizione 5) sostituito
        val plan = r.plan(s)
        assertEquals(plan.map { it.order }.distinct(), plan.map { it.order })
        assertEquals(6, plan.size)
        assertEquals("Lateral Raise (Cable)", plan[5].exercise.exercise.name)
        assertEquals(1, plan[5].done.size)
        assertEquals(2, plan[5].target!!.sets)
    }
}
