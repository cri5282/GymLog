package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType

/** [weightKg]/[reps]/[rir] sono il lato sinistro (o la serie bilaterale); i campi "R" il destro, null se bilaterale. */
data class SetDraft(
    val weightKg: Double, val reps: Int, val rir: Int?, val type: SetType,
    val weightKgR: Double? = null, val repsR: Int? = null, val rirR: Int? = null,
) {
    val split: Boolean get() = weightKgR != null
}

data class Target(val sets: Int, val repMin: Int, val repMax: Int, val rirMin: Int, val rirMax: Int)

/**
 * Doppia progressione: se nell'ultima sessione tutte le serie allenanti hanno raggiunto [Target.repMax]
 * con RIR ≤ [Target.rirMax], suggerisce il carico minimo usato + [stepKg]. Altrimenti null.
 */
fun suggestLoad(lastSets: List<SetDraft>, target: Target, stepKg: Double): Double? {
    val working = lastSets.filter { it.type == SetType.WORKING }
    if (working.isEmpty() || working.size < target.sets) return null
    fun reached(reps: Int, rir: Int?) = reps >= target.repMax && rir != null && rir <= target.rirMax
    val allDone = working.all { reached(it.reps, it.rir) && (!it.split || reached(it.repsR ?: 0, it.rirR)) }
    if (!allDone) return null
    return working.minOf { minOf(it.weightKg, it.weightKgR ?: it.weightKg) } + stepKg
}
