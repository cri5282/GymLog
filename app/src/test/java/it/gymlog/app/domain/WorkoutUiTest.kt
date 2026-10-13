package it.gymlog.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutUiTest {
    @Test fun activeRow_firstUndone() = assertEquals(1, activeRowIndex(listOf(true, false, false), null))
    @Test fun activeRow_selectedUndone_wins() = assertEquals(2, activeRowIndex(listOf(true, false, false), 2))
    @Test fun activeRow_selectedDone_fallsBackToFirstUndone() = assertEquals(1, activeRowIndex(listOf(true, false, true), 2))
    @Test fun activeRow_selectedDoneButExplicit_staysWhenAllowed() = assertEquals(0, activeRowIndex(listOf(true, false), 0, allowDone = true))
    @Test fun activeRow_allDone_isNull() = assertNull(activeRowIndex(listOf(true, true), null))
    @Test fun activeRow_invalidSelection_fallsBack() = assertEquals(1, activeRowIndex(listOf(true, false, false), 7))
    @Test fun progress_countsAllPages() = assertEquals(3 to 5, sessionProgress(listOf(listOf(true, false), listOf(true, true, false))))
    @Test fun records_onlyHigherThanPrevious() = assertEquals(
        listOf("A", "C"),
        recordsBeaten(linkedMapOf("A" to 85.0, "B" to 60.0, "C" to 20.0), mapOf("A" to 80.0, "B" to 60.0, "C" to null)),
    )
    @Test fun records_zeroWeightNeverRecord() = assertEquals(emptyList<String>(), recordsBeaten(mapOf("Crunch" to 0.0), mapOf("Crunch" to null)))
}
