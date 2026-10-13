package it.gymlog.app.domain

/**
 * Riga espansa di una pagina: quella selezionata se valida (se già fatta solo con [allowDone]),
 * altrimenti la prima non fatta; null se sono tutte fatte.
 */
fun activeRowIndex(done: List<Boolean>, selected: Int?, allowDone: Boolean = false): Int? {
    if (selected != null && selected in done.indices && (allowDone || !done[selected])) return selected
    return done.indexOfFirst { !it }.takeIf { it >= 0 }
}

/** (serie confermate, serie totali) su tutte le pagine. */
fun sessionProgress(pages: List<List<Boolean>>): Pair<Int, Int> =
    pages.sumOf { p -> p.count { it } } to pages.sumOf { it.size }

/** Esercizi il cui carico massimo della sessione supera il record precedente (null = mai fatto, conta se > 0). */
fun recordsBeaten(sessionBest: Map<String, Double>, previousBest: Map<String, Double?>): List<String> =
    sessionBest.filter { (name, kg) -> kg > 0 && kg > (previousBest[name] ?: 0.0) }.keys.toList()
