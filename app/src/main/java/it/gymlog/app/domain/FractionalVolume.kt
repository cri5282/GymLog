package it.gymlog.app.domain

import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SetType

data class ExerciseMuscles(val primary: Muscle, val secondary: List<Muscle>)

/** Serie frazionarie per muscolo: 1 sul primario, 0,5 su ciascun secondario; il riscaldamento non conta. */
fun fractionalSets(sets: List<Pair<SetType, ExerciseMuscles>>): Map<Muscle, Double> {
    val result = linkedMapOf<Muscle, Double>()
    for ((type, muscles) in sets) {
        if (type == SetType.WARMUP) continue
        result.merge(muscles.primary, 1.0, Double::plus)
        muscles.secondary.forEach { result.merge(it, 0.5, Double::plus) }
    }
    return result
}
