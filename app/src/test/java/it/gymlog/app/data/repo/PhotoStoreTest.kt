package it.gymlog.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun newCameraFile_isInPhotosDirWithUniqueName() {
        val store = PhotoStore(tmp.root)
        val a = store.newCameraFile()
        val b = store.newCameraFile()
        assertEquals(tmp.root.resolve("photos"), a.parentFile)
        assertNotEquals(a.name, b.name)
    }

    @Test fun delete_removesFile() {
        val store = PhotoStore(tmp.root)
        val f = store.newCameraFile().apply { writeText("x") }
        store.delete(f.path)
        assertFalse(f.exists())
    }
}
