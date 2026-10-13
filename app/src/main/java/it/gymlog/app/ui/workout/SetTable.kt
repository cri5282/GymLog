package it.gymlog.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.SetType
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal
import it.gymlog.app.ui.design.CircleCheck
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.Stepper

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
    val select: () -> Unit,
)

/** Tabella delle serie: intestazione # KG REPS ✓, riga attiva espansa, le altre compatte. */
@Composable
fun SetTable(rows: List<RowState>, active: Int?, actions: (Int) -> RowActions, onAddSet: () -> Unit) {
    val c = Gym.colors
    GymCard {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", style = GymType.label, color = c.textSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Text("KG", style = GymType.label, color = c.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("REPS", style = GymType.label, color = c.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("✓", style = GymType.label, color = c.textSecondary, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        }
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = c.border.copy(alpha = 0.4f))
            if (i == active) ActiveRow(i, row, actions(i)) else CompactRow(i, row, actions(i))
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Icon(Icons.Rounded.Add, null, tint = c.accent)
            Text("Aggiungi serie", color = c.accent, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun TypeBadge(index: Int, type: SetType, onSelect: (SetType) -> Unit) {
    val c = Gym.colors
    var open by remember { mutableStateOf(false) }
    val (text, color) = when (type) {
        SetType.WORKING -> "${index + 1}" to c.text
        SetType.WARMUP -> "R" to c.warmup
        SetType.DROP -> "D" to c.drop
        SetType.FAILURE -> "C" to c.error
    }
    Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(c.raised).clickable { open = true },
            contentAlignment = Alignment.Center,
        ) { Text(text, color = color, fontWeight = FontWeight.Bold) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SetType.entries.forEach { t -> DropdownMenuItem(text = { Text(t.label) }, onClick = { onSelect(t); open = false }) }
        }
    }
}

@Composable
private fun CompactRow(index: Int, row: RowState, a: RowActions) {
    val c = Gym.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = a.select)
            .padding(vertical = 8.dp).alpha(if (row.done) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(index, row.type, a.setType)
        Text(formatKg(row.weightKg), style = GymType.number, color = c.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${row.reps}", style = GymType.number, color = c.text)
            if (row.done && row.rir != null) Text("RIR ${row.rir}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
        }
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { CircleCheck(row.done, a.confirm) }
    }
}

@Composable
private fun ActiveRow(index: Int, row: RowState, a: RowActions) {
    val c = Gym.colors
    var editKg by remember { mutableStateOf(false) }
    var editReps by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(row.notes.isNotEmpty()) }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(16.dp))
            .background(c.accentContainer.copy(alpha = 0.35f)).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(index, row.type, a.setType)
            Stepper(formatKg(row.weightKg), "kg", { a.weightStep(-1) }, { a.weightStep(+1) }, { editKg = true },
                Modifier.weight(1f).padding(end = 4.dp))
            Stepper("${row.reps}", "reps", { a.repsStep(-1) }, { a.repsStep(+1) }, { editReps = true },
                Modifier.weight(1f).padding(start = 4.dp))
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { CircleCheck(row.done, a.confirm) }
        }
        Row(Modifier.padding(start = 40.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("RIR", style = GymType.label, color = c.textSecondary)
            (0..5).forEach { v ->
                GymChip("$v", selected = row.rir == v, onClick = { a.setRir(if (row.rir == v) null else v) }, modifier = Modifier.weight(1f))
            }
            IconButton(onClick = { showNote = !showNote }) {
                Icon(Icons.AutoMirrored.Rounded.Notes, "Nota", tint = if (row.notes.isNotEmpty()) c.accent else c.textSecondary)
            }
        }
        if (showNote) {
            OutlinedTextField(row.notes, a.setNote, label = { Text("Nota") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        }
    }
    if (editKg) NumberDialog("Carico (kg)", formatKg(row.weightKg), KeyboardType.Decimal, { editKg = false }) { text ->
        parseDecimal(text)?.takeIf { it >= 0 }?.let { a.setWeight(it); true } ?: false
    }
    if (editReps) NumberDialog("Ripetizioni", row.reps.toString(), KeyboardType.Number, { editReps = false }) { text ->
        text.trim().toIntOrNull()?.takeIf { it in 0..100 }?.let { a.setReps(it); true } ?: false
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
        containerColor = Gym.colors.card,
    )
}

