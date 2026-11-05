package it.gymlog.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Exercise::class, ExerciseSecondaryMuscle::class, ExercisePhoto::class,
        Program::class, RoutineDay::class, RoutineExercise::class,
        WorkoutSession::class, SetEntry::class, WorkoutExerciseLayout::class,
        BodyEntry::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyDao(): BodyDao

    companion object {
        const val NAME = "gymlog.db"

        /** Solo ADD COLUMN: le righe esistenti non vengono né copiate né modificate. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Exercise ADD COLUMN single INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE SetEntry ADD COLUMN weightKgR REAL")
                db.execSQL("ALTER TABLE SetEntry ADD COLUMN repsR INTEGER")
                db.execSQL("ALTER TABLE SetEntry ADD COLUMN rirR INTEGER")
            }
        }

        /** Nomi degli esercizi unilaterali già presenti nel seed prima della 1.2.0. */
        val SEEDED_SINGLE = listOf(
            "Single Arm Triceps Pushdown (Cable)",
            "Single Lat Pulldown (Macchina)",
            "Single Hammer Curl (Cavo)",
            "Single Leg Extensions",
            "Single Leg Press (Machine)",
        )

        /** Superserie, sezione Corpo e flag unilaterali per gli esercizi già salvati. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE RoutineExercise ADD COLUMN supersetGroup INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `BodyEntry` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `weightKg` REAL, " +
                        "`neckCm` REAL, `chestCm` REAL, `waistCm` REAL, `hipsCm` REAL, " +
                        "`armCm` REAL, `thighCm` REAL, `calfCm` REAL, " +
                        "`skinChest` REAL, `skinMidaxillary` REAL, `skinTriceps` REAL, " +
                        "`skinSubscapular` REAL, `skinAbdomen` REAL, `skinSuprailiac` REAL, `skinThigh` REAL, " +
                        "`notes` TEXT NOT NULL, `measuredAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_BodyEntry_epochDay` ON `BodyEntry` (`epochDay`)")
                val names = SEEDED_SINGLE.joinToString(",") { "'${it.replace("'", "''")}'" }
                db.execSQL("UPDATE Exercise SET single = 1 WHERE name IN ($names)")
            }
        }

        /** Catalogo condiviso: solo colonne nuove e nullable, nessuna riga toccata. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Exercise ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE Exercise ADD COLUMN remoteCreatedBy TEXT")
                db.execSQL("ALTER TABLE Exercise ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE Exercise ADD COLUMN sharedImageKey TEXT")
                db.execSQL("ALTER TABLE Exercise ADD COLUMN pendingImageFile TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `WorkoutExerciseLayout` (`sessionId` INTEGER NOT NULL, `exerciseOrder` INTEGER NOT NULL, `rowCount` INTEGER NOT NULL, `prefillSlots` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`sessionId`, `exerciseOrder`), FOREIGN KEY(`sessionId`) REFERENCES `WorkoutSession`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
