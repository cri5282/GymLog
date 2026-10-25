package it.gymlog.app.ui.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// DecimalFormat non è thread-safe: UI, export PDF e CSV possono formattare in parallelo.
private val kgFormat = ThreadLocal.withInitial { DecimalFormat("0.##", DecimalFormatSymbols(Locale.ITALIAN)) }

/** 82.5 → "82,5"; 80.0 → "80". */
fun formatKg(kg: Double): String = kgFormat.get()!!.format(kg)

/** Accetta sia la virgola sia il punto come separatore decimale. */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ITALIAN)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)

fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormat)

fun formatDate(date: java.time.LocalDate): String = date.format(dateFormat)

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormat)
