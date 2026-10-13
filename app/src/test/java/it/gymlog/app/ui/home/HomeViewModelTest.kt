package it.gymlog.app.ui.home

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.domain.DayMarker
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val zone = ZoneId.of("Europe/Rome")
    private val today = LocalDate.of(2026, 10, 12) // lunedì
    private val todayMillis = LocalDateTime.of(2026, 10, 12, 10, 0).atZone(zone).toInstant().toEpochMilli()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private suspend fun TestScope.vm(): Pair<HomeViewModel, WorkoutRepository> {
        SeedData.seedIfEmpty(db)
        val workouts = WorkoutRepository(db, testSettings(tmp.root, backgroundScope), clock = { todayMillis })
        return HomeViewModel(ProgramRepository(db), workouts, today = { today }, zone = zone) to workouts
    }

    private suspend fun HomeViewModel.await(cond: (HomeState) -> Boolean) =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { state.first(cond) } }

    @Test fun doubleTapStart_navigatesOnce() = runTest {
        val (vm, _) = vm()
        var calls = 0
        vm.start(null) { calls++ }
        vm.start(null) { calls++ }
        withContext(Dispatchers.Default.limitedParallelism(1)) { delay(500) }
        assertEquals(1, calls)
    }

    @Test fun initial_selectsTodayWithPushA() = runTest {
        val (vm, _) = vm()
        val s = vm.await { it.selectedRoutine != null }
        assertEquals("Push A", s.selectedRoutine!!.name)
        assertEquals(6, s.selectedExerciseCount)
        assertEquals(5, s.plannedPerWeek)
        assertEquals(today, s.selected)
        assertEquals(today, s.weekStart)
    }

    @Test fun nextWeek_movesWeekStart() = runTest {
        val (vm, _) = vm()
        vm.await { it.selectedRoutine != null }
        vm.nextWeek()
        assertEquals(LocalDate.of(2026, 10, 19), vm.await { it.weekStart == LocalDate.of(2026, 10, 19) }.weekStart)
        vm.previousWeek(); vm.previousWeek()
        assertEquals(LocalDate.of(2026, 10, 5), vm.await { it.weekStart == LocalDate.of(2026, 10, 5) }.days.first())
    }

    @Test fun select_sunday_isRest() = runTest {
        val (vm, _) = vm()
        vm.await { it.selectedRoutine != null }
        vm.select(LocalDate.of(2026, 10, 18))
        assertNull(vm.await { it.selected == LocalDate.of(2026, 10, 18) }.selectedRoutine)
    }

    @Test fun finishedSessionToday_markerDoneAndSelectedSession() = runTest {
        val (vm, workouts) = vm()
        val s = workouts.startOrResume(ProgramRepository(db).todayDay(today)!!.id)
        workouts.confirmSet(s, db.exerciseDao().findByName("Incline Chest Press (Machine)")!!.id, 0, 0,
            SetDraft(80.0, 8, 2, SetType.WORKING), "")
        workouts.finish(s)
        val st = vm.await { it.markers.firstOrNull() == DayMarker.DONE }
        assertNotNull(st.selectedSession)
        assertEquals(1, st.thisWeekDone)
        assertEquals(1, st.totalWorkouts)
        assertEquals(1, st.streak)
        assertNotNull(st.last)
    }
}
