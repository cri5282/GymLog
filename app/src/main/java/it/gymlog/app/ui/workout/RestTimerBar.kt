package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.timer.TimerState
import it.gymlog.app.ui.design.CardShape
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import kotlinx.coroutines.delay

/** Barra fluttuante del recupero: tempo, avanzamento, +15 s, Salta. Invisibile se non c'è un recupero. */
@Composable
fun RestTimerBar(state: TimerState, onAdjust: (Int) -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val end = state.endAtMillis ?: return
    val c = Gym.colors
    var remaining by remember(end) { mutableIntStateOf(state.remainingSeconds(System.currentTimeMillis())) }
    LaunchedEffect(end) {
        while (true) {
            remaining = state.remainingSeconds(System.currentTimeMillis())
            if (remaining == 0) break
            delay(250)
        }
    }
    Surface(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = CardShape,
        color = c.raised, shadowElevation = 8.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (remaining > 0) "%d:%02d".format(remaining / 60, remaining % 60) else "Pronto!",
                    style = GymType.timer, color = if (remaining > 0) c.text else c.accent, modifier = Modifier.weight(1f))
                if (remaining > 0) {
                    TextButton(onClick = { onAdjust(-15) }) {
                        Text("−15 s", color = c.accent, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = { onAdjust(15) }) {
                    Text("+15 s", color = c.accent, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                }
                PrimaryButton(if (remaining > 0) "Salta" else "Chiudi", onSkip, Modifier.heightIn(min = 52.dp))
            }
            LinearProgressIndicator(
                progress = { if (state.totalSeconds > 0) remaining / state.totalSeconds.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = c.accent, trackColor = c.border,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
        }
    }
}

