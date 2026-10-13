package it.gymlog.app.ui.history

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.data.db.SetType
import it.gymlog.app.data.repo.WorkoutRepository
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.testutil.inMemoryDb
import it.gymlog.app.testutil.testSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionDetailViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); db = inMemoryDb() }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    private suspend fun TestScope.setup(): Pair<WorkoutRepository, Long> {
        SeedData.seedIfEmpty(db)
        val repo = WorkoutRepository(db, testSettings(tmp.root, backgroundScope))
        val s = repo.startOrResume(null)
        val ex = db.exerciseDao().findByName("Lat Pulldown (Machine)")!!.id
        repo.confirmSet(s, ex, 0, 0, SetDraft(80.0, 8, 2, SetType.WORKING), "")
        repo.confirmSet(s, ex, 0, 1, SetDraft(80.0, 7, 1, SetType.WORKING), "")
        repo.finish(s)
        return repo to s
    }

    private suspend fun SessionDetailViewModel.await(cond: (SessionDetailState) -> Boolean) =
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { state.first(cond) } }

    @Test fun deleteLastSet_keepsSession() = runTest {
        val (repo, s) = setup()
        val vm = SessionDetailViewModel(s, repo)
        val sets = vm.await { it.groups.isNotEmpty() }.groups.single().sets
        vm.deleteSet(sets[0]); vm.deleteSet(sets[1])
        vm.await { it.groups.isEmpty() }
        assertTrue(repo.getSession(s) != null)
    }

    @Test fun deleteSession_removesSets() = runTest {
        val (repo, s) = setup()
        val vm = SessionDetailViewModel(s, repo)
        vm.await { it.session != null }
        var done = false
        vm.deleteSession { done = true }
        withContext(Dispatchers.Default.limitedParallelism(1)) { withTimeout(10_000) { while (!done) kotlinx.coroutines.delay(20) } }
        assertNull(repo.getSession(s))
        assertTrue(db.workoutDao().getSets(s).isEmpty())
    }

    @Test fun editSet_updatesValues() = runTest {
        val (repo, s) = setup()
        val vm = SessionDetailViewModel(s, repo)
        val first = vm.await { it.groups.isNotEmpty() }.groups.single().sets[0]
        vm.updateSet(first.copy(weightKg = 82.5, reps = 7))
        val updated = vm.await { it.groups.single().sets[0].weightKg == 82.5 }.groups.single().sets[0]
        assertEquals(7, updated.reps)
        assertEquals("Lat Pulldown (Machine)", vm.state.value.groups.single().exercise.name)
    }
}
