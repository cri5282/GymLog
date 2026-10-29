package it.gymlog.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.RoundIconButton
import kotlinx.coroutines.delay

/** "mm:ss" (o "h:mm:ss") trascorso da [startedAt]. */
fun elapsedLabel(startedAt: Long, now: Long): String {
    val s = ((now - startedAt).coerceAtLeast(0) / 1000)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun WorkoutTopBar(
    title: String, startedAt: Long, done: Int, total: Int, canReplace: Boolean,
    onClose: () -> Unit, onFinish: () -> Unit, onAddExercise: () -> Unit, onReplaceExercise: () -> Unit, onEditExercise: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Gym.colors
    val now by produceState(System.currentTimeMillis()) { while (true) { value = System.currentTimeMillis(); delay(1000) } }
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Rounded.Close, "Esci (l'allenamento resta in corso)", onClose, size = 48)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${elapsedLabel(startedAt, now)} · $done/$total serie", color = c.textSecondary)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Altro", tint = c.text) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Aggiungi esercizio") }, onClick = { menu = false; onAddExercise() })
                    DropdownMenuItem(text = { Text("Sostituisci esercizio (solo oggi)") }, enabled = canReplace,
                        onClick = { menu = false; onReplaceExercise() })
                    DropdownMenuItem(text = { Text("Modifica esercizio") }, onClick = { menu = false; onEditExercise() })
                    DropdownMenuItem(text = { Text("Impostazioni allenamento") }, onClick = { menu = false; onOpenSettings() })
                }
            }
            IconButton(
                onClick = onFinish,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(c.accent),
            ) {
                Icon(Icons.Rounded.Check, "Termina allenamento", tint = c.onAccent)
            }
        }
        LinearProgressIndicator(
            progress = { if (total > 0) done / total.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp),
            color = c.accent, trackColor = c.raised, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
        )
    }
}
