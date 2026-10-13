package it.gymlog.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Exercise::class, ExerciseSecondaryMuscle::class, ExercisePhoto::class,
        Program::class, RoutineDay::class, RoutineExercise::class,
        WorkoutSession::class, SetEntry::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "gymlog.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
