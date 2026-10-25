package it.gymlog.app.testutil

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/**
 * Contesto isolato per i test che lavorano su database, foto e preferenze reali.
 * Evita di toccare i file dell'Application Robolectric (condivisi con gli altri test).
 */
class TestContext(base: Context, root: File) : ContextWrapper(base) {
    private val filesDir = File(root, "files").apply { mkdirs() }
    private val cacheDir = File(root, "cache").apply { mkdirs() }
    private val databasesDir = File(root, "databases").apply { mkdirs() }

    override fun getFilesDir(): File = filesDir
    override fun getCacheDir(): File = cacheDir
    override fun getDatabasePath(name: String): File = File(databasesDir, name)

    override fun deleteDatabase(name: String): Boolean {
        val file = getDatabasePath(name)
        val deleted = file.delete()
        File("${file.path}-wal").delete()
        File("${file.path}-shm").delete()
        return deleted
    }
}
