package it.gymlog.app.data.sync

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.remote.CatalogRemote
import it.gymlog.app.data.remote.InsertResult
import it.gymlog.app.data.remote.RemoteExercise
import it.gymlog.app.testutil.inMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CatalogSyncTest {
    private lateinit var db: AppDatabase

    private val me = "00000000-0000-0000-0000-000000000002"
    private val other = "00000000-0000-0000-0000-000000000003"

    @get:org.junit.Rule val folder = TemporaryFolder()

    @Before fun setUp() {
        db = inMemoryDb()
    }

    @After fun tearDown() {
        db.close()
    }

    private fun sync(remote: FakeRemote, isAdmin: Boolean = false) =
        CatalogSync(db, remote, uid = me, isAdmin = isAdmin)

    private suspend fun local(
        name: String,
        remoteId: String? = null,
        createdBy: String? = null,
        notes: String = "",
    ): Exercise {
        val exercise = Exercise(
            name = name,
            primaryMuscle = Muscle.PETTO,
            executionNotes = notes,
            loadStepKg = 2.5,
            remoteId = remoteId,
            remoteCreatedBy = createdBy,
        )
        val id = db.exerciseDao().insert(exercise)
        return exercise.copy(id = id)
    }

    @Test fun pushNew_uploadsLocalOnlyAndRemembersRemoteId() = runTest {
        val exercise = local("Panca piana", notes = "scapole addotte")
        val remote = FakeRemote()

        val report = sync(remote).syncOnce()

        assertEquals(1, report.pushedNew)
        assertEquals(1, remote.rows.size)
        assertEquals("Panca piana", remote.rows.values.single().name)
        val saved = db.exerciseDao().getWithDetails(exercise.id)!!.exercise
        assertEquals(remote.rows.values.single().id, saved.remoteId)
        assertEquals(me, saved.remoteCreatedBy)
        assertFalse(saved.dirty)
    }

    @Test fun pushNew_duplicateNameIsLinkedByPullAndKeepsPersonalFields() = runTest {
        val exercise = local("Panca piana", notes = "le mie note")
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(
            id = "r-1", name = "panca piana", primaryMuscle = "PETTO",
            secondaryMuscles = listOf("TRICIPITI"), createdBy = other,
        )

        val report = sync(remote).syncOnce()

        assertEquals(1, report.linked)
        assertEquals(1, remote.rows.size)
        val saved = db.exerciseDao().getWithDetails(exercise.id)!!.exercise
        assertEquals("r-1", saved.remoteId)
        assertEquals("le mie note", saved.executionNotes)
        assertEquals(2.5, saved.loadStepKg!!, 0.0)
        assertEquals(listOf(Muscle.TRICIPITI), db.exerciseDao().secondaryFor(exercise.id))
    }

    @Test fun pull_addsUnknownRemoteExerciseWithSecondaryMuscles() = runTest {
        val remote = FakeRemote()
        remote.rows["r-2"] = RemoteExercise(
            id = "r-2", name = "Rematore", primaryMuscle = "DORSALI",
            secondaryMuscles = listOf("BICIPITI", "TRAPEZI"), lengthBias = "LENGTHENED",
            single = true, createdBy = other,
        )

        val report = sync(remote).syncOnce()

        assertEquals(1, report.added)
        val saved = db.exerciseDao().findByRemoteId("r-2")!!
        assertEquals(Muscle.DORSALI, saved.primaryMuscle)
        assertEquals(LengthBias.LENGTHENED, saved.lengthBias)
        assertTrue(saved.single)
        assertEquals(other, saved.remoteCreatedBy)
        assertEquals(
            setOf(Muscle.BICIPITI, Muscle.TRAPEZI),
            db.exerciseDao().secondaryFor(saved.id).toSet(),
        )
    }

    @Test fun pull_tombstoneArchivesButKeepsHistory() = runTest {
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = me)
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(
            id = "r-1", name = "Panca piana", primaryMuscle = "PETTO",
            createdBy = me, deleted = true,
        )

        val report = sync(remote).syncOnce()

        assertEquals(1, report.archived)
        assertEquals(1, db.exerciseDao().count())
        assertTrue(db.exerciseDao().findByRemoteId("r-1")!!.archived)
        assertEquals(exercise.name, db.exerciseDao().findByRemoteId("r-1")!!.name)
    }

    @Test fun pull_unknownMuscleIsSkippedInsteadOfBreakingTheSync() = runTest {
        val remote = FakeRemote()
        remote.rows["r-3"] = RemoteExercise(
            id = "r-3", name = "Esercizio futuro", primaryMuscle = "SPALLE",
        )

        val report = sync(remote).syncOnce()

        assertEquals(1, report.skippedRows)
        assertEquals(0, db.exerciseDao().count())
    }

    @Test fun pushUpdates_refusedForAnotherUsersExercise() = runTest {
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = other)
        db.exerciseDao().update(db.exerciseDao().getWithDetails(exercise.id)!!.exercise.copy(dirty = true))
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(id = "r-1", name = "Panca piana", primaryMuscle = "PETTO", createdBy = other)

        val report = sync(remote).syncOnce()

        assertEquals(1, report.refused)
        assertFalse(db.exerciseDao().getWithDetails(exercise.id)!!.exercise.dirty)
        assertEquals("Panca piana", remote.rows.getValue("r-1").name)
    }

    @Test fun pushUpdates_creatorSendsSharedFields() = runTest {
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = me)
        db.exerciseDao().update(db.exerciseDao().getWithDetails(exercise.id)!!.exercise.copy(name = "Panca piana (bilanciere)", dirty = true))
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(id = "r-1", name = "Panca piana", primaryMuscle = "PETTO", createdBy = me)

        val report = sync(remote).syncOnce()

        assertEquals(1, report.pushedUpdates)
        assertEquals("Panca piana (bilanciere)", remote.rows.getValue("r-1").name)
        assertFalse(db.exerciseDao().getWithDetails(exercise.id)!!.exercise.dirty)
    }

    @Test fun pushImages_setsSharedImageWhenNobodyWasFirst() = runTest {
        val file = folder.newFile("img.jpg")
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = me)
        db.exerciseDao().update(
            db.exerciseDao().getWithDetails(exercise.id)!!.exercise.copy(pendingImageFile = file.path)
        )
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(id = "r-1", name = "Panca piana", primaryMuscle = "PETTO", createdBy = me)

        val report = CatalogSync(db, remote, me, isAdmin = false).syncOnce()

        assertEquals(1, report.pushedImages)
        assertEquals("img/path.jpg", remote.rows.getValue("r-1").imagePath)
        val saved = db.exerciseDao().getWithDetails(exercise.id)!!.exercise
        assertEquals("img/path.jpg", saved.sharedImageKey)
        assertNull(saved.pendingImageFile)
    }

    @Test fun pushImages_lostRaceClearsPendingImage() = runTest {
        val file = folder.newFile("img.jpg")
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = me)
        db.exerciseDao().update(
            db.exerciseDao().getWithDetails(exercise.id)!!.exercise.copy(pendingImageFile = file.path)
        )
        val remote = FakeRemote(acceptImage = false)

        val report = sync(remote).syncOnce()

        assertEquals(1, report.imagesLost)
        val saved = db.exerciseDao().getWithDetails(exercise.id)!!.exercise
        assertNull(saved.sharedImageKey)
        assertNull(saved.pendingImageFile)
    }

    @Test fun pushImages_doesNotUploadWhenSomeoneElseAlreadySetIt() = runTest {
        val file = folder.newFile("img.jpg")
        val exercise = local("Panca piana", remoteId = "r-1", createdBy = me)
        db.exerciseDao().update(
            db.exerciseDao().getWithDetails(exercise.id)!!.exercise.copy(
                sharedImageKey = "altro/immagine.jpg",
                pendingImageFile = file.path,
            )
        )
        val remote = FakeRemote()
        remote.rows["r-1"] = RemoteExercise(
            id = "r-1", name = "Panca piana", primaryMuscle = "PETTO",
            imagePath = "altro/immagine.jpg", createdBy = me,
        )

        val report = sync(remote).syncOnce()

        assertEquals(1, report.imagesLost)
        assertEquals(0, remote.uploads)
        assertNull(db.exerciseDao().getWithDetails(exercise.id)!!.exercise.pendingImageFile)
    }

    private class FakeRemote(private val acceptImage: Boolean = true) : CatalogRemote {
        val rows = linkedMapOf<String, RemoteExercise>()
        var uploads = 0

        override suspend fun pull(): List<RemoteExercise> = rows.values.toList()

        override suspend fun insert(exercise: RemoteExercise): InsertResult {
            if (rows.values.any { it.name.equals(exercise.name, ignoreCase = true) && !it.deleted }) {
                return InsertResult.DuplicateName
            }
            rows[exercise.id] = exercise
            return InsertResult.Inserted
        }

        override suspend fun update(exercise: RemoteExercise) {
            val old = rows.getValue(exercise.id)
            // Il server non lascia toccare image_path da un update dei metadati.
            rows[exercise.id] = exercise.copy(imagePath = old.imagePath, deleted = old.deleted)
        }

        override suspend fun uploadImage(file: File, remoteId: String): String {
            uploads++
            return "img/path.jpg"
        }

        override suspend fun setImage(remoteId: String, path: String): Boolean {
            if (!acceptImage) return false
            rows[remoteId] = rows.getValue(remoteId).copy(imagePath = path)
            return true
        }

        override suspend fun clearImage(remoteId: String) {
            rows[remoteId] = rows.getValue(remoteId).copy(imagePath = null)
        }

        override suspend fun delete(remoteId: String) {
            rows[remoteId] = rows.getValue(remoteId).copy(deleted = true)
        }
    }
}
