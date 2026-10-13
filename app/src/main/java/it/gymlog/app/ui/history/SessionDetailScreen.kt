package it.gymlog.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.RoundIconButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SetEntry
import it.gymlog.app.data.db.SetType
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.formatTime
import it.gymlog.app.ui.common.parseDecimal
import it.gymlog.app.ui.exercises.EnumDropdown

/** "80×8 @2" (+ tipo se non allenante). */
fun SetEntry.label(): String = buildString {
    append("${formatKg(weightKg)}×$reps")
    rir?.let { append(" @$it") }
    if (setType != SetType.WORKING) append(" (${setType.label.lowercase()})")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, onOpenExercise: (Long) -> Unit) {
    val vm = appViewModel(key = "session-$sessionId") { SessionDetailViewModel(sessionId, it.workouts) }
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SetEntry?>(null) }
    var deletingSession by remember { mutableStateOf(false) }

    val c = Gym.colors
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
                Spacer(Modifier.weight(1f))
                RoundIconButton(Icons.Rounded.Delete, "Elimina allenamento", { deletingSession = true }, size = 44)
            }
            Text(state.session?.title.orEmpty(), style = GymType.screenTitle, color = c.text)
            state.session?.let { s ->
                Text(buildString {
                    append("${formatDate(s.startedAt)} · ${formatTime(s.startedAt)}")
                    s.endedAt?.let { append(" · ${(it - s.startedAt).coerceAtLeast(0) / 60_000} min") }
                }, color = c.textSecondary)
            }
        }
        items(state.groups, key = { "g${it.exercise.id}-${it.sets.first().exerciseOrder}" }) { g ->
            GymCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onOpenExercise(g.exercise.id) }) {
                    Text(g.exercise.name, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Storico esercizio", tint = c.textSecondary)
                }
                g.sets.forEach { set ->
                    Row(Modifier.fillMaxWidth().clickable { editing = set }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${set.setIndex + 1}", color = c.textSecondary, modifier = Modifier.width(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(set.label(), style = GymType.number.copy(fontSize = 18.sp), color = c.text)
                            if (set.notes.isNotBlank()) Text(set.notes, color = c.textSecondary)
                        }
                        Icon(Icons.Rounded.Edit, "Modifica serie", tint = c.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    editing?.let { set ->
        EditSetDialog(set, onSave = { vm.updateSet(it); editing = null }, onDelete = { vm.deleteSet(set); editing = null },
            onDismiss = { editing = null })
    }
    if (deletingSession) ConfirmDialog("Eliminare l'allenamento?", "Verranno eliminate tutte le sue serie.", "Elimina",
        onConfirm = { deletingSession = false; vm.deleteSession(onBack) }, onDismiss = { deletingSession = false })
}

@Composable
private fun EditSetDialog(set: SetEntry, onSave: (SetEntry) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var kg by remember { mutableStateOf(formatKg(set.weightKg)) }
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var rir by remember { mutableStateOf(set.rir?.toString().orEmpty()) }
    var type by remember { mutableStateOf(set.setType) }
    var notes by remember { mutableStateOf(set.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    val kgValue = parseDecimal(kg)?.takeIf { it >= 0 }
    val repsValue = reps.toIntOrNull()?.takeIf { it in 0..100 }
    val rirValue = if (rir.isBlank()) null else rir.toIntOrNull()?.takeIf { it in 0..5 }
    val valid = kgValue != null && repsValue != null && (rir.isBlank() || rirValue != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica serie") },
        text = {
            Column {
                OutlinedTextField(kg, { kg = it }, label = { Text("kg") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(reps, { reps = it }, label = { Text("Ripetizioni") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(rir, { rir = it }, label = { Text("RIR (0-5, vuoto = non registrato)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                EnumDropdown("Tipo", type, SetType.entries, { it.label }) { type = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, singleLine = true)
                Row { TextButton(onClick = { confirmDelete = true }) { Text("Elimina serie", color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(set.copy(weightKg = kgValue!!, reps = repsValue!!, rir = rirValue, setType = type, notes = notes.trim()))
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
    if (confirmDelete) ConfirmDialog("Eliminare la serie?", set.label(), "Elimina",
        onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
}
