package it.gymlog.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.SetType
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal

/** Callback di una riga serie. */
class RowActions(
    val weightStep: (Int) -> Unit,
    val setWeight: (Double) -> Unit,
    val repsStep: (Int) -> Unit,
    val setReps: (Int) -> Unit,
    val setRir: (Int?) -> Unit,
    val setType: (SetType) -> Unit,
    val setNote: (String) -> Unit,
    val confirm: () -> Unit,
)

@Composable
fun SetRow(index: Int, row: RowState, actions: RowActions) {
    var editKg by remember { mutableStateOf(false) }
    var editReps by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(row.notes.isNotEmpty()) }

    Card(
        colors = if (row.done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Serie ${index + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Column {
                    AssistChip(onClick = { typeMenu = true }, label = { Text(row.type.label) })
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        SetType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { actions.setType(t); typeMenu = false })
                        }
                    }
                }
                IconButton(onClick = { showNote = !showNote }) { Icon(Icons.AutoMirrored.Filled.Notes, "Nota") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper("kg", formatKg(row.weightKg), { actions.weightStep(-1) }, { actions.weightStep(+1) }, { editKg = true },
                    Modifier.weight(1f))
                Stepper("reps", row.reps.toString(), { actions.repsStep(-1) }, { actions.repsStep(+1) }, { editReps = true },
                    Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RIR", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 4.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0..5).forEach { v ->
                        FilterChip(
                            selected = row.rir == v,
                            onClick = { actions.setRir(if (row.rir == v) null else v) },
                            label = { Text("$v") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                FilledIconButton(
                    onClick = actions.confirm,
                    modifier = Modifier.padding(start = 8.dp).size(64.dp),
                    colors = if (row.done) IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    else IconButtonDefaults.filledIconButtonColors(),
                ) { Icon(Icons.Default.Check, if (row.done) "Aggiorna serie" else "Serie fatta", Modifier.size(36.dp)) }
            }
            if (showNote) {
                OutlinedTextField(row.notes, actions.setNote, label = { Text("Nota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (editKg) NumberDialog("Carico (kg)", formatKg(row.weightKg), KeyboardType.Decimal, { editKg = false }) { text ->
        parseDecimal(text)?.takeIf { it >= 0 }?.let { actions.setWeight(it); true } ?: false
    }
    if (editReps) NumberDialog("Ripetizioni", row.reps.toString(), KeyboardType.Number, { editReps = false }) { text ->
        text.trim().toIntOrNull()?.takeIf { it in 0..100 }?.let { actions.setReps(it); true } ?: false
    }
}

@Composable
private fun Stepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit, edit: () -> Unit, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = minus, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.Remove, "Meno $label") }
        Column(
            Modifier.weight(1f).widthIn(min = 48.dp).clickable(onClick = edit).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalIconButton(onClick = plus, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.Add, "Più $label") }
    }
}

/** [onSubmit] restituisce true se il valore è valido (e il dialogo si chiude). */
@Composable
private fun NumberDialog(title: String, initial: String, type: KeyboardType, onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(text, { text = it; error = false }, isError = error, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = type),
                supportingText = { if (error) Text("Valore non valido") })
        },
        confirmButton = { TextButton(onClick = { if (onSubmit(text)) onDismiss() else error = true }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
