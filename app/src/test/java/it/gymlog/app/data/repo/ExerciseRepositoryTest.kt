package it.gymlog.app.data.repo

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SeedData
import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var repo: ExerciseRepository
    private lateinit var photos: PhotoStore

    @Before fun setUp() {
        db = inMemoryDb()
        photos = PhotoStore(tmp.root)
        repo = ExerciseRepository(db, photos)
    }
    @After fun tearDown() { db.close() }

    private fun draft(name: String = "Pec Deck") = ExerciseDraft(
        name = name, primary = Muscle.PETTO, secondary = listOf(Muscle.DELTOIDE_ANTERIORE),
        bias = LengthBias.LENGTHENED, notes = "lento", hevyName = null, loadStepKg = null,
    )

    @Test fun save_new_thenEdit_updatesFieldsAndSecondary() = runTest {
        val id = repo.save(draft())
        repo.save(draft().copy(id = id, name = "Pec Deck 2", secondary = listOf(Muscle.TRICIPITI), loadStepKg = 1.25))
        val e = repo.get(id)!!
        assertEquals("Pec Deck 2", e.exercise.name)
        assertEquals(1.25, e.exercise.loadStepKg!!, 1e-9)
        assertEquals(listOf(Muscle.TRICIPITI), e.secondary.map { it.muscle })
    }

    @Test fun save_clearsNeedsReview() = runTest {
        val id = db.exerciseDao().insert(it.gymlog.app.data.db.Exercise(name = "X", primaryMuscle = Muscle.ADDOME, needsReview = true))
        repo.save(draft("X").copy(id = id))
        assertFalse(repo.get(id)!!.exercise.needsReview)
    }

    @Test(expected = DuplicateNameException::class)
    fun save_duplicateName_throwsDuplicateNameException() = runTest {
        repo.save(draft("Panca"))
        repo.save(draft("panca"))
    }

    @Test fun save_blankHevyName_storedAsNull() = runTest {
        repo.save(draft("A").copy(hevyName = "  "))
        repo.save(draft("B").copy(hevyName = ""))
        assertNull(db.exerciseDao().findByName("A")!!.hevyName)
    }

    @Test fun deleteOrArchive_unused_deletesAndRemovesPhotoFiles() = runTest {
        val id = repo.save(draft())
        val file = photos.newCameraFile().apply { writeText("jpg") }
        repo.addPhoto(id, file)
        assertEquals(DeleteResult.DELETED, repo.deleteOrArchive(id))
        assertNull(repo.get(id))
        assertFalse(file.exists())
    }

    @Test fun deleteOrArchive_used_archives() = runTest {
        SeedData.seedIfEmpty(db)
        val id = db.exerciseDao().findByName("Chest Fly (Machine)")!!.id
        assertEquals(DeleteResult.ARCHIVED, repo.deleteOrArchive(id))
        assertTrue(repo.get(id)!!.exercise.archived)
    }

    @Test fun observeLibrary_filtersByQueryCaseInsensitiveAndMuscle() = runTest {
        SeedData.seedIfEmpty(db)
        val names = repo.observeLibrary("pull", Muscle.DORSALI).first().map { it.exercise.name }
        assertEquals(listOf("Lat Pulldown (Machine)", "Single Lat Pulldown (Macchina)"), names)
    }

    @Test fun movePhoto_swapsOrder() = runTest {
        val id = repo.save(draft())
        val a = photos.newCameraFile().apply { writeText("a") }
        val b = photos.newCameraFile().apply { writeText("b") }
        repo.addPhoto(id, a); repo.addPhoto(id, b)
        val second = repo.get(id)!!.sortedPhotos[1]
        repo.movePhoto(second, up = true)
        assertEquals(b.path, repo.get(id)!!.sortedPhotos[0].filePath)
    }

    @Test(expected = DuplicateHevyNameException::class)
    fun save_duplicateHevyName_throwsTypedException() = runTest {
        repo.save(draft("A").copy(hevyName = "Chest Press"))
        repo.save(draft("B").copy(hevyName = "Chest Press"))
    }

    @Test fun save_persistsSingleFlag_andKeepsItOnEdit() = runTest {
        val id = repo.save(draft().copy(single = true))
        assertTrue(repo.get(id)!!.exercise.single)
        repo.save(draft().copy(id = id, single = false))
        assertFalse(repo.get(id)!!.exercise.single)
    }
}
