package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType
import kotlin.math.roundToInt

/** Arrotonda [kg] al multiplo di [step] più vicino (es. 82,5 con passo 2,5). */
fun roundToStep(kg: Double, step: Double): Double =
    if (step <= 0) kg else (kg / step).roundToInt() * step

/**
 * Serie di riscaldamento ricavate dal miglior carico allenante dell'ultima volta:
 * 40% × 8, 60% × 5, 80% × 3, arrotondate al passo dei dischi e senza superare il carico di lavoro.
 */
fun warmupSets(lastWorkingKg: Double, stepKg: Double, single: Boolean = false): List<SetDraft> {
    if (lastWorkingKg <= 0 || stepKg <= 0) return emptyList()
    val fractions = listOf(0.40 to 8, 0.60 to 5, 0.80 to 3)
    return fractions.mapNotNull { (fraction, reps) ->
        val kg = roundToStep(lastWorkingKg * fraction, stepKg)
        if (kg <= 0 || kg >= lastWorkingKg) null
        else SetDraft(kg, reps, null, SetType.WARMUP)
    }.distinctBy { it.weightKg }
        .map { if (single) it.copy(weightKgR = it.weightKg, repsR = it.reps) else it }
}
