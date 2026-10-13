package it.gymlog.app.ui.workout

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.timer.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

class FakeRestTimer : RestTimer {
    override val state = MutableStateFlow(TimerState())
    var started: Int? = null
    override fun start(seconds: Int) { started = seconds }
    override fun addSeconds(seconds: Int) {}
    override fun skip() { started = null }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkoutViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val timer = FakeRestTimer()

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDb()
    }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private class Env(val settings: SettingsRepository, val workouts: WorkoutRepository, val session: Long)

    private suspend fun TestScope.env(): Env {
        SeedData.seedIfEmpty(db)
        val settings = testSettings(tmp.root, backgroundScope)
        val workouts = WorkoutRepository(db, settings)
        val day = ProgramRepository(db).todayDay(LocalDate.of(2026, 10, 12))!!.id
        return Env(settings, workouts, workouts.startOrResume(day))
    }

    private fun vm(e: Env) = WorkoutViewModel(e.session, e.workouts, e.settings, timer)

    private suspend fun WorkoutViewModel.await(cond: (WorkoutUiState) -> Boolean) =
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) { state.first { !it.loading && cond(it) } }
        }

    @Test fun confirm_persistsSetAndStartsTimerWithExerciseRest() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 0, 60.0); vm.changeReps(0, 0, 8)
        vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        val sets = db.workoutDao().getSets(e.session)
        assertEquals(1, sets.size)
        assertEquals(60.0, sets[0].weightKg, 1e-9)
        assertEquals(8, sets[0].reps)
        assertEquals(150, timer.started)
    }

    @Test fun applySuggestion_setsWeightOnUndoneWorkingRows() = runTest {
        val e = env()
        // sessione precedente conclusa: 3 × 80 kg × 8 @ RIR 2 su Incline Chest Press
        val first = vm(e).await { true }.pages[0].planned.exercise.exercise.id
        repeat(3) { e.workouts.confirmSet(e.session, first, 0, it, SetDraft(80.0, 8, 2, SetType.WORKING), "") }
        e.workouts.finish(e.session)
        val next = e.workouts.startOrResume(ProgramRepository(db).todayDay(LocalDate.of(2026, 10, 12))!!.id)
        val vm = WorkoutViewModel(next, e.workouts, e.settings, timer)
        vm.await { it.pages[0].rows.size == 3 }
        vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        vm.applySuggestion(0)
        val rows = vm.await { it.pages[0].rows[1].weightKg == 82.5 }.pages[0].rows
        assertEquals(80.0, rows[0].weightKg, 1e-9)
        assertEquals(listOf(82.5, 82.5), rows.drop(1).map { it.weightKg })
    }

    @Test fun changeWeight_usesLoadStep_andNeverBelowZero() = runTest {
        val e = env()
        e.settings.update { it.copy(defaultLoadStepKg = 5.0) }
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 0, 2.5)
        vm.changeWeight(0, 0, +1)
        assertEquals(7.5, vm.state.value.pages[0].rows[0].weightKg, 1e-9)
        vm.changeWeight(0, 0, -1); vm.changeWeight(0, 0, -1)
        assertEquals(0.0, vm.state.value.pages[0].rows[0].weightKg, 1e-9)
        vm.changeReps(0, 0, -1)
        assertEquals(0, vm.state.value.pages[0].rows[0].reps)
    }

    @Test fun reopenViewModel_restoresDoneRows() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 0, 70.0); vm.setRir(0, 0, 1)
        vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        val reopened = vm(e).await { true }
        val row = reopened.pages[0].rows[0]
        assertTrue(row.done)
        assertEquals(70.0, row.weightKg, 1e-9)
        assertEquals(1, row.rir)
        assertEquals(3, reopened.pages[0].rows.size)
    }

    @Test fun addExercise_appendsPage_andReplaceSwapsExercise() = runTest {
        val e = env()
        val vm = vm(e)
        val pages = vm.await { true }.pages.size
        val shrug = db.exerciseDao().findByName("Shrug (Machine)")!!.id
        vm.addExercise(shrug)
        assertEquals("Shrug (Machine)", vm.await { it.pages.size == pages + 1 }.pages.last().planned.exercise.exercise.name)
        val lateral = db.exerciseDao().findByName("Lateral Raise (Cable)")!!.id
        vm.replaceExercise(1, lateral)
        val replaced = vm.await { it.pages[1].planned.exercise.exercise.id == lateral }.pages[1]
        assertEquals(3, replaced.planned.target!!.sets) // target della Panca Piana mantenuto
    }

    @Test fun finish_setsRecap() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 0, 50.0); vm.changeReps(0, 0, 10)
        vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        vm.finish()
        val recap = vm.await { it.recap != null }.recap!!
        assertEquals(1, recap.workingSets)
        assertEquals(500.0, recap.volumeKg, 1e-9)
    }

    @Test fun doubleTapConfirm_savesOnce() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.confirm(0, 0); vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        withContext(Dispatchers.Default.limitedParallelism(1)) { kotlinx.coroutines.delay(300) }
        assertEquals(1, db.workoutDao().getSets(e.session).size)
    }

    @Test fun editingDoneRow_isSavedWithoutReconfirm() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 0, 60.0)
        vm.confirm(0, 0)
        vm.await { it.pages[0].rows[0].done }
        vm.setWeight(0, 0, 65.0)
        vm.setRir(0, 0, 0)
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10_000) {
                while (db.workoutDao().getSets(e.session).single().let { it.weightKg != 65.0 || it.rir != 0 }) kotlinx.coroutines.delay(20)
            }
        }
    }

    @Test fun confirmOutOfOrder_resumeKeepsSlots() = runTest {
        val e = env()
        val vm = vm(e)
        vm.await { true }
        vm.setWeight(0, 2, 70.0)
        vm.confirm(0, 2)
        vm.await { it.pages[0].rows[2].done }
        val rows = vm(e).await { true }.pages[0].rows
        assertEquals(3, rows.size)
        assertEquals(listOf(false, false, true), rows.map { it.done })
        assertEquals(70.0, rows[2].weightKg, 1e-9)
    }
}
