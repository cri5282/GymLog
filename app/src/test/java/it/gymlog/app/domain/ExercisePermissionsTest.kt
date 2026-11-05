package it.gymlog.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePermissionsTest {
    private val me = "utente-1"
    private val other = "utente-2"

    @Test fun localExerciseIsEditableByAnyone() {
        assertTrue(canEditShared(remoteId = null, creatorId = null, uid = null, isAdmin = false))
    }

    @Test fun sharedExerciseIsEditableByCreatorAndAdminOnly() {
        assertTrue(canEditShared("r-1", me, me, isAdmin = false))
        assertTrue(canEditShared("r-1", other, me, isAdmin = true))
        assertFalse(canEditShared("r-1", other, me, isAdmin = false))
        assertFalse(canEditShared("r-1", other, uid = null, isAdmin = false))
    }
}
