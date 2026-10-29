package it.gymlog.app.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Messaggi brevi con un'azione "Annulla" opzionale. */
class GymSnackbar(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private var undoJob: Job? = null

    fun show(message: String) {
        undoJob?.cancel()
        undoJob = null
        scope.launch {
            host.showSnackbar(message = message, withDismissAction = true, duration = SnackbarDuration.Short)
        }
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        // Una sola azione Annulla alla volta: l'ultima rimozione è quella ripristinabile.
        undoJob?.cancel()
        undoJob = scope.launch {
            val result = host.showSnackbar(
                message = message,
                actionLabel = "Annulla",
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

val LocalSnackbar = staticCompositionLocalOf<GymSnackbar?> { null }
