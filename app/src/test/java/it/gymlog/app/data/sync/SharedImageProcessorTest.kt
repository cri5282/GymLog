package it.gymlog.app.data.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class SharedImageProcessorTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun jpeg(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return File(tmp.root, name).also { file ->
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
        }
    }

    private fun size(file: File): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth to bounds.outHeight
    }

    @Test fun process_limitsLongSideTo1024() {
        val output = SharedImageProcessor.process(jpeg("grande.jpg", 2000, 1000), tmp.newFolder())

        assertEquals(1024 to 512, size(output))
        assertTrue("JPEG sotto il limite del bucket", output.length() in 1..999_999)
    }

    @Test fun process_keepsSmallImagesAtTheirSize() {
        val output = SharedImageProcessor.process(jpeg("piccola.jpg", 400, 300), tmp.newFolder())

        assertEquals(400 to 300, size(output))
    }
}
