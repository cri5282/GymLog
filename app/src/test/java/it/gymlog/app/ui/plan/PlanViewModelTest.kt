package it.gymlog.app.ui.plan

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.testutil.inMemoryDb
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlanViewModelTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private suspend fun PlanViewModel.await(cond: (PlanState) -> Boolean) =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { state.first(cond) } }

    private suspend fun vm(): PlanViewModel { SeedData.seedIfEmpty(db); return PlanViewModel(ProgramRepository(db)) }

    @Test fun week_matchesSeed() = runTest {
        val s = vm().await { it.routines.size == 5 }
        assertEquals(listOf("Push A", "Pull A", "Legs", "Push B", "Pull B", null, null), s.week.map { it?.name })
    }

    @Test fun assign_saturdayToLegs_movesLegs() = runTest {
        val vm = vm()
        val legs = vm.await { it.routines.size == 5 }.routines.first { it.day.name == "Legs" }.day.id
        vm.assign(6, legs)
        val s = vm.await { it.week[5]?.name == "Legs" }
        assertEquals(null, s.week[2])
    }

    @Test fun routines_haveExerciseCounts() = runTest {
        assertEquals(listOf(6, 6, 7, 6, 8), vm().await { it.routines.size == 5 }.routines.map { it.exerciseCount })
    }

    @Test fun createProgram_thenSelect_switchesWeek() = runTest {
        val vm = vm()
        vm.await { it.routines.size == 5 }
        vm.createProgram("Forza")
        val s = vm.await { it.program?.name == "Forza" }
        assertEquals(List(7) { null }, s.week)
        assertEquals(2, s.programs.size)
    }
}
