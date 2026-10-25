package it.gymlog.app.data.repo

import androidx.room.withTransaction
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.ExercisePhoto
import it.gymlog.app.data.db.ExerciseWithDetails
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

data class ExerciseDraft(
    val id: Long = 0,
    val name: String,
    val primary: Muscle,
    val secondary: List<Muscle>,
    val bias: LengthBias,
    val notes: String,
    val hevyName: String?,
    val loadStepKg: Double?,
    val single: Boolean = false,
)

enum class DeleteResult { DELETED, ARCHIVED }

class DuplicateNameException(name: String) : Exception("Esiste già un esercizio chiamato \"$name\"")

class DuplicateHevyNameException(hevyName: String, usedBy: String) :
    Exception("Il nome Hevy \"$hevyName\" è già usato da \"$usedBy\"")

class ExerciseRepository(private val db: AppDatabase, private val photos: PhotoStore) {
    private val dao = db.exerciseDao()

    fun observeLibrary(query: String, muscle: Muscle?): Flow<List<ExerciseWithDetails>> =
        dao.observeActive().map { list ->
            list.filter { e ->
                (query.isBlank() || e.exercise.name.contains(query.trim(), ignoreCase = true)) &&
                    (muscle == null || e.exercise.primaryMuscle == muscle)
            }
        }

    suspend fun get(id: Long): ExerciseWithDetails? = dao.getWithDetails(id)

    fun observe(id: Long): Flow<ExerciseWithDetails?> = dao.observeWithDetails(id)

    /** Crea o aggiorna; lancia [DuplicateNameException] se il nome è già usato da un altro esercizio. */
    suspend fun save(draft: ExerciseDraft): Long = db.withTransaction {
        val name = draft.name.trim()
        require(name.isNotEmpty()) { "Il nome è obbligatorio" }
        dao.findByName(name)?.let { if (it.id != draft.id) throw DuplicateNameException(name) }
        val hevyName = draft.hevyName?.trim()?.ifEmpty { null }
        hevyName?.let { h -> dao.findByHevyName(h)?.let { if (it.id != draft.id) throw DuplicateHevyNameException(h, it.name) } }
        val existing = if (draft.id != 0L) dao.getWithDetails(draft.id)?.exercise else null
        val exercise = Exercise(
            id = draft.id,
            name = name,
            primaryMuscle = draft.primary,
            lengthBias = draft.bias,
            executionNotes = draft.notes.trim(),
            hevyName = hevyName,
            loadStepKg = draft.loadStepKg,
            archived = existing?.archived ?: false,
            needsReview = false,
            single = draft.single,
        )
        val id = if (existing == null) dao.insert(exercise.copy(id = 0)) else { dao.update(exercise); draft.id }
        dao.replaceSecondary(id, draft.secondary.filter { it != draft.primary })
        id
    }

    suspend fun isUsed(id: Long): Boolean = dao.isUsed(id)

    suspend fun deleteOrArchive(id: Long): DeleteResult {
        val details = dao.getWithDetails(id) ?: return DeleteResult.DELETED
        return if (dao.isUsed(id)) {
            dao.update(details.exercise.copy(archived = true))
            DeleteResult.ARCHIVED
        } else {
            details.photos.forEach { photos.delete(it.filePath) }
            dao.delete(details.exercise)
            DeleteResult.DELETED
        }
    }

    suspend fun addPhoto(exerciseId: Long, file: File) {
        val next = (dao.getWithDetails(exerciseId)?.photos?.maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.insertPhoto(ExercisePhoto(exerciseId = exerciseId, filePath = file.path, sortOrder = next))
    }

    suspend fun removePhoto(photo: ExercisePhoto) {
        dao.deletePhoto(photo)
        photos.delete(photo.filePath)
    }

    suspend fun movePhoto(photo: ExercisePhoto, up: Boolean) {
        val list = dao.getWithDetails(photo.exerciseId)?.sortedPhotos ?: return
        val i = list.indexOfFirst { it.id == photo.id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        val reordered = list.toMutableList().apply { add(j, removeAt(i)) }
        dao.updatePhotos(reordered.mapIndexed { index, p -> p.copy(sortOrder = index) })
    }
}
