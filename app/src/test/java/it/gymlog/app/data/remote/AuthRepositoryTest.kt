package it.gymlog.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleNonceTest {
    @Test fun raw_isUrlSafeAndDifferentEveryTime() {
        val nonce = GoogleNonce.raw()
        assertTrue("base64url senza padding", nonce.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertNotEquals(nonce, GoogleNonce.raw())
    }

    @Test fun hash_isSha256InLowercaseHex() {
        // Vettore noto: SHA-256("abc"), quello che Google confronta nel token.
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", GoogleNonce.hash("abc"))
        assertTrue(GoogleNonce.hash(GoogleNonce.raw()).matches(Regex("[0-9a-f]{64}")))
    }
}

class AuthRepositoryTest {
    @Test fun withoutSupabaseTheAccountSectionIsUnavailable() {
        val repo = AuthRepository(client = null, googleClientId = "")
        assertFalse(repo.available)
        assertEquals(AuthState.Unavailable, repo.state.value)
    }
}
