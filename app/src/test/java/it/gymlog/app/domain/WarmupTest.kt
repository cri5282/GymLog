package it.gymlog.app.domain

import it.gymlog.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupTest {
    @Test fun roundsToLoadStepAndUsesFortySixtyEightyPercent() {
        val warmups = warmupSets(lastWorkingKg = 80.0, stepKg = 2.5)
        assertEquals(listOf(32.5, 47.5, 65.0), warmups.map { it.weightKg })
        assertEquals(listOf(8, 5, 3), warmups.map { it.reps })
        assertTrue(warmups.all { it.type == SetType.WARMUP && it.rir == null })
    }

    @Test fun neverReachesTheWorkingWeight() {
        val warmups = warmupSets(lastWorkingKg = 20.0, stepKg = 5.0)
        assertEquals(listOf(10.0, 15.0), warmups.map { it.weightKg })
        assertTrue(warmups.all { it.weightKg < 20.0 })
    }

    @Test fun tooLightOrInvalidInputHasNoWarmups() {
        assertTrue(warmupSets(0.0, 2.5).isEmpty())
        assertTrue(warmupSets(2.0, 0.0).isEmpty())
        assertTrue(warmupSets(2.5, 2.5).isEmpty())
    }

    @Test fun unilateralWarmupsHaveBothSides() {
        val warmups = warmupSets(lastWorkingKg = 50.0, stepKg = 2.5, single = true)
        assertTrue(warmups.isNotEmpty())
        assertTrue(warmups.all { it.weightKgR == it.weightKg && it.repsR == it.reps })
    }

    @Test fun duplicateRoundedWeightsAreRemoved() {
        val warmups = warmupSets(lastWorkingKg = 5.0, stepKg = 2.5)
        assertEquals(warmups.map { it.weightKg }.distinct(), warmups.map { it.weightKg })
    }
}
