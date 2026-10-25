package it.gymlog.app.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.testutil.TestContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupRestoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        context = TestContext(ApplicationProvider.getApplicationContext(), tmp.root)
        db = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).allowMainThreadQueries().build()
    }

    @After fun tearDown() {
        runCatching { db.close() }
    }

    @Test fun restore_replacesDatabasePhotosAndSettings() = runTest {
        db.exerciseDao().insert(Exercise(name = "Prima", primaryMuscle = Muscle.PETTO))
        File(context.filesDir, "photos").mkdirs()
        File(context.filesDir, "photos/vecchia.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(context.filesDir, "datastore").mkdirs()
        File(context.filesDir, "datastore/settings.preferences_pb").writeBytes(byteArrayOf(9))
        val out = ByteArrayOutputStream()
        BackupExporter(context, db).export(out)

        // Dopo il backup i dati cambiano: il ripristino deve riportare quelli salvati.
        db.exerciseDao().insert(Exercise(name = "Dopo", primaryMuscle = Muscle.DORSALI))
        File(context.filesDir, "photos/nuova.jpg").writeBytes(byteArrayOf(4))

        val files = BackupExporter(context, db).restore(ByteArrayInputStream(out.toByteArray()))
        assertEquals(3, files)
        assertFalse(File(context.filesDir, "photos/nuova.jpg").exists())
        assertEquals(listOf<Byte>(1, 2, 3), File(context.filesDir, "photos/vecchia.jpg").readBytes().toList())
        assertEquals(listOf<Byte>(9), File(context.filesDir, "datastore/settings.preferences_pb").readBytes().toList())

        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .allowMainThreadQueries().build()
        try {
            assertTrue(reopened.exerciseDao().findByName("Prima") != null)
            assertTrue(reopened.exerciseDao().findByName("Dopo") == null)
        } finally {
            reopened.close()
        }
    }

    @Test(expected = RestoreException::class)
    fun restore_rejectsZipWithoutDatabaseWithoutTouchingData() = runTest {
        db.exerciseDao().insert(Exercise(name = "Prima", primaryMuscle = Muscle.PETTO))
        val zip = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(zip).use { z ->
            z.putNextEntry(java.util.zip.ZipEntry("readme.txt"))
            z.write("niente database".toByteArray())
            z.closeEntry()
        }
        try {
            BackupExporter(context, db).restore(ByteArrayInputStream(zip.toByteArray()))
        } finally {
            // La validazione avviene prima di chiudere il database: i dati sono intatti.
            assertTrue(db.exerciseDao().findByName("Prima") != null)
        }
    }
}
