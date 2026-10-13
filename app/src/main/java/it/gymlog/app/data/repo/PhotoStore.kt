package it.gymlog.app.data.repo

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Foto dei macchinari, copiate nella memoria privata dell'app. */
class PhotoStore(filesDir: File) {
    private val dir = File(filesDir, "photos")

    fun newCameraFile(): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    suspend fun importFrom(uri: Uri, resolver: ContentResolver): File = withContext(Dispatchers.IO) {
        val target = newCameraFile()
        val input = resolver.openInputStream(uri) ?: throw IOException("Impossibile leggere l'immagine")
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        target
    }

    fun delete(path: String) {
        File(path).delete()
    }
}
