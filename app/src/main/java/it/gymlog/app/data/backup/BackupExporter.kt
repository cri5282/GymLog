package it.gymlog.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import it.gymlog.app.MainActivity
import it.gymlog.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

open class RestoreException(message: String) : Exception(message)

/** Il database è già stato chiuso: l'app deve riavviarsi anche se il ripristino è fallito. */
class RestoreNeedsRestart(message: String) : RestoreException(message)

/**
 * Scrive e ripristina uno zip con database, foto e impostazioni.
 * Il ripristino chiude il database e sostituisce i file: l'app va poi riavviata.
 */
class BackupExporter(private val context: Context, private val db: AppDatabase) {

    suspend fun export(out: OutputStream): Int = withContext(Dispatchers.IO) {
        // Porta nel file principale le scritture ancora nel WAL, così gymlog.db è completo da solo.
        checkpoint()
        var files = 0
        ZipOutputStream(out.buffered()).use { zip ->
            fun add(name: String, file: File) {
                if (!file.isFile) return
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                files++
            }
            add(AppDatabase.NAME, context.getDatabasePath(AppDatabase.NAME))
            File(context.filesDir, "photos").listFiles()?.sortedBy { it.name }?.forEach { add("photos/${it.name}", it) }
            File(context.filesDir, "datastore").listFiles()?.forEach { add("datastore/${it.name}", it) }
        }
        files
    }

    /** Backup automatico nella cartella scelta (SAF); restituisce il numero di file salvati. */
    suspend fun autoBackup(treeUri: Uri, resolver: ContentResolver): Int = withContext(Dispatchers.IO) {
        val tree = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri),
        )
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
        val doc = DocumentsContract.createDocument(resolver, tree, "application/zip", "gymlog-backup-$stamp.zip")
            ?: throw java.io.IOException("Impossibile creare il file nella cartella scelta.")
        resolver.openOutputStream(doc)?.use { export(it) }
            ?: throw java.io.IOException("Impossibile scrivere il backup.")
    }

    /**
     * Ripristina database, foto e impostazioni dallo zip. Il database viene chiuso: dopo il
     * ripristino va chiamato [restartApp].
     */
    suspend fun restore(input: InputStream): Int = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-${System.nanoTime()}")
        staging.mkdirs()
        try {
            var database: File? = null
            var files = 0
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.replace('\\', '/')
                    val target = safeTarget(staging, name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { out -> zip.copyTo(out) }
                        files++
                        if (name == AppDatabase.NAME) database = target
                    }
                    zip.closeEntry()
                }
            }
            val file = database ?: throw RestoreException("Il backup non contiene il database di GymLog.")
            val magic = ByteArray(16)
            val read = file.inputStream().use { input ->
                var total = 0
                while (total < 16) {
                    val n = input.read(magic, total, 16 - total)
                    if (n <= 0) break
                    total += n
                }
                total
            }
            if (read < 16 || !String(magic, 0, read, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                throw RestoreException("Il database nel backup non è valido.")
            }

            // Da qui in poi i file vengono sostituiti: il database va chiuso prima.
            checkpoint()
            val dbPath = context.getDatabasePath(AppDatabase.NAME)
            val safety = File(context.cacheDir, "pre-restore-${System.nanoTime()}.db")
            dbPath.copyTo(safety, overwrite = true)
            db.close()
            try {
                context.deleteDatabase(AppDatabase.NAME)
                file.copyTo(dbPath, overwrite = true)

                val photos = File(context.filesDir, "photos")
                photos.deleteRecursively()
                photos.mkdirs()
                File(staging, "photos").listFiles()?.forEach { it.copyTo(File(photos, it.name), overwrite = true) }

                val datastore = File(context.filesDir, "datastore")
                datastore.deleteRecursively()
                datastore.mkdirs()
                File(staging, "datastore").listFiles()?.forEach { it.copyTo(File(datastore, it.name), overwrite = true) }
            } catch (t: Throwable) {
                // Prova a rimettere il database precedente: l'app deve comunque riavviarsi
                // perché l'istanza Room ormai è chiusa.
                runCatching {
                    context.deleteDatabase(AppDatabase.NAME)
                    safety.copyTo(dbPath, overwrite = true)
                }
                throw RestoreNeedsRestart("Ripristino interrotto: ${t.message ?: "errore sconosciuto"}.")
            } finally {
                safety.delete()
            }
            files
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Riapre l'app dopo un ripristino (il processo viene terminato e rilanciato). */
    fun restartApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        exitProcess(0)
    }

    private fun checkpoint() {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    /** Percorso sicuro dentro [root]: niente "..", percorsi assoluti o file fuori dalle cartelle note. */
    private fun safeTarget(root: File, name: String): File {
        val allowed = name == AppDatabase.NAME || name.startsWith("photos/") || name.startsWith("datastore/")
        if (!allowed || name.contains("..")) throw RestoreException("Il backup contiene un file non previsto: $name")
        val file = File(root, name)
        if (!file.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
            throw RestoreException("Il backup contiene un percorso non valido: $name")
        }
        return file
    }
}
