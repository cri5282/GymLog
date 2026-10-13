package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefillTest {
    private fun w(kg: Double, reps: Int) = SetDraft(kg, reps, 1, SetType.WORKING)

    @Test fun prefill_noHistory_returnsEmptyRows() =
        assertEquals(List(3) { SetDraft(0.0, 0, null, SetType.WORKING) }, prefill(emptyList(), 3))

    @Test fun prefill_padsWithLastWorkingSet() =
        assertEquals(listOf(w(80.0, 8), w(80.0, 7), w(80.0, 7)), prefill(listOf(w(80.0, 8), w(80.0, 7)), 3))

    @Test fun prefill_ignoresOtherExercisesAndExtraSets() {
        val last = listOf(w(80.0, 8), w(80.0, 8), w(80.0, 7), w(70.0, 10))
        assertEquals(last, prefill(last, 3))
    }

    @Test fun prefill_warmupKeptAndNotCountedAsWorking() {
        val warm = SetDraft(40.0, 10, null, SetType.WARMUP)
        assertEquals(listOf(warm, w(80.0, 8), w(80.0, 8)), prefill(listOf(warm, w(80.0, 8)), 2))
    }
}
