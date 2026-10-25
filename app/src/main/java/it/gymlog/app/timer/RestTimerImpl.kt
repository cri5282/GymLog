package it.gymlog.app.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Timer di recupero basato su un istante di fine assoluto: resta corretto anche se l'app
 * va in background. [onActive] avvia il servizio in primo piano, [onCleared] lo ferma.
 */
class RestTimerImpl(
    private val clock: () -> Long = System::currentTimeMillis,
    private val onActive: () -> Unit = {},
    private val onCleared: () -> Unit = {},
) : RestTimer {
    private val _state = MutableStateFlow(TimerState())
    override val state: StateFlow<TimerState> = _state.asStateFlow()

    override fun start(seconds: Int) {
        if (seconds <= 0) { skip(); return }
        _state.value = TimerState(endAtMillis = clock() + seconds * 1000L, totalSeconds = seconds)
        onActive()
    }

    override fun addSeconds(seconds: Int) {
        val s = _state.value
        val end = s.endAtMillis ?: return
        val now = clock()
        if (seconds < 0) {
            if (end <= now) return
            val adjustedEnd = end + seconds * 1000L
            if (adjustedEnd <= now) { skip(); return }
            val remaining = ((adjustedEnd - now + 999) / 1000).toInt()
            _state.value = TimerState(adjustedEnd, maxOf(remaining, s.totalSeconds + seconds))
            onActive()
        } else {
            _state.value = if (end <= now) TimerState(now + seconds * 1000L, seconds)
            else TimerState(end + seconds * 1000L, maxOf(0, s.totalSeconds + seconds))
            onActive()
        }
    }

    override fun skip() {
        _state.value = TimerState()
        onCleared()
    }
}
