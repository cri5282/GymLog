package it.gymlog.app.timer

import kotlinx.coroutines.flow.StateFlow

/** Stato del recupero: [endAtMillis] è l'istante assoluto di fine, null = nessun recupero in corso. */
data class TimerState(val endAtMillis: Long? = null, val totalSeconds: Int = 0) {
    /** Secondi mancanti arrotondati per eccesso; 0 se finito o inattivo. */
    fun remainingSeconds(nowMillis: Long): Int {
        val end = endAtMillis ?: return 0
        val left = end - nowMillis
        return if (left <= 0) 0 else ((left + 999) / 1000).toInt()
    }
}

interface RestTimer {
    val state: StateFlow<TimerState>
    fun start(seconds: Int)
    fun addSeconds(seconds: Int)
    fun skip()
}
