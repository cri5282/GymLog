package it.gymlog.app.data.sync

import android.util.Log
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.remote.AuthRepository
import it.gymlog.app.data.remote.AuthState
import it.gymlog.app.data.remote.CatalogRemote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Esito dell'ultimo scambio, per la sezione account. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data class Done(
        val added: Int,
        val linked: Int,
        val sent: Int,
        val archived: Int,
        val refused: Int,
        val imagesLost: Int,
        val at: Long,
    ) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/**
 * Sincronizza al login, dopo le modifiche locali e con WorkManager quando l'app è chiusa.
 */
class CatalogSyncRunner(
    private val db: AppDatabase,
    private val remote: CatalogRemote,
    private val auth: AuthRepository,
    private val scope: CoroutineScope,
    private val hasConsent: suspend (String) -> Boolean = { true },
) {
    private val mutex = Mutex()
    private var debounce: Job? = null
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun start() = scope.launch {
        auth.state.collectLatest { state ->
            if (state is AuthState.SignedIn) {
                syncNow()
                remote.changes().retryWhen { _, _ -> delay(5_000); true }.collect { requestSync() }
            }
        }
    }

    /** Da chiamare dopo un salvataggio: più modifiche ravvicinate diventano un solo scambio. */
    fun requestSync() {
        debounce?.cancel()
        debounce = scope.launch {
            delay(5_000)
            syncNow()
        }
    }

    suspend fun syncNow(): SyncStatus? {
        return mutex.withLock {
            // Leggi account e consenso dopo aver preso il lock: l'utente può uscire mentre il sync aspetta.
            val user = auth.state.value as? AuthState.SignedIn ?: return@withLock null
            if (!hasConsent(user.uid)) return@withLock null
            _status.value = SyncStatus.Running
            try {
                val report = CatalogSync(db, remote, user.uid, user.isAdmin, user.canManageImages).syncOnce()
                SyncStatus.Done(
                    added = report.added,
                    linked = report.linked,
                    sent = report.pushedNew + report.pushedUpdates,
                    archived = report.archived,
                    refused = report.refused,
                    imagesLost = report.imagesLost,
                    at = System.currentTimeMillis(),
                ).also { _status.value = it }
            } catch (e: CancellationException) {
                _status.value = SyncStatus.Idle
                throw e
            } catch (e: Exception) {
                // Le eccezioni HTTP possono includere l'header Authorization: non mostrarlo né registrarlo.
                Log.w("GymLog", "Sincronizzazione catalogo non riuscita (${e::class.simpleName})")
                SyncStatus.Failed("Errore di sincronizzazione. Riprova più tardi.")
                    .also { _status.value = it }
            }
        }
    }

    suspend fun clearSharedImage(localId: Long, remoteId: String): Boolean {
        return try {
            val cleared = mutex.withLock {
                val user = auth.state.value as? AuthState.SignedIn ?: return@withLock false
                if (!user.canManageImages) return@withLock false
                remote.clearImage(remoteId)
                db.exerciseDao().clearSharedImage(localId)
                true
            }
            if (cleared) requestSync()
            cleared
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /** Solo admin: cancella dal catalogo condiviso. La copia locale viene gestita dal chiamante. */
    suspend fun deleteSharedExercise(remoteId: String): Boolean {
        return try {
            val deleted = mutex.withLock {
                val user = auth.state.value as? AuthState.SignedIn ?: return@withLock false
                if (!user.isAdmin) return@withLock false
                remote.delete(remoteId)
                true
            }
            if (deleted) requestSync()
            deleted
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }
}
