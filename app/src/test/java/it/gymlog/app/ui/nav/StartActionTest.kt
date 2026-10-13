package it.gymlog.app.ui.nav

import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.data.db.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test

class StartActionTest {
    private val day = RoutineDay(id = 7, programId = 1, name = "Push A", dayOfWeek = 1, sortOrder = 0)

    @Test fun start_unfinished_resumes() =
        assertEquals(StartAction.Resume(3), startAction(WorkoutSession(id = 3, title = "X", startedAt = 0), day))

    @Test fun start_todayRoutine_startsIt() = assertEquals(StartAction.StartDay(7), startAction(null, day))

    @Test fun start_restDay_picks() = assertEquals(StartAction.Pick, startAction(null, null))
}
