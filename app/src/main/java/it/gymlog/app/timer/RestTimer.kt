package it.gymlog.app.timer

import kotlinx.coroutines.flow.StateFlow

/**
 * Stato del recupero: [endAtMillis] è l'istante assoluto di fine, null = nessun recupero in corso.
 * In pausa [endAtMillis] è null e [pausedRemainingSeconds] conserva il tempo rimasto.
 */
data class TimerState(
    val endAtMillis: Long? = null,
    val totalSeconds: Int = 0,
    val pausedRemainingSeconds: Int? = null,
) {
    val isPaused: Boolean get() = pausedRemainingSeconds != null
    val isActive: Boolean get() = endAtMillis != null || pausedRemainingSeconds != null

    /** Secondi mancanti arrotondati per eccesso; 0 se finito o inattivo. */
    fun remainingSeconds(nowMillis: Long): Int {
        pausedRemainingSeconds?.let { return it }
        val end = endAtMillis ?: return 0
        val left = end - nowMillis
        return if (left <= 0) 0 else ((left + 999) / 1000).toInt()
    }
}

interface RestTimer {
    val state: StateFlow<TimerState>
    fun start(seconds: Int)
    fun addSeconds(seconds: Int)
    fun pause()
    fun resume()
    fun skip()
}
