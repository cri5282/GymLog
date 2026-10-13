package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType

data class SetDraft(val weightKg: Double, val reps: Int, val rir: Int?, val type: SetType)

data class Target(val sets: Int, val repMin: Int, val repMax: Int, val rirMin: Int, val rirMax: Int)

/**
 * Doppia progressione: se nell'ultima sessione tutte le serie allenanti hanno raggiunto [Target.repMax]
 * con RIR ≤ [Target.rirMax], suggerisce il carico minimo usato + [stepKg]. Altrimenti null.
 */
fun suggestLoad(lastSets: List<SetDraft>, target: Target, stepKg: Double): Double? {
    val working = lastSets.filter { it.type == SetType.WORKING }
    if (working.isEmpty() || working.size < target.sets) return null
    val allDone = working.all { it.reps >= target.repMax && it.rir != null && it.rir <= target.rirMax }
    if (!allDone) return null
    return working.minOf { it.weightKg } + stepKg
}
