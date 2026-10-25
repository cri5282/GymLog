package it.gymlog.app.domain

import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.StatSet
import it.gymlog.app.data.settings.OneRmFormula
import java.time.LocalDate
import java.time.ZoneId

/** Carico del lato più forte della serie (per una serie bilaterale è il carico stesso). */
fun StatSet.topWeight(): Double = maxOf(weightKg, weightKgR ?: weightKg)

/** Volume della serie: entrambi i lati per gli esercizi unilaterali. */
fun StatSet.volumeKg(): Double = weightKg * reps + (weightKgR ?: 0.0) * (repsR ?: 0)

/** 1RM stimato con Epley o Brzycki. Con 1 ripetizione vale il carico stesso. */
fun estimatedOneRm(kg: Double, reps: Int, formula: OneRmFormula): Double = when {
    kg <= 0 || reps <= 0 -> 0.0
    reps == 1 -> kg
    formula == OneRmFormula.EPLEY -> kg * (1.0 + reps / 30.0)
    else -> if (reps >= 37) kg else kg * 36.0 / (37.0 - reps)
}

private fun StatSet.bestE1rm(formula: OneRmFormula): Double =
    maxOf(estimatedOneRm(weightKg, reps, formula), estimatedOneRm(weightKgR ?: 0.0, repsR ?: 0, formula))

data class ProgressPoint(
    val date: LocalDate,
    val bestKg: Double,
    val e1rmKg: Double,
    val volumeKg: Double,
)

data class ExerciseProgress(
    val exerciseId: Long,
    val name: String,
    val muscle: Muscle,
    val single: Boolean,
    /** Punti in ordine cronologico, uno per giorno di allenamento. */
    val points: List<ProgressPoint>,
    val bestKg: Double,
    val bestE1rm: Double,
    val bestVolume: Double,
    val lastDate: LocalDate?,
    /** Divario percentuale del lato destro rispetto al sinistro nell'ultima sessione (null se bilaterale). */
    val asymmetryPct: Double?,
)

/** Divario percentuale: positivo = lato destro più forte. Null se il sinistro non ha carico. */
fun asymmetryPct(left: Double, right: Double): Double? =
    if (left <= 0) null else (right - left) / left * 100.0

private fun StatSet.dateIn(zone: ZoneId): LocalDate = localDate(startedAt, zone)

/** Andamento per esercizio: per ogni giorno miglior carico, miglior 1RM stimato e volume totale. */
fun exerciseProgress(
    sets: List<StatSet>,
    formula: OneRmFormula,
    zone: ZoneId,
): List<ExerciseProgress> = sets.groupBy { it.exerciseId }.map { (id, list) ->
    val first = list.first()
    val points = list.groupBy { it.dateIn(zone) }.toSortedMap().map { (date, daySets) ->
        ProgressPoint(
            date = date,
            bestKg = daySets.maxOf { it.topWeight() },
            e1rmKg = daySets.maxOf { it.bestE1rm(formula) },
            volumeKg = daySets.sumOf { it.volumeKg() },
        )
    }
    val single = first.single
    val lastDay = list.groupBy { it.dateIn(zone) }.maxByOrNull { it.key }?.value.orEmpty()
    val left = lastDay.maxOfOrNull { it.weightKg } ?: 0.0
    val right = lastDay.mapNotNull { it.weightKgR }.maxOrNull()
    ExerciseProgress(
        exerciseId = id,
        name = first.exerciseName,
        muscle = first.muscle,
        single = single,
        points = points,
        bestKg = points.maxOfOrNull { it.bestKg } ?: 0.0,
        bestE1rm = points.maxOfOrNull { it.e1rmKg } ?: 0.0,
        bestVolume = points.maxOfOrNull { it.volumeKg } ?: 0.0,
        lastDate = points.lastOrNull()?.date,
        asymmetryPct = if (single && right != null) asymmetryPct(left, right) else null,
    )
}.sortedByDescending { it.lastDate }

/** Volume settimanale delle ultime [weeks] settimane (lun–dom), dalla più vecchia alla corrente. */
fun weeklyVolumeSeries(
    sets: List<StatSet>,
    today: LocalDate,
    zone: ZoneId,
    weeks: Int = 8,
): List<Pair<LocalDate, Double>> {
    val current = weekStart(today)
    val from = current.minusWeeks((weeks - 1).toLong())
    val totals = HashMap<LocalDate, Double>()
    sets.forEach { s ->
        val week = weekStart(s.dateIn(zone))
        if (!week.isBefore(from) && !week.isAfter(current)) totals.merge(week, s.volumeKg(), Double::plus)
    }
    return (0 until weeks).map { i -> val w = from.plusWeeks(i.toLong()); w to (totals[w] ?: 0.0) }
}

/** Serie allenanti di questa settimana per muscolo primario. */
fun weeklyMuscleSets(sets: List<StatSet>, today: LocalDate, zone: ZoneId): List<Pair<Muscle, Int>> {
    val from = weekStart(today)
    val to = from.plusWeeks(1)
    return sets.filter { s -> val d = s.dateIn(zone); !d.isBefore(from) && d.isBefore(to) }
        .groupingBy { it.muscle }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { it.key to it.value }
}

data class PersonalRecord(val exerciseName: String, val value: Double, val date: LocalDate)

data class AsymmetryPoint(val date: LocalDate, val leftKg: Double, val rightKg: Double, val pct: Double)

/** Divario sinistro/destro per sessione (solo serie con lato destro registrato). */
fun asymmetryHistory(sets: List<StatSet>, zone: ZoneId): List<AsymmetryPoint> =
    sets.filter { it.weightKgR != null }
        .groupBy { it.dateIn(zone) }
        .toSortedMap()
        .mapNotNull { (date, day) ->
            val left = day.maxOf { it.weightKg }
            val right = day.maxOf { it.weightKgR ?: 0.0 }
            asymmetryPct(left, right)?.let { AsymmetryPoint(date, left, right, it) }
        }

/**
 * Record di 1RM stimato in ordine cronologico: ogni volta che un esercizio supera il proprio
 * massimo precedente. [limit] limita i più recenti.
 */
fun latestRecords(sets: List<StatSet>, formula: OneRmFormula, zone: ZoneId, limit: Int = 8): List<PersonalRecord> {
    val best = HashMap<Long, Double>()
    val records = mutableListOf<PersonalRecord>()
    sets.groupBy { it.exerciseId }.forEach { (_, list) ->
        list.groupBy { it.dateIn(zone) }.toSortedMap().forEach { (date, daySets) ->
            val name = daySets.first().exerciseName
            val e1rm = daySets.maxOf { it.bestE1rm(formula) }
            val prev = best[daySets.first().exerciseId] ?: 0.0
            if (e1rm > prev + 0.01) {
                best[daySets.first().exerciseId] = e1rm
                if (prev > 0.0) records += PersonalRecord(name, e1rm, date)
            }
        }
    }
    return records.sortedByDescending { it.date }.take(limit)
}
