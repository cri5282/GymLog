package it.gymlog.app.domain

import it.gymlog.app.data.db.SessionTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun d(s: String) = LocalDate.parse(s)
    private fun ms(dt: String) = LocalDateTime.parse(dt).atZone(zone).toInstant().toEpochMilli()
    private var nextId = 1L
    private fun session(start: String, minutes: Long?) = SessionTime(
        nextId++, "S", ms(start), minutes?.let { ms(start) + it * 60_000 }, 3,
    )

    @Test fun weekStart_isMonday() = assertEquals(d("2026-10-12"), weekStart(d("2026-10-14")))

    @Test fun weekMarkers_doneAndPlanned() = assertEquals(
        listOf(DayMarker.DONE, DayMarker.DONE, DayMarker.PLANNED, DayMarker.PLANNED, DayMarker.PLANNED, DayMarker.NONE, DayMarker.NONE),
        weekMarkers(d("2026-10-12"), d("2026-10-14"), setOf(d("2026-10-12"), d("2026-10-13")), (1..5).toSet()),
    )

    @Test fun weekMarkers_pastPlannedNotDone_isNone() =
        assertEquals(DayMarker.NONE, weekMarkers(d("2026-10-12"), d("2026-10-14"), emptySet(), setOf(1))[0])

    @Test fun weekMarkers_acrossYearBoundary() {
        val m = weekMarkers(d("2026-12-28"), d("2027-01-05"), setOf(d("2027-01-02")), emptySet())
        assertEquals(7, m.size)
        assertEquals(DayMarker.DONE, m[5])
        assertEquals(6, m.count { it == DayMarker.NONE })
    }

    @Test fun streak_none_isZero() = assertEquals(0, weekStreak(emptySet(), d("2026-10-14")))

    @Test fun streak_consecutiveWeeks() = assertEquals(3,
        weekStreak(setOf(d("2026-09-29"), d("2026-10-06"), d("2026-10-12")), d("2026-10-14")))

    @Test fun streak_currentWeekEmpty_countsFromPrevious() = assertEquals(2,
        weekStreak(setOf(d("2026-09-29"), d("2026-10-06")), d("2026-10-14")))

    @Test fun streak_gapBreaks() = assertEquals(1,
        weekStreak(setOf(d("2026-09-21"), d("2026-10-05")), d("2026-10-07")))

    @Test fun activity_thresholds() {
        val s = listOf(
            session("2026-10-01T10:00", 30), session("2026-10-02T10:00", 31), session("2026-10-03T10:00", 60),
            session("2026-10-04T10:00", 90), session("2026-10-05T10:00", 91),
        )
        val l = activityLevels(s, d("2026-10-14"), zone)
        assertEquals(listOf(1, 2, 2, 3, 4), (1..5).map { l[d("2026-10-0$it")] })
    }

    @Test fun activity_sumsSameDay() = assertEquals(3,
        activityLevels(listOf(session("2026-10-01T08:00", 40), session("2026-10-01T18:00", 40)), d("2026-10-14"), zone)[d("2026-10-01")])

    @Test fun activity_unfinishedOrZeroDuration_isLevelOne() {
        val l = activityLevels(listOf(session("2026-10-01T08:00", null), session("2026-10-02T08:00", 0)), d("2026-10-14"), zone)
        assertEquals(1, l[d("2026-10-01")])
        assertEquals(1, l[d("2026-10-02")])
    }

    @Test fun activity_olderThanYear_excluded() =
        assertTrue(activityLevels(listOf(session("2025-09-09T08:00", 60)), d("2026-10-14"), zone).isEmpty())

    @Test fun activity_sessionAcrossMidnight_countsOnStartDay() {
        val l = activityLevels(listOf(session("2026-10-10T23:30", 70)), d("2026-10-14"), zone)
        assertEquals(3, l[d("2026-10-10")])
        assertNull(l[d("2026-10-11")])
    }

    @Test fun localDate_usesZone() = assertEquals(d("2026-10-11"), localDate(ms("2026-10-11T00:30"), zone))
}
