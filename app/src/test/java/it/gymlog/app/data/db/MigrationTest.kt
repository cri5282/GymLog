package it.gymlog.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Garantisce che aggiornare dalla versione 1 non perda né alteri nessun dato esistente. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val tables = listOf(
        "Exercise", "ExerciseSecondaryMuscle", "ExercisePhoto", "Program",
        "RoutineDay", "RoutineExercise", "WorkoutSession", "SetEntry",
    )

    private fun seedV1(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO Exercise (id, name, primaryMuscle, lengthBias, executionNotes, hevyName, loadStepKg, archived, needsReview) VALUES " +
            "(1, 'Panca piana', 'PETTO', 'NONE', 'Scapole addotte', 'Bench Press (Barbell)', 2.5, 0, 0)," +
            "(2, 'Curl manubri', 'BICIPITI', 'LENGTHENED', '', NULL, NULL, 1, 1)")
        db.execSQL("INSERT INTO ExerciseSecondaryMuscle (exerciseId, muscle) VALUES (1, 'TRICIPITI'), (1, 'DELTOIDE_ANTERIORE')")
        db.execSQL("INSERT INTO ExercisePhoto (id, exerciseId, filePath, sortOrder) VALUES (1, 1, '/data/files/photos/a.jpg', 0), (2, 1, '/data/files/photos/b.jpg', 1)")
        db.execSQL("INSERT INTO Program (id, name, isActive) VALUES (1, 'Push Pull Legs', 1)")
        db.execSQL("INSERT INTO RoutineDay (id, programId, name, dayOfWeek, sortOrder) VALUES (1, 1, 'Push', 1, 0), (2, 1, 'Pull', NULL, 1)")
        db.execSQL("INSERT INTO RoutineExercise (id, routineDayId, exerciseId, sortOrder, targetSets, repMin, repMax, targetRirMin, targetRirMax, restSeconds, notes) VALUES " +
            "(1, 1, 1, 0, 4, 6, 10, 1, 2, 150, 'pausa 1s')")
        db.execSQL("INSERT INTO WorkoutSession (id, routineDayId, title, startedAt, endedAt, notes, source) VALUES " +
            "(1, 1, 'Push', 1700000000000, 1700003600000, 'ottima sessione', 'MANUAL')," +
            "(2, NULL, 'Allenamento libero', 1700100000000, NULL, '', 'HEVY_IMPORT')")
        db.execSQL("INSERT INTO SetEntry (id, sessionId, exerciseId, exerciseOrder, setIndex, setType, weightKg, reps, rir, notes, completedAt) VALUES " +
            "(1, 1, 1, 0, 0, 'WARMUP', 40.0, 10, NULL, '', 1700000100000)," +
            "(2, 1, 1, 0, 1, 'WORKING', 82.5, 8, 2, 'facile', 1700000400000)," +
            "(3, 1, 1, 0, 2, 'WORKING', 82.5, 7, 1, '', 1700000700000)")
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info($table)").use { c -> generateSequence { if (c.moveToNext()) c.getString(1) else null }.toList() }

    /** Contenuto di una tabella come testo, riga per riga, solo sulle [cols] indicate. */
    private fun dump(db: SupportSQLiteDatabase, table: String, cols: List<String>): List<List<String?>> =
        db.query("SELECT ${cols.joinToString()} FROM $table ORDER BY rowid").use { c ->
            generateSequence { if (c.moveToNext()) cols.indices.map { i -> if (c.isNull(i)) null else c.getString(i) } else null }.toList()
        }

    @Test fun migrate1To2_keepsEveryExistingRowUnchanged() {
        val before = mutableMapOf<String, List<List<String?>>>()
        val oldColumns = mutableMapOf<String, List<String>>()
        helper.createDatabase("migration-test", 1).use { db ->
            seedV1(db)
            for (t in tables) {
                oldColumns[t] = columns(db, t)
                before[t] = dump(db, t, oldColumns.getValue(t))
            }
        }
        assertTrue(before.values.all { it.isNotEmpty() })

        val migrated = helper.runMigrationsAndValidate("migration-test", 2, true, AppDatabase.MIGRATION_1_2)
        for (t in tables) {
            assertEquals("tabella $t", before.getValue(t), dump(migrated, t, oldColumns.getValue(t)))
        }

        // Nuove colonne: esercizi bilaterali, serie senza lato destro.
        assertEquals(listOf(listOf("0"), listOf("0")), dump(migrated, "Exercise", listOf("single")))
        assertEquals(List(3) { listOf(null, null, null) }, dump(migrated, "SetEntry", listOf("weightKgR", "repsR", "rirR")))
        migrated.close()
    }

    @Test fun migrate2To3_addsBodyAndSupersetAndSingle() {
        helper.createDatabase("migration-2-3", 2).use { db ->
            db.execSQL("INSERT INTO Exercise (id, name, primaryMuscle, lengthBias, executionNotes, hevyName, loadStepKg, archived, needsReview, single) VALUES (1, 'Single Leg Extensions', 'QUADRICIPITI', 'NONE', '', NULL, 2.5, 0, 0, 0)")
        }
        val migrated = helper.runMigrationsAndValidate("migration-2-3", 3, true, AppDatabase.MIGRATION_2_3)
        assertEquals(listOf(listOf("1")), dump(migrated, "Exercise", listOf("single")))
        assertEquals(emptyList<List<String?>>(), dump(migrated, "BodyEntry", listOf("epochDay")))
        migrated.close()
    }

    @Test fun migrate1To2_emptyDatabase() {
        helper.createDatabase("migration-empty", 1).close()
        helper.runMigrationsAndValidate("migration-empty", 2, true, AppDatabase.MIGRATION_1_2).close()
    }
}
