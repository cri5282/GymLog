package it.gymlog.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

/** Implementazione reale del catalogo condiviso su Supabase. */
class SupabaseCatalogRemote(private val client: SupabaseClient) : CatalogRemote {
    private val table get() = client.postgrest["exercises"]

    override suspend fun pull(): List<RemoteExercise> =
        table.select().decodeList<Row>().map { it.toModel() }

    override suspend fun insert(exercise: RemoteExercise): InsertResult =
        try {
            table.insert(
                NewRow(
                    id = exercise.id,
                    name = exercise.name,
                    primaryMuscle = exercise.primaryMuscle,
                    secondaryMuscles = exercise.secondaryMuscles,
                    lengthBias = exercise.lengthBias,
                    single = exercise.single,
                    createdBy = exercise.createdBy,
                )
            )
            InsertResult.Inserted
        } catch (e: PostgrestRestException) {
            // Violazione dell'indice unico sul nome: la pull collega la riga già esistente.
            if (e.code == "23505") InsertResult.DuplicateName else throw e
        }

    override suspend fun update(exercise: RemoteExercise) {
        // Solo le colonne concesse in UPDATE; `id` e `created_by` non si toccano.
        table.update({
            set("name", exercise.name)
            set("primary_muscle", exercise.primaryMuscle)
            set("secondary_muscles", exercise.secondaryMuscles)
            set("length_bias", exercise.lengthBias)
            set("single", exercise.single)
        }) {
            filter { eq("id", exercise.id) }
        }
    }

    override suspend fun uploadImage(file: File, remoteId: String): String {
        val uid = client.auth.currentUserOrNull()?.id ?: error("Nessun account autenticato")
        // Il percorso deve rispettare la regola del server: <uid>/<uuid>.jpg.
        val path = "$uid/${UUID.randomUUID()}.jpg"
        client.storage.from(BUCKET).upload(path, file.readBytes()) { upsert = false }
        return path
    }

    override suspend fun setImage(remoteId: String, path: String): Boolean =
        client.postgrest.rpc("set_exercise_image", buildJsonObject {
            put("p_exercise", remoteId)
            put("p_path", path)
        }).decodeAs<Boolean>()

    override suspend fun clearImage(remoteId: String) {
        client.postgrest.rpc("clear_exercise_image", buildJsonObject {
            put("p_exercise", remoteId)
        })
    }

    override suspend fun delete(remoteId: String) {
        client.postgrest.rpc("admin_delete_exercise", buildJsonObject {
            put("p_exercise", remoteId)
        })
    }

    private companion object {
        const val BUCKET = "exercise-images"
    }

    /** Colonne permesse in INSERT. `null` non viene inviato perché la colonna non è concessa. */
    @Serializable
    private data class NewRow(
        val id: String,
        val name: String,
        val primaryMuscle: String,
        val secondaryMuscles: List<String>,
        val lengthBias: String,
        val single: Boolean,
        val createdBy: String?,
    )

    @Serializable
    private data class Row(
        val id: String,
        val name: String,
        val primaryMuscle: String,
        val secondaryMuscles: List<String> = emptyList(),
        val lengthBias: String = "NONE",
        val single: Boolean = false,
        val imagePath: String? = null,
        val createdBy: String? = null,
        @SerialName("deleted_at") val deletedAt: String? = null,
    ) {
        fun toModel() = RemoteExercise(
            id = id,
            name = name,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            lengthBias = lengthBias,
            single = single,
            imagePath = imagePath,
            createdBy = createdBy,
            deleted = deletedAt != null,
        )
    }
}
