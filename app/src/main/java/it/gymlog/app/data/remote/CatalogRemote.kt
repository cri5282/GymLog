package it.gymlog.app.data.remote

import java.io.File

/**
 * Una riga del catalogo condiviso: solo i campi che escono dal telefono.
 * Note di esecuzione, passo di carico, hevyName e foto personali non esistono qui.
 */
data class RemoteExercise(
    val id: String,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val lengthBias: String = "NONE",
    val single: Boolean = false,
    val imagePath: String? = null,
    val createdBy: String? = null,
    val deleted: Boolean = false,
)

sealed interface InsertResult {
    data object Inserted : InsertResult

    /** Nome già nel catalogo: la pull successiva collega l'esercizio esistente. */
    data object DuplicateName : InsertResult
}

/** Porta verso Supabase. L'implementazione reale sta in `SupabaseCatalogRemote`. */
interface CatalogRemote {
    /** Tutto il catalogo, cancellati compresi: poche centinaia di righe, una richiesta. */
    suspend fun pull(): List<RemoteExercise>

    suspend fun insert(exercise: RemoteExercise): InsertResult

    suspend fun update(exercise: RemoteExercise)

    /** Carica il JPEG e restituisce la chiave nel bucket. */
    suspend fun uploadImage(file: File, remoteId: String): String

    /** `true` se l'immagine è stata impostata, `false` se qualcuno è arrivato prima. */
    suspend fun setImage(remoteId: String, path: String): Boolean

    suspend fun clearImage(remoteId: String)

    suspend fun delete(remoteId: String)
}
