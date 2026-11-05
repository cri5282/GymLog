package it.gymlog.app.data.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Produce un JPEG pubblico senza EXIF, massimo 1024 px per lato. L'originale resta intatto. */
object SharedImageProcessor {
    fun process(source: File, outputDir: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Immagine non valida" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("Impossibile leggere l'immagine")
        val orientation = ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        val transformed = if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) decoded
            else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true).also { if (it !== decoded) decoded.recycle() }
        val scale = minOf(1f, 1024f / maxOf(transformed.width, transformed.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(transformed, (transformed.width * scale).toInt(), (transformed.height * scale).toInt(), true).also { transformed.recycle() } else transformed
        outputDir.mkdirs()
        return File(outputDir, "shared-${UUID.randomUUID()}.jpg").also { file ->
            try {
                for (quality in listOf(80, 70, 60, 50)) {
                    FileOutputStream(file).use { resized.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                    if (file.length() < 950_000) break
                }
                check(file.length() < 1_000_000) { "Immagine ancora troppo grande dopo la compressione" }
            } catch (t: Throwable) { file.delete(); throw t }
            finally { resized.recycle() }
        }
    }
}
