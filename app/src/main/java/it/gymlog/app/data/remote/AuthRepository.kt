package it.gymlog.app.data.remote

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

sealed interface AuthState {
    /** Build senza Supabase configurato: la sezione account non compare. */
    data object Unavailable : AuthState
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class SignedIn(
        val uid: String,
        val email: String,
        val name: String?,
        /** Deciso dal server (`is_admin()`), non dal client. */
        val isAdmin: Boolean,
        /** Permesso server di pubblicare e sostituire le immagini condivise. */
        val canManageImages: Boolean = isAdmin,
    ) : AuthState

    data class Error(val message: String) : AuthState
}

/**
 * Accesso facoltativo con Google. Senza account l'app resta identica alla 1.5.0.
 * La sessione è gestita da supabase-kt (`SessionStatus`): qui si traduce solo in [AuthState].
 */
class AuthRepository(
    private val client: SupabaseClient?,
    private val googleClientId: String,
) {
    private val _state = MutableStateFlow<AuthState>(
        if (client == null) AuthState.Unavailable else AuthState.Loading
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val available: Boolean get() = client != null

    /** Da chiamare una volta sola all'avvio: segue la sessione salvata e i cambi di stato. */
    suspend fun observe() {
        val c = client ?: return
        c.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user == null) _state.value = AuthState.SignedOut else signedIn(user)
                }
                is SessionStatus.NotAuthenticated -> _state.value = AuthState.SignedOut
                else -> if (_state.value !is AuthState.SignedIn) _state.value = AuthState.Loading
            }
        }
    }

    suspend fun signIn(activity: Activity?) {
        val c = client ?: return
        if (activity == null) {
            _state.value = AuthState.Error("Impossibile aprire l'accesso Google da questa schermata")
            return
        }
        _state.value = AuthState.Loading
        val rawNonce = GoogleNonce.raw()
        try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setServerClientId(googleClientId)
                        // Google vuole l'hash, Supabase riceve il valore originale e lo verifica.
                        .setNonce(GoogleNonce.hash(rawNonce))
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()
            val credential = CredentialManager.create(activity)
                .getCredential(activity, request)
                .credential
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                _state.value = AuthState.Error("Google non ha restituito un token valido")
                return
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            c.auth.signInWith(IDToken) {
                idToken = google.idToken
                provider = Google
                nonce = rawNonce
            }
            // `sessionStatus` pubblica lo stato autenticato: qui non serve altro.
        } catch (_: GetCredentialCancellationException) {
            _state.value = AuthState.SignedOut
        } catch (e: GetCredentialException) {
            _state.value = AuthState.Error("Accesso Google non riuscito: ${e.message ?: "errore sconosciuto"}")
        } catch (e: Exception) {
            _state.value = AuthState.Error("Accesso non riuscito: ${e.message ?: "controlla la connessione"}")
        }
    }

    suspend fun signOut() {
        val c = client ?: return
        // Anche offline la sessione locale sparisce: l'app torna utilizzabile senza account.
        runCatching { c.auth.signOut() }
        runCatching { c.auth.clearSession() }
    }

    suspend fun deleteAccount() {
        val c = client ?: return
        runCatching { c.postgrest.rpc("delete_my_account") }
            .onSuccess {
                // L'utente non esiste più sul server: la sessione va solo cancellata in locale.
                runCatching { c.auth.clearSession() }
                _state.value = AuthState.SignedOut
            }
            .onFailure { _state.value = AuthState.Error("Eliminazione non riuscita: ${it.message ?: "riprova"}")
            }
    }

    suspend fun imageEditors(): List<String> = client!!.postgrest.rpc("list_exercise_image_editors").decodeList()

    suspend fun setImageEditor(email: String, enabled: Boolean) {
        client!!.postgrest.rpc("set_exercise_image_editor", buildJsonObject {
            put("p_email", email.trim())
            put("p_enabled", enabled)
        })
    }

    suspend fun refreshImagePermission() {
        val user = _state.value as? AuthState.SignedIn ?: return
        if (user.isAdmin) return
        val allowed = runCatching {
            client!!.postgrest.rpc("can_manage_exercise_images").decodeAs<Boolean>()
        }.getOrDefault(false)
        _state.value = user.copy(canManageImages = allowed)
    }

    private suspend fun signedIn(user: UserInfo) {
        val admin = runCatching {
            client!!.postgrest.rpc("is_admin").decodeAs<Boolean>()
        }.getOrDefault(false)
        val canManageImages = admin || runCatching {
            client!!.postgrest.rpc("can_manage_exercise_images").decodeAs<Boolean>()
        }.getOrDefault(false)
        _state.value = AuthState.SignedIn(
            uid = user.id,
            email = user.email.orEmpty(),
            name = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull,
            isAdmin = admin,
            canManageImages = canManageImages,
        )
    }
}

/** Nonce per Google: 32 byte casuali, inviati a Google come SHA-256 esadecimale. */
internal object GoogleNonce {
    fun raw(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
