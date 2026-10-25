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
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class BackupExporterTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        context = TestContext(ApplicationProvider.getApplicationContext(), tmp.root)
        db = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).allowMainThreadQueries().build()
    }
    @After fun tearDown() { db.close() }

    @Test fun zipContainsDatabaseWithDataPhotosAndSettings() = runTest {
        db.exerciseDao().insert(Exercise(name = "Panca piana", primaryMuscle = Muscle.PETTO))
        File(context.filesDir, "photos").mkdirs()
        File(context.filesDir, "photos/a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(context.filesDir, "datastore").mkdirs()
        File(context.filesDir, "datastore/settings.preferences_pb").writeBytes(byteArrayOf(9))

        val out = ByteArrayOutputStream()
        BackupExporter(context, db).export(out)

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { z ->
            generateSequence { z.nextEntry }.forEach { entries[it.name] = z.readBytes() }
        }
        assertEquals(setOf("gymlog.db", "photos/a.jpg", "datastore/settings.preferences_pb"), entries.keys)
        assertEquals(listOf<Byte>(1, 2, 3), entries.getValue("photos/a.jpg").toList())

        // Il DB nello zip, aperto da solo, contiene l'esercizio.
        val restored = File(context.cacheDir, "restored.db").apply { writeBytes(entries.getValue("gymlog.db")) }
        android.database.sqlite.SQLiteDatabase.openDatabase(restored.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use {
            it.rawQuery("SELECT COUNT(*) FROM Exercise WHERE name = 'Panca piana'", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
            }
        }
    }
}
