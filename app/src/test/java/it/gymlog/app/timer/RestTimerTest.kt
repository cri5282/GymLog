package it.gymlog.app.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerTest {
    private var now = 0L
    private var started = 0
    private var stopped = 0
    private val timer = RestTimerImpl(clock = { now }, onActive = { started++ }, onCleared = { stopped++ })

    @Test fun remaining_isComputedFromEndTime() {
        timer.start(120)
        now = 61_000
        assertEquals(59, timer.state.value.remainingSeconds(now))
        now = 200_000
        assertEquals(0, timer.state.value.remainingSeconds(now))
        assertEquals(1, started)
    }

    @Test fun remaining_roundsUp() {
        timer.start(10)
        now = 9_001
        assertEquals(1, timer.state.value.remainingSeconds(now))
    }

    @Test fun addSeconds_extendsEnd() {
        timer.start(120)
        timer.addSeconds(30)
        assertEquals(150, timer.state.value.remainingSeconds(0))
        assertEquals(150, timer.state.value.totalSeconds)
    }

    @Test fun addSeconds_afterEnd_restartsFromNow() {
        timer.start(60)
        now = 100_000
        timer.addSeconds(30)
        assertEquals(30, timer.state.value.remainingSeconds(now))
    }

    @Test fun addSeconds_whenIdle_doesNothing() {
        timer.addSeconds(30)
        assertNull(timer.state.value.endAtMillis)
    }

    @Test fun skip_clearsState() {
        timer.start(120)
        timer.skip()
        assertNull(timer.state.value.endAtMillis)
        assertEquals(0, timer.state.value.remainingSeconds(0))
        assertEquals(1, stopped)
    }
}
