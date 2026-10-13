package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType

/**
 * Righe iniziali di un esercizio: le serie dell'ultima volta, completate con copie dell'ultima
 * serie allenante finché le serie allenanti sono almeno [targetSets].
 */
fun prefill(lastSets: List<SetDraft>, targetSets: Int): List<SetDraft> {
    val workingCount = lastSets.count { it.type == SetType.WORKING }
    val filler = lastSets.lastOrNull { it.type == SetType.WORKING } ?: SetDraft(0.0, 0, null, SetType.WORKING)
    return lastSets + List((targetSets - workingCount).coerceAtLeast(0)) { filler }
}
