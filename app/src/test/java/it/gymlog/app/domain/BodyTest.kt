package it.gymlog.app.domain

import it.gymlog.app.data.db.BodyEntry
import it.gymlog.app.data.settings.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyTest {
    private fun entry(
        weightKg: Double? = null,
        neck: Double? = null,
        chest: Double? = null,
        waist: Double? = null,
        hips: Double? = null,
        skinChest: Double? = null,
        skinMidaxillary: Double? = null,
        triceps: Double? = null,
        subscapular: Double? = null,
        suprailiac: Double? = null,
        abdomen: Double? = null,
        thigh: Double? = null,
    ) = BodyEntry(
        epochDay = LocalDate.of(2026, 10, 12).toEpochDay(),
        weightKg = weightKg, neckCm = neck, chestCm = chest, waistCm = waist, hipsCm = hips,
        skinChest = skinChest, skinMidaxillary = skinMidaxillary, skinTriceps = triceps,
        skinSubscapular = subscapular, skinSuprailiac = suprailiac, skinAbdomen = abdomen, skinThigh = thigh,
    )

    @Test fun jacksonPollock7_usesAllFoldsAndAge() {
        val e = entry(
            skinChest = 10.0, skinMidaxillary = 15.0, triceps = 10.0, subscapular = 15.0,
            abdomen = 25.0, suprailiac = 15.0, thigh = 10.0,
        )
        assertEquals(14.63, bodyFatJackson7(e, age = 30, sex = Sex.MALE)!!, 0.02)
    }

    @Test fun jacksonPollock3_usesTheThreeSexSpecificFolds() {
        val e = entry(skinChest = 10.0, abdomen = 25.0, thigh = 10.0)
        assertEquals(13.61, bodyFatJackson3(e, age = 30, sex = Sex.MALE)!!, 0.02)
        assertNull(bodyFatJackson3(e, age = 30, sex = Sex.FEMALE))
    }

    @Test fun navy_requiresHeightNeckAndWaist() {
        val e = entry(neck = 40.0, waist = 85.0)
        assertNull(bodyFatNavy(e, heightCm = null, sex = Sex.MALE))
        assertEquals(14.53, bodyFatNavy(e, heightCm = 180.0, sex = Sex.MALE)!!, 0.02)
        assertNull(bodyFatNavy(e, heightCm = 180.0, sex = Sex.FEMALE))
    }

    @Test fun ffmi_andNormalization() {
        assertEquals(20.99, ffmi(80.0, 180.0, 15.0)!!, 0.01)
        assertEquals(20.99, ffmiNormalized(80.0, 180.0, 15.0)!!, 0.01)
        assertEquals(24.14, ffmiNormalized(80.0, 170.0, 15.0)!!, 0.02)
        assertNull(ffmi(null, 180.0, 15.0))
        assertNull(ffmi(80.0, null, 15.0))
        assertNull(ffmi(80.0, 180.0, null))
    }

    @Test fun ageIsApproximateFromBirthYear() {
        assertEquals(30, ageAt(1996, LocalDate.of(2026, 10, 12)))
        assertNull(ageAt(1800, LocalDate.of(2026, 10, 12)))
        assertNull(ageAt(null, LocalDate.of(2026, 10, 12)))
    }

    @Test fun movingAverageIgnoresMissingWeights() {
        val entries = listOf(
            BodyEntry(epochDay = LocalDate.of(2026, 10, 1).toEpochDay(), weightKg = 80.0),
            BodyEntry(epochDay = LocalDate.of(2026, 10, 2).toEpochDay(), waistCm = 85.0),
            BodyEntry(epochDay = LocalDate.of(2026, 10, 3).toEpochDay(), weightKg = 82.0),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 1) to 80.0, LocalDate.of(2026, 10, 3) to 81.0),
            weightMovingAverage(entries, window = 7),
        )
    }

    @Test fun estimatesPreferAvailableMethods() {
        val e = entry(weightKg = 80.0, neck = 40.0, waist = 85.0, skinChest = 10.0, abdomen = 25.0, thigh = 10.0)
        val estimates = bodyFatEstimates(e, heightCm = 180.0, birthYear = 1996, sex = Sex.MALE)
        assertEquals(listOf("JP3", "Navy"), estimates.keys.toList())
    }
}
