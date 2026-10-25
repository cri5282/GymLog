package it.gymlog.app.data.repo

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.BodyEntry
import kotlinx.coroutines.flow.Flow

/** Misurazioni facoltative di peso, circonferenze e pliche. */
class BodyRepository(private val db: AppDatabase) {
    private val dao = db.bodyDao()

    fun observeAll(): Flow<List<BodyEntry>> = dao.observeAll()
    suspend fun all(): List<BodyEntry> = dao.all()
    suspend fun latest(): BodyEntry? = dao.latest()
    suspend fun save(entry: BodyEntry): Long = dao.upsert(entry)
    suspend fun delete(entry: BodyEntry) = dao.delete(entry)
}
