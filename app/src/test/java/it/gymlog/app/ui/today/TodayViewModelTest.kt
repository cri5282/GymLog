package it.gymlog.app.ui.today

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.repo.ProgramRepository
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    @Test fun doubleTapStart_navigatesOnce() = runTest {
        SeedData.seedIfEmpty(db)
        val vm = TodayViewModel(ProgramRepository(db), WorkoutRepository(db, testSettings(tmp.root, backgroundScope)))
        var calls = 0
        vm.start(null) { calls++ }
        vm.start(null) { calls++ }
        withContext(Dispatchers.Default.limitedParallelism(1)) { delay(500) }
        assertEquals(1, calls)
    }
}
