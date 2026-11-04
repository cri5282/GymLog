package it.gymlog.app.data.sync

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.Exercise
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.remote.CatalogRemote
import it.gymlog.app.data.remote.InsertResult
import it.gymlog.app.data.remote.RemoteExercise
import java.io.File
import java.util.UUID

/**
 * Scambio del catalogo condiviso, senza rete nel percorso dell'allenamento.
 *
 * Pull a snapshot completo: il catalogo è di poche centinaia di righe e una richiesta evita
 * cursori, paginazione e righe perse con lo stesso timestamp.
 * `shortcut: pull completo, passare a un cursore keyset oltre ~2000 esercizi.`
 *
 * I campi personali (note, passo di carico, hevyName, foto locali) non vengono mai toccati.
 */
class CatalogSync(
    private val db: AppDatabase,
    private val remote: CatalogRemote,
    private val uid: String,
    private val isAdmin: Boolean,
) {
    private val dao = db.exerciseDao()

    data class Report(
        var pushedNew: Int = 0,
        var pushedUpdates: Int = 0,
        var pushedImages: Int = 0,
        var linked: Int = 0,
        var added: Int = 0,
        var archived: Int = 0,
        /** Modifiche scartate perché l'esercizio è di un altro utente. */
        var refused: Int = 0,
        /** Immagini non applicate perché qualcuno era arrivato prima. */
        var imagesLost: Int = 0,
        var skippedRows: Int = 0,
    )

    suspend fun syncOnce(): Report {
        val report = Report()
        pushNew(report)
        pushUpdates(report)
        pushImages(report)
        pull(report)
        return report
    }

    private suspend fun pushNew(report: Report) {
        for (local in dao.localOnly()) {
            val row = toRemote(local, UUID.randomUUID().toString())
            when (remote.insert(row)) {
                InsertResult.Inserted -> {
                    // Solo dopo il successo: un invio perso viene ritentato e finisce in
                    // "nome duplicato", dove la pull collega la riga già creata.
                    dao.markRemote(local.id, row.id, uid)
                    report.pushedNew++
                }
                InsertResult.DuplicateName -> {
                    // La pull dello stesso giro collega l'esercizio che esiste già.
                    report.skippedRows++
                }
            }
        }
    }

    private suspend fun pushUpdates(report: Report) {
        for (local in dao.dirty()) {
            if (!canEdit(local)) {
                dao.clearDirty(local.id)
                report.refused++
                continue
            }
            remote.update(toRemote(local, local.remoteId!!))
            dao.clearDirty(local.id)
            report.pushedUpdates++
        }
    }

    private suspend fun pushImages(report: Report) {
        for (local in dao.pendingImages()) {
            val remoteId = local.remoteId ?: continue
            val key = local.sharedImageKey
            if (key != null && !isAdmin) {
                // Qualcuno l'ha impostata nel frattempo: la mia non serve.
                dao.clearPendingImage(local.id)
                report.imagesLost++
                continue
            }
            val file = local.pendingImageFile?.let(::File)
            if (file == null || !file.exists()) {
                dao.clearPendingImage(local.id)
                continue
            }
            val path = remote.uploadImage(file, remoteId)
            if (remote.setImage(remoteId, path)) {
                dao.setSharedImage(local.id, path)
                report.pushedImages++
            } else {
                dao.clearPendingImage(local.id)
                report.imagesLost++
            }
        }
    }

    private suspend fun pull(report: Report) {
        val rows = remote.pull()
        db.withTransaction {
            // Ordine stabile: l'esito non deve dipendere dall'ordine di arrivo delle righe.
            for (row in rows.sortedBy { it.name.lowercase() }) {
                try {
                    applyRemote(row, report)
                } catch (_: SQLiteConstraintException) {
                    // Nome in conflitto con un esercizio locale già collegato a un altro remoto.
                    report.skippedRows++
                }
            }
        }
    }

    private suspend fun applyRemote(row: RemoteExercise, report: Report) {
        val muscle = runCatching { Muscle.valueOf(row.primaryMuscle) }.getOrNull()
        if (muscle == null) {
            // App più vecchia del server: la riga resta fuori invece di rompere il sync.
            report.skippedRows++
            return
        }
        val bias = runCatching { LengthBias.valueOf(row.lengthBias) }.getOrDefault(LengthBias.NONE)
        val secondary = row.secondaryMuscles.mapNotNull { runCatching { Muscle.valueOf(it) }.getOrNull() }

        val existing = dao.findByRemoteId(row.id)
        if (existing != null) {
            val updated = existing.copy(
                name = row.name,
                primaryMuscle = muscle,
                lengthBias = bias,
                single = row.single,
                sharedImageKey = row.imagePath,
                remoteCreatedBy = row.createdBy,
                archived = existing.archived || row.deleted,
            )
            if (updated != existing) dao.update(updated)
            if (secondary.toSet() != dao.secondaryFor(existing.id).toSet()) {
                dao.replaceSecondary(existing.id, secondary)
            }
            if (row.deleted && !existing.archived) report.archived++
            return
        }

        // Tombstone di una riga che non abbiamo mai avuto: niente fantasma archiviato.
        if (row.deleted) return

        val sameName = dao.findByName(row.name)
        if (sameName != null) {
            if (sameName.remoteId != null) {
                report.skippedRows++
                return
            }
            // Esercizio creato localmente e mai condiviso: è lo stesso esercizio, si collega.
            dao.update(
                sameName.copy(
                    remoteId = row.id,
                    remoteCreatedBy = row.createdBy,
                    name = row.name,
                    primaryMuscle = muscle,
                    lengthBias = bias,
                    single = row.single,
                    sharedImageKey = row.imagePath,
                    dirty = false,
                )
            )
            dao.replaceSecondary(sameName.id, secondary)
            report.linked++
            return
        }

        val id = dao.insert(
            Exercise(
                name = row.name,
                primaryMuscle = muscle,
                lengthBias = bias,
                single = row.single,
                remoteId = row.id,
                remoteCreatedBy = row.createdBy,
                sharedImageKey = row.imagePath,
            )
        )
        dao.replaceSecondary(id, secondary)
        report.added++
    }

    private fun canEdit(exercise: Exercise): Boolean =
        isAdmin || exercise.remoteCreatedBy == uid

    private suspend fun toRemote(exercise: Exercise, remoteId: String) = RemoteExercise(
        id = remoteId,
        name = exercise.name,
        primaryMuscle = exercise.primaryMuscle.name,
        secondaryMuscles = dao.secondaryFor(exercise.id).map { it.name },
        lengthBias = exercise.lengthBias.name,
        single = exercise.single,
        createdBy = uid,
    )
}
