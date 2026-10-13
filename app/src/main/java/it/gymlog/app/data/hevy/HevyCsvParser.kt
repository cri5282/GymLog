package it.gymlog.app.data.hevy

import it.gymlog.app.data.db.SetType
import java.io.Reader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.floor

class HevyFormatException(message: String) : Exception(message)

data class HevyRow(
    val line: Int,
    val title: String,
    val startTime: Long,
    val endTime: Long?,
    val exerciseTitle: String,
    val setIndex: Int,
    val setType: SetType,
    val weightKg: Double,
    val reps: Int,
    val rir: Int?,
    val notes: String,
)

data class ParseResult(val rows: List<HevyRow>, val invalidLines: List<Int>)

/** Lettore dell'export CSV di Hevy ("workouts.csv"). Colonne lette per nome. */
object HevyCsvParser {
    private val REQUIRED = listOf("title", "start_time", "exercise_title", "set_type", "weight_kg", "reps")

    private val DATE_FORMATS: List<DateTimeFormatter> = listOf(Locale.ITALIAN, Locale.ENGLISH).map { locale ->
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM yyyy, HH:mm").toFormatter(locale)
    } + DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun parse(input: Reader): ParseResult {
        val records = readCsv(input)
        if (records.isEmpty()) throw HevyFormatException("Il file è vuoto.")
        val header = records.first().first.map { it.trim() }
        val missing = REQUIRED.filter { it !in header }
        if (missing.isNotEmpty()) {
            throw HevyFormatException("Non sembra un export di Hevy: mancano le colonne ${missing.joinToString()}.")
        }
        val col = header.withIndex().associate { (i, name) -> name to i }
        val rows = mutableListOf<HevyRow>()
        val invalid = mutableListOf<Int>()
        for ((fields, line) in records.drop(1)) {
            if (fields.all { it.isBlank() }) continue
            fun f(name: String) = col[name]?.let { fields.getOrNull(it) }?.trim().orEmpty()
            val weightText = f("weight_kg")
            val repsText = f("reps")
            if (weightText.isEmpty() && repsText.isEmpty()) continue // cardio o serie a tempo
            try {
                val rpe = f("rpe").takeIf { it.isNotEmpty() }?.let(::parseNumber)
                rows += HevyRow(
                    line = line,
                    title = f("title"),
                    startTime = parseDate(f("start_time")),
                    endTime = f("end_time").takeIf { it.isNotEmpty() }?.let(::parseDate),
                    exerciseTitle = f("exercise_title").ifEmpty { throw IllegalArgumentException("esercizio vuoto") },
                    setIndex = f("set_index").toIntOrNull() ?: 0,
                    setType = when (f("set_type").lowercase()) {
                        "warmup" -> SetType.WARMUP
                        "dropset" -> SetType.DROP
                        "failure" -> SetType.FAILURE
                        else -> SetType.WORKING
                    },
                    weightKg = if (weightText.isEmpty()) 0.0 else parseNumber(weightText),
                    reps = if (repsText.isEmpty()) 0 else repsText.toInt(),
                    rir = rpe?.let { floor(10 - it + 0.5).toInt().coerceIn(0, 5) },
                    notes = f("exercise_notes"),
                )
            } catch (e: IllegalArgumentException) {
                invalid += line
            } catch (e: DateTimeParseException) {
                invalid += line
            }
        }
        return ParseResult(rows, invalid)
    }

    private fun parseNumber(text: String): Double = text.replace(',', '.').toDouble()

    private fun parseDate(text: String): Long {
        for (format in DATE_FORMATS) {
            try {
                return LocalDateTime.parse(text, format).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        throw DateTimeParseException("Data non riconosciuta", text, 0)
    }

    /** CSV RFC 4180: restituisce (campi, numero di riga del file in cui inizia il record). */
    private fun readCsv(input: Reader): List<Pair<List<String>, Int>> {
        val text = input.readText().removePrefix("﻿")
        val records = mutableListOf<Pair<List<String>, Int>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var line = 1
        var recordLine = 1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                when {
                    c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> { if (c == '\n') line++; field.append(c) }
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { fields.add(field.toString()); field.clear() }
                    '\r' -> {}
                    '\n' -> {
                        fields.add(field.toString()); field.clear()
                        records += fields to recordLine
                        fields = mutableListOf()
                        line++
                        recordLine = line
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            records += fields to recordLine
        }
        return records
    }
}
