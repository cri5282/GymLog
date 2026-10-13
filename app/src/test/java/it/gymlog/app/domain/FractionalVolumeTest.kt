package it.gymlog.app.domain

import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class FractionalVolumeTest {
    @Test fun fractional_primaryOneSecondaryHalf_warmupExcluded() {
        val bench = ExerciseMuscles(Muscle.PETTO, listOf(Muscle.TRICIPITI, Muscle.DELTOIDE_ANTERIORE))
        val sets = List(3) { SetType.WORKING to bench } + (SetType.WARMUP to bench)
        assertEquals(
            mapOf(Muscle.PETTO to 3.0, Muscle.TRICIPITI to 1.5, Muscle.DELTOIDE_ANTERIORE to 1.5),
            fractionalSets(sets),
        )
    }

    @Test fun fractional_dropAndFailureCount() {
        val curl = ExerciseMuscles(Muscle.BICIPITI, emptyList())
        assertEquals(mapOf(Muscle.BICIPITI to 2.0), fractionalSets(listOf(SetType.DROP to curl, SetType.FAILURE to curl)))
    }
}
