package it.gymlog.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.gymlog.app.timer.TimerState
import it.gymlog.app.ui.design.CardShape
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import kotlinx.coroutines.delay

/** Barra fluttuante del recupero: tempo, pausa, modifica e salto. Invisibile se non c'è un recupero. */
@Composable
fun RestTimerBar(
    state: TimerState,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSetDuration: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isActive) return
    val c = Gym.colors
    var remaining by remember(state.endAtMillis, state.pausedRemainingSeconds) {
        mutableIntStateOf(state.remainingSeconds(System.currentTimeMillis()))
    }
    var editOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state.endAtMillis, state.pausedRemainingSeconds) {
        while (true) {
            remaining = state.remainingSeconds(System.currentTimeMillis())
            if (remaining <= 0 || state.isPaused) break
            delay(250)
        }
    }
    Surface(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = CardShape, color = c.raised, shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(
                    Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = "Modifica tempo di recupero") {
                        editOpen = true
                    },
                ) {
                    Text(
                        if (remaining > 0) "%d:%02d".format(remaining / 60, remaining % 60) else "Pronto!",
                        style = GymType.timer, color = if (remaining > 0) c.text else c.accent,
                    )
                    if (state.isPaused) {
                        Text("In pausa", color = c.warmup, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (remaining > 0) {
                    TextButton(onClick = { onAdjust(-15) }) {
                        Text("−15", color = c.accent, style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { onAdjust(15) }) {
                        Text("+15", color = c.accent, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remaining > 0) {
                    TextButton(onClick = if (state.isPaused) onResume else onPause, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (state.isPaused) "Riprendi" else "Pausa", color = c.accent)
                    }
                    TextButton(onClick = { editOpen = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Modifica", color = c.accent)
                    }
                }
                PrimaryButton(if (remaining > 0) "Salta" else "Chiudi", onSkip, Modifier.weight(1f).heightIn(min = 52.dp))
            }
            LinearProgressIndicator(
                progress = { if (state.totalSeconds > 0) remaining / state.totalSeconds.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = c.accent, trackColor = c.border,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
        }
    }
    if (editOpen) {
        RestDurationDialog(
            initial = state.totalSeconds.coerceAtLeast(60),
            onDismiss = { editOpen = false },
            onConfirm = { seconds -> editOpen = false; onSetDuration(seconds) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestDurationDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val c = Gym.colors
    var text by remember(initial) { mutableStateOf(initial.toString()) }
    val seconds = text.trim().toIntOrNull()
    val valid = seconds != null && seconds in 5..1800
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tempo di recupero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 180, 300).forEach { preset ->
                        GymChip(
                            text = "%d:%02d".format(preset / 60, preset % 60),
                            selected = seconds == preset,
                            onClick = { text = preset.toString() },
                            selectableRole = Role.RadioButton,
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                    label = { Text("Secondi") },
                    supportingText = { Text("Da 5 a 1800 secondi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { seconds?.let(onConfirm) }, enabled = valid) { Text("Imposta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        containerColor = c.card,
    )
}
