package it.gymlog.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.gymlog.app.GymLogApp
import it.gymlog.app.data.remote.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Sincronizzazione periodica del catalogo anche ad app chiusa. */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? GymLogApp)?.container ?: return Result.success()
        val runner = container.catalogSync ?: return Result.success()
        // Il processo può partire proprio per il worker: aspetta il ripristino della sessione.
        val state = withTimeoutOrNull(20_000) {
            container.auth.state.first { it !is AuthState.Loading }
        } ?: return Result.retry()
        val user = state as? AuthState.SignedIn ?: return if (state is AuthState.Error) Result.retry() else Result.success()
        // Senza consenso per questo account non c'è nulla da inviare.
        if (container.settings.current().catalogConsentFor != user.uid) return Result.success()
        return when (runner.syncNow()) {
            is SyncStatus.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        const val UNIQUE_NAME = "catalog-sync-periodic"
    }
}

/** Registra la sync periodica una sola volta; le richieste successive non la duplicano. */
object CatalogSyncScheduler {
    private const val PERIOD_HOURS = 12L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(SyncWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
