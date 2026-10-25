package it.gymlog.app.domain

import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.db.StatSet
import it.gymlog.app.data.settings.OneRmFormula
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ProgressStatsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private var nextId = 1L

    private fun ms(dateTime: String) =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun set(
        exerciseId: Long = 1,
        name: String = "Panca piana",
        muscle: Muscle = Muscle.PETTO,
        single: Boolean = false,
        start: String,
        kg: Double = 100.0,
        reps: Int = 5,
        kgR: Double? = null,
        repsR: Int? = null,
    ) = StatSet(
        setId = nextId++, exerciseId = exerciseId, exerciseName = name, muscle = muscle, single = single,
        startedAt = ms(start), setType = it.gymlog.app.data.db.SetType.WORKING,
        weightKg = kg, weightKgR = kgR, reps = reps, repsR = repsR,
    )

    @Test fun estimatedOneRm_epleyAndBrzycki() {
        assertEquals(116.67, estimatedOneRm(100.0, 5, OneRmFormula.EPLEY), 0.01)
        assertEquals(112.5, estimatedOneRm(100.0, 5, OneRmFormula.BRZYCKI), 0.01)
        assertEquals(100.0, estimatedOneRm(100.0, 1, OneRmFormula.EPLEY), 0.0)
        assertEquals(0.0, estimatedOneRm(0.0, 10, OneRmFormula.EPLEY), 0.0)
    }

    @Test fun weightAndVolume_countBothSides() {
        val s = set(start = "2026-10-12T18:00", kg = 20.0, reps = 10, kgR = 22.0, repsR = 8)
        assertEquals(22.0, s.topWeight(), 0.0)
        assertEquals(376.0, s.volumeKg(), 0.0)
    }

    @Test fun exerciseProgress_groupsByDay() {
        val progress = exerciseProgress(
            listOf(
                set(start = "2026-10-12T18:00", kg = 100.0, reps = 5),
                set(start = "2026-10-12T18:20", kg = 90.0, reps = 8),
                set(start = "2026-10-19T18:00", kg = 105.0, reps = 5),
            ),
            OneRmFormula.EPLEY,
            zone,
        ).single()
        assertEquals("Panca piana", progress.name)
        assertEquals(2, progress.points.size)
        assertEquals(105.0, progress.bestKg, 0.0)
        assertEquals(122.5, progress.bestE1rm, 0.01)
        assertEquals(1220.0, progress.bestVolume, 0.0)
        assertEquals(LocalDate.of(2026, 10, 19), progress.lastDate)
        assertNull(progress.asymmetryPct)
    }

    @Test fun exerciseProgress_asymmetryUsesLatestSession() {
        val progress = exerciseProgress(
            listOf(
                set(start = "2026-10-12T18:00", kg = 20.0, reps = 10, kgR = 18.0, repsR = 10, single = true),
                set(start = "2026-10-19T18:00", kg = 20.0, reps = 10, kgR = 22.0, repsR = 10, single = true),
                set(start = "2026-10-19T18:10", kg = 25.0, reps = 8, kgR = 27.5, repsR = 8, single = true),
            ),
            OneRmFormula.EPLEY,
            zone,
        ).single()
        assertEquals(10.0, progress.asymmetryPct!!, 0.001)
    }

    @Test fun weeklyVolume_returnsEveryWeekIncludingEmptyOnes() {
        val series = weeklyVolumeSeries(
            listOf(
                set(start = "2026-10-05T18:00", kg = 100.0, reps = 5),
                set(start = "2026-10-12T18:00", kg = 100.0, reps = 10),
            ),
            today = LocalDate.of(2026, 10, 14),
            zone = zone,
            weeks = 2,
        )
        assertEquals(listOf(LocalDate.of(2026, 10, 5) to 500.0, LocalDate.of(2026, 10, 12) to 1000.0), series)
    }

    @Test fun muscleSets_onlyCurrentWeek() {
        val muscleSets = weeklyMuscleSets(
            listOf(
                set(start = "2026-10-05T18:00", muscle = Muscle.DORSALI),
                set(start = "2026-10-12T18:00", muscle = Muscle.PETTO),
                set(start = "2026-10-13T18:00", muscle = Muscle.PETTO),
            ),
            today = LocalDate.of(2026, 10, 14),
            zone = zone,
        )
        assertEquals(listOf(Muscle.PETTO to 2), muscleSets)
    }

    @Test fun records_skipFirstSessionAndTrackImprovements() {
        val records = latestRecords(
            listOf(
                set(start = "2026-10-05T18:00", kg = 100.0, reps = 5),
                set(start = "2026-10-12T18:00", kg = 105.0, reps = 5),
            ),
            OneRmFormula.EPLEY,
            zone,
        )
        assertEquals(1, records.size)
        assertEquals("Panca piana", records.first().exerciseName)
        assertEquals(LocalDate.of(2026, 10, 12), records.first().date)
        assertEquals(122.5, records.first().value, 0.01)
    }

    @Test fun asymmetryHistory_onlySetsWithRightSide() {
        val points = asymmetryHistory(
            listOf(
                set(start = "2026-10-12T18:00", kg = 20.0, reps = 10),
                set(start = "2026-10-12T18:10", kg = 20.0, reps = 10, kgR = 22.0, repsR = 10, single = true),
                set(start = "2026-10-19T18:00", kg = 25.0, reps = 8, kgR = 25.0, repsR = 8, single = true),
            ),
            zone,
        )
        assertEquals(2, points.size)
        assertEquals(10.0, points.first().pct, 0.001)
        assertEquals(0.0, points.last().pct, 0.001)
    }
}
