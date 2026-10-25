package it.gymlog.app.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import it.gymlog.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/** Esporta una singola scheda come testo leggibile o come PDF. */
class RoutineTextExporter(private val db: AppDatabase) {

    suspend fun exportDayText(dayId: Long): String = withContext(Dispatchers.IO) {
        val day = db.programDao().getDay(dayId) ?: return@withContext ""
        val exercises = db.programDao().getDayExercises(dayId)
        buildString {
            append("GYMLOG · ${day.name}\n")
            append("=".repeat(minOf(40, day.name.length + 9))).append("\n\n")
            exercises.forEach { e ->
                val re = e.routineExercise
                append("• ${e.exercise.exercise.name}\n")
                append("   ${re.targetSets} serie × ${re.repMin}–${re.repMax} reps")
                append(" · RIR ${re.targetRirMin}–${re.targetRirMax} · recupero ${re.restSeconds}s\n")
                if (re.supersetGroup != null) append("   Superserie ${re.supersetGroup}\n")
                if (re.notes.isNotBlank()) append("   Note: ${re.notes.trim()}\n")
                append("\n")
            }
        }.trim()
    }

    /** Scrive un PDF A4 semplice, con a capo automatico e più pagine. */
    suspend fun exportDayPdf(dayId: Long, out: OutputStream): Int = withContext(Dispatchers.IO) {
        val dayText = exportDayText(dayId)
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f
        val body = Paint().apply { textSize = 12f; color = android.graphics.Color.BLACK; isAntiAlias = true }
        val title = Paint().apply { textSize = 20f; color = android.graphics.Color.BLACK; isFakeBoldText = true }
        val lineHeight = 18f
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 20f
        var usedTitle = false

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
        }

        dayText.lines().forEachIndexed { index, raw ->
            val paint = if (index == 0) title else body
            val maxWidth = pageWidth - margin * 2
            val words = raw.ifBlank { " " }.split(' ')
            var line = ""
            fun flush() {
                if (y > pageHeight - margin) newPage()
                canvas.drawText(line, margin, y, paint)
                y += if (paint === title) 28f else lineHeight
                line = ""
            }
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) flush()
                    line = word
                }
            }
            if (line.isNotEmpty()) flush()
        }
        document.finishPage(page)
        document.writeTo(out)
        document.close()
        pageNumber
    }
}
