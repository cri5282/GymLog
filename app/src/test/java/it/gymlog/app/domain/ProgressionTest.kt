package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionTest {
    private val target = Target(sets = 3, repMin = 6, repMax = 8, rirMin = 1, rirMax = 2)
    private fun w(kg: Double, reps: Int, rir: Int?) = SetDraft(kg, reps, rir, SetType.WORKING)

    @Test fun suggest_allSetsAtTopWithRirOk_returnsPlusStep() =
        assertEquals(82.5, suggestLoad(List(3) { w(80.0, 8, 2) }, target, 2.5)!!, 1e-9)

    @Test fun suggest_oneSetBelowRepMax_null() =
        assertNull(suggestLoad(listOf(w(80.0, 8, 2), w(80.0, 8, 2), w(80.0, 7, 2)), target, 2.5))

    @Test fun suggest_rirAboveMax_null() =
        assertNull(suggestLoad(listOf(w(80.0, 8, 2), w(80.0, 8, 3), w(80.0, 8, 1)), target, 2.5))

    @Test fun suggest_missingRir_null() =
        assertNull(suggestLoad(listOf(w(80.0, 8, 2), w(80.0, 8, null), w(80.0, 8, 1)), target, 2.5))

    @Test fun suggest_fewerSetsThanTarget_null() =
        assertNull(suggestLoad(listOf(w(80.0, 8, 2), w(80.0, 8, 2)), target, 2.5))

    @Test fun suggest_warmupIgnored() {
        val sets = listOf(SetDraft(40.0, 12, 5, SetType.WARMUP)) + List(3) { w(80.0, 8, 2) }
        assertEquals(82.5, suggestLoad(sets, target, 2.5)!!, 1e-9)
    }

    @Test fun suggest_differentLoads_usesMin() =
        assertEquals(82.5, suggestLoad(listOf(w(82.5, 8, 2), w(80.0, 8, 2), w(80.0, 8, 1)), target, 2.5)!!, 1e-9)

    @Test fun suggest_noHistory_null() = assertNull(suggestLoad(emptyList(), target, 2.5))

    private fun split(kgL: Double, repsL: Int, rirL: Int?, kgR: Double, repsR: Int, rirR: Int?) =
        SetDraft(kgL, repsL, rirL, SetType.WORKING, kgR, repsR, rirR)

    @Test fun suggest_split_bothSidesAtTop_usesMinAcrossSides() =
        assertEquals(22.5, suggestLoad(List(3) { split(20.0, 8, 2, 22.0, 8, 1) }, target, 2.5)!!, 1e-9)

    @Test fun suggest_split_rightSideBelowRepMax_null() =
        assertNull(suggestLoad(List(3) { split(20.0, 8, 2, 22.0, 7, 1) }, target, 2.5))

    @Test fun suggest_split_rightSideRirTooHigh_null() =
        assertNull(suggestLoad(List(3) { split(20.0, 8, 2, 22.0, 8, 4) }, target, 2.5))
}
