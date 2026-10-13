package it.gymlog.app.testutil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.gymlog.app.data.db.AppDatabase

fun inMemoryDb(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
