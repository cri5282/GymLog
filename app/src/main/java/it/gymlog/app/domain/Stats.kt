package it.gymlog.app.domain

import it.gymlog.app.data.db.SessionTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class DayMarker { DONE, PLANNED, NONE }

fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

/** Lunedì della settimana di [date]. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Pallini dei 7 giorni a partire da [weekStart]: DONE se c'è un allenamento quel giorno, PLANNED se il giorno
 * della settimana ha una scheda ed è oggi o futuro, altrimenti NONE.
 */
fun weekMarkers(weekStart: LocalDate, today: LocalDate, sessionDates: Set<LocalDate>, plannedDays: Set<Int>): List<DayMarker> =
    (0L until 7L).map { i ->
        val day = weekStart.plusDays(i)
        when {
            day in sessionDates -> DayMarker.DONE
            day.dayOfWeek.value in plannedDays && !day.isBefore(today) -> DayMarker.PLANNED
            else -> DayMarker.NONE
        }
    }

/**
 * Settimane (lun–dom) consecutive con almeno un allenamento. La settimana corrente conta se ha già un
 * allenamento; se è ancora vuota non interrompe la serie, che parte dalla precedente.
 */
fun weekStreak(sessionDates: Set<LocalDate>, today: LocalDate): Int {
    val weeks = sessionDates.map(::weekStart).toSet()
    var week = weekStart(today)
    if (week !in weeks) week = week.minusWeeks(1)
    var count = 0
    while (week in weeks) { count++; week = week.minusWeeks(1) }
    return count
}

/**
 * Livello 1–4 per ogni giorno degli ultimi 365 giorni con almeno un allenamento, in base ai minuti totali
 * (≤30, ≤60, ≤90, >90). Ogni sessione conta nel giorno in cui è iniziata; senza durata vale livello 1.
 */
fun activityLevels(sessions: List<SessionTime>, today: LocalDate, zone: ZoneId): Map<LocalDate, Int> {
    val from = today.minusDays(364)
    val minutes = linkedMapOf<LocalDate, Long>()
    for (s in sessions) {
        val day = localDate(s.startedAt, zone)
        if (day.isBefore(from) || day.isAfter(today)) continue
        val m = s.endedAt?.let { (it - s.startedAt).coerceAtLeast(0) / 60_000 } ?: 0
        minutes.merge(day, m, Long::plus)
    }
    return minutes.mapValues { (_, m) ->
        when {
            m <= 30 -> 1
            m <= 60 -> 2
            m <= 90 -> 3
            else -> 4
        }
    }
}
