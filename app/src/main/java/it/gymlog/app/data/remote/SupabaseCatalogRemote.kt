package it.gymlog.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementazione reale del catalogo condiviso su Supabase. */
class SupabaseCatalogRemote(private val client: SupabaseClient) : CatalogRemote {
    private val table get() = client.postgrest["exercises"]

    @OptIn(SupabaseExperimental::class)
    override fun changes(): Flow<Unit> = client.from("exercises").selectAsFlow(Row::id).map { Unit }

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

    override suspend fun update(exercise: RemoteExercise): Boolean {
        // Solo le colonne concesse in UPDATE; `id` e `created_by` non si toccano.
        val rows = table.update({
            set("name", exercise.name)
            set("primary_muscle", exercise.primaryMuscle)
            set("secondary_muscles", exercise.secondaryMuscles)
            set("length_bias", exercise.lengthBias)
            set("single", exercise.single)
        }) {
            select()
            filter { eq("id", exercise.id) }
        }.decodeList<Row>()
        return rows.isNotEmpty()
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
        @SerialName("primary_muscle")
        val primaryMuscle: String,
        @SerialName("secondary_muscles")
        val secondaryMuscles: List<String>,
        @SerialName("length_bias")
        val lengthBias: String,
        val single: Boolean,
        @SerialName("created_by")
        val createdBy: String?,
    )

    @Serializable
    private data class Row(
        val id: String,
        val name: String,
        @SerialName("primary_muscle")
        val primaryMuscle: String,
        @SerialName("secondary_muscles")
        val secondaryMuscles: List<String> = emptyList(),
        @SerialName("length_bias")
        val lengthBias: String = "NONE",
        val single: Boolean = false,
        @SerialName("image_path") val imagePath: String? = null,
        @SerialName("created_by")
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
