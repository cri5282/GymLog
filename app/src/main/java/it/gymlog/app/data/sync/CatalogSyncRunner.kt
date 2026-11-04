package it.gymlog.app.data.sync

import android.util.Log
import it.gymlog.app.data.db.AppDatabase
import it.gymlog.app.data.remote.AuthRepository
import it.gymlog.app.data.remote.AuthState
import it.gymlog.app.data.remote.CatalogRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        val at: Long,
    ) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/**
 * Fa partire [CatalogSync] quando l'utente accede e dopo ogni modifica locale.
 *
 * `shortcut: niente lavoro in background a app chiusa; sincronizza all'avvio, al login e dopo
 * un salvataggio (attesa di 5 s per accorpare). Aggiungere WorkManager se serve il periodo 12 h.`
 */
class CatalogSyncRunner(
    private val db: AppDatabase,
    private val remote: CatalogRemote,
    private val auth: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var debounce: Job? = null
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun start() = scope.launch {
        auth.state.collect { state -> if (state is AuthState.SignedIn) syncNow() }
    }

    /** Da chiamare dopo un salvataggio: più modifiche ravvicinate diventano un solo scambio. */
    fun requestSync() {
        debounce?.cancel()
        debounce = scope.launch {
            delay(5_000)
            syncNow()
        }
    }

    suspend fun syncNow() {
        // Se l'account è cambiato durante l'attesa, vince lo stato corrente.
        val user = auth.state.value as? AuthState.SignedIn ?: return
        _status.value = SyncStatus.Running
        mutex.withLock {
            runCatching {
                CatalogSync(db, remote, user.uid, user.isAdmin).syncOnce()
            }.onSuccess { report ->
                _status.value = SyncStatus.Done(
                    added = report.added,
                    linked = report.linked,
                    sent = report.pushedNew + report.pushedUpdates,
                    archived = report.archived,
                    refused = report.refused,
                    at = System.currentTimeMillis(),
                )
            }.onFailure {
                Log.w("GymLog", "Sincronizzazione del catalogo non riuscita", it)
                _status.value = SyncStatus.Failed(it.message ?: "errore di rete")
            }
        }
    }
}
