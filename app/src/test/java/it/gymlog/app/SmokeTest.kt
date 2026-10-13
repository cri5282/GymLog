package it.gymlog.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appIdIsCorrect() {
        assertEquals("it.gymlog.app", BuildConfig.APPLICATION_ID)
    }
}
