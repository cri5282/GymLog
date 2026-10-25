package it.gymlog.app.data.backup

import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.db.CsvRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Esporta tutte le serie concluse in CSV (una riga per serie), leggibile da Excel o fogli di calcolo. */
class WorkoutCsvExporter(private val db: AppDatabase) {

    suspend fun export(out: OutputStream): Int = withContext(Dispatchers.IO) {
        val rows = db.workoutDao().allRowsForExport()
        OutputStreamWriter(out, Charsets.UTF_8).buffered().use { w ->
            w.write(
                "sessione_id,data,ora,titolo,esercizio,tipo_serie," +
                    "kg_sx,reps_sx,rir_sx,kg_dx,reps_dx,rir_dx,note\n"
            )
            rows.forEach { r -> w.write(line(r)) }
        }
        rows.size
    }

    private fun line(r: CsvRow): String {
        val date = Instant.ofEpochMilli(r.startedAt).atZone(ZoneId.systemDefault())
        return listOf(
            r.sessionId.toString(),
            date.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            date.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            r.title,
            r.exerciseName,
            r.setType.label,
            number(r.weightKg),
            r.reps.toString(),
            r.rir?.toString().orEmpty(),
            r.weightKgR?.let(::number).orEmpty(),
            r.repsR?.toString().orEmpty(),
            r.rirR?.toString().orEmpty(),
            r.notes,
        ).joinToString(",") { escape(it) } + "\n"
    }

    private fun number(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}
