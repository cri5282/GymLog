package it.gymlog.app.ui.stats

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.db.WorkoutSession
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
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
class StatsViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private val zone = ZoneId.of("Europe/Rome")
    private val today = LocalDate.of(2026, 10, 14)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private fun ms(dt: String) = LocalDateTime.parse(dt).atZone(zone).toInstant().toEpochMilli()

    private suspend fun session(start: String, working: Int = 1, warmup: Int = 0): Long {
        val dao = db.workoutDao()
        val s = dao.insertSession(WorkoutSession(title = start, startedAt = ms(start), endedAt = ms(start) + 3_600_000))
        val ex = db.exerciseDao().findByName("Lat Pulldown (Machine)")!!.id
        repeat(working + warmup) { i ->
            dao.insertSet(SetEntry(sessionId = s, exerciseId = ex, exerciseOrder = 0, setIndex = i,
                setType = if (i < warmup) SetType.WARMUP else SetType.WORKING, weightKg = 50.0, reps = 10,
                completedAt = ms(start) + 60_000))
        }
        return s
    }

    private suspend fun StatsViewModel.await(cond: (StatsState) -> Boolean) =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { state.first(cond) } }

    @Test fun stats_countsTotalsMonthAndWeek() = runTest {
        SeedData.seedIfEmpty(db)
        session("2026-10-02T18:00")
        session("2026-10-12T18:00", working = 3, warmup = 1)
        session("2026-09-28T18:00")
        val vm = StatsViewModel(WorkoutRepository(db, testSettings(tmp.root, backgroundScope)), today = { today }, zone = zone)
        val s = vm.await { it.total == 3 }
        assertEquals(2, s.thisMonth)
        assertEquals(3, vm.await { it.setsThisWeek == 3 }.setsThisWeek)
        assertEquals(3, s.levels.size)
    }

    @Test fun stats_recentMaxFiveNewestFirst() = runTest {
        SeedData.seedIfEmpty(db)
        (1..7).forEach { session("2026-10-0${it}T18:00") }
        val vm = StatsViewModel(WorkoutRepository(db, testSettings(tmp.root, backgroundScope)), today = { today }, zone = zone)
        val s = vm.await { it.total == 7 }
        assertEquals(5, s.recent.size)
        assertEquals("2026-10-07T18:00", s.recent.first().title)
    }

    @Test fun refreshDay_newWeek_resetsSetsThisWeek() = runTest {
        SeedData.seedIfEmpty(db)
        session("2026-10-12T18:00", working = 3)
        var now = today
        val vm = StatsViewModel(WorkoutRepository(db, testSettings(tmp.root, backgroundScope)), today = { now }, zone = zone)
        vm.await { it.setsThisWeek == 3 }
        now = LocalDate.of(2026, 10, 20)
        vm.refreshDay()
        assertEquals(0, vm.await { it.today == now }.setsThisWeek)
    }
}
