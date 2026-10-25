package it.gymlog.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Undo
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
import androidx.compose.runtime.key
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
    val unconfirm: () -> Unit = {},
    val weightStepR: (Int) -> Unit = {},
    val setWeightR: (Double) -> Unit = {},
    val repsStepR: (Int) -> Unit = {},
    val setRepsR: (Int) -> Unit = {},
    val setRirR: (Int?) -> Unit = {},
)

/** Tabella delle serie: intestazione # KG REPS ✓, riga attiva espansa, le altre compatte. */
@Composable
fun SetTable(rows: List<RowState>, active: Int?, actions: (Int) -> RowActions, onAddSet: () -> Unit, single: Boolean = false) {
    val c = Gym.colors
    GymCard {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", style = GymType.label, color = c.textSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Text(if (single) "KG  SX / DX" else "KG", style = GymType.label, color = c.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(if (single) "REPS  SX / DX" else "REPS", style = GymType.label, color = c.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("✓", style = GymType.label, color = c.textSecondary, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        }
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = c.border.copy(alpha = 0.4f))
            // Le serie di riscaldamento non entrano nella numerazione delle serie allenanti.
            val workingNumber = rows.take(i + 1).count { it.type == SetType.WORKING }
            // Lo stato locale (nota aperta, dialoghi) deve restare legato alla riga, non alla posizione.
            key(i) {
                if (i == active) ActiveRow(workingNumber, row, actions(i))
                else CompactRow(workingNumber, row, actions(i))
            }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Icon(Icons.Rounded.Add, null, tint = c.accent)
            Text("Aggiungi serie", color = c.accent, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun TypeBadge(number: Int, type: SetType, onSelect: (SetType) -> Unit) {
    val c = Gym.colors
    var open by remember { mutableStateOf(false) }
    val (text, color) = when (type) {
        SetType.WORKING -> "$number" to c.text
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
private fun CompactRow(workingNumber: Int, row: RowState, a: RowActions) {
    val c = Gym.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = a.select)
            .padding(vertical = 8.dp).alpha(if (row.done) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(workingNumber, row.type, a.setType)
        val split = row.weightKgR != null
        Text(
            if (split) "${formatKg(row.weightKg)} / ${formatKg(row.weightKgR!!)}" else formatKg(row.weightKg),
            style = GymType.number, color = c.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (split) "${row.reps} / ${row.repsR ?: row.reps}" else "${row.reps}", style = GymType.number, color = c.text)
            if (row.done && (row.rir != null || row.rirR != null)) {
                Text(
                    if (split) "RIR ${row.rir ?: "–"} / ${row.rirR ?: "–"}" else "RIR ${row.rir}",
                    style = MaterialTheme.typography.labelSmall, color = c.textSecondary,
                )
            }
        }
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { CircleCheck(row.done, if (row.done) a.unconfirm else a.confirm) }
    }
}

@Composable
private fun ActiveRow(workingNumber: Int, row: RowState, a: RowActions) {
    val c = Gym.colors
    var showNote by remember { mutableStateOf(row.notes.isNotEmpty()) }
    val split = row.weightKgR != null
    val noteButton: @Composable () -> Unit = {
        IconButton(onClick = { showNote = !showNote }) {
            Icon(Icons.AutoMirrored.Rounded.Notes, "Nota", tint = if (row.notes.isNotEmpty()) c.accent else c.textSecondary)
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(16.dp))
            .background(c.accentContainer).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Riga 1: tipo/numero, etichetta, conferma. Poi kg e reps (con RIR) per ogni lato; infine la nota.
        Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(workingNumber, row.type, a.setType)
            Text(if (row.type == SetType.WORKING) "Serie $workingNumber" else row.type.label,
                style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { CircleCheck(row.done, if (row.done) a.unconfirm else a.confirm) }
        }
        SideEditor(
            if (split) "SINISTRO" else null, row.weightKg, row.reps, row.rir,
            a.weightStep, a.setWeight, a.repsStep, a.setReps, a.setRir,
            trailing = { if (!split) noteButton() },
        )
        if (split) {
            SideEditor(
                "DESTRO", row.weightKgR!!, row.repsR ?: row.reps, row.rirR,
                a.weightStepR, a.setWeightR, a.repsStepR, a.setRepsR, a.setRirR,
                trailing = { noteButton() },
            )
        }
        if (showNote) {
            OutlinedTextField(row.notes, a.setNote, label = { Text("Nota") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        }
    }
}

/** Kg, reps e RIR di un lato; [label] è null per le serie bilaterali. */
@Composable
private fun SideEditor(
    label: String?, kg: Double, reps: Int, rir: Int?,
    weightStep: (Int) -> Unit, setWeight: (Double) -> Unit, repsStep: (Int) -> Unit, setReps: (Int) -> Unit, setRir: (Int?) -> Unit,
    trailing: @Composable () -> Unit,
) {
    val c = Gym.colors
    var editKg by remember { mutableStateOf(false) }
    var editReps by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (label != null) {
            Text(label, style = GymType.label, color = c.accent, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Stepper(formatKg(kg), "kg", { weightStep(-1) }, { weightStep(+1) }, { editKg = true }, Modifier.fillMaxWidth())
                Text("KG", style = GymType.label, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Stepper("$reps", "reps", { repsStep(-1) }, { repsStep(+1) }, { editReps = true }, Modifier.fillMaxWidth())
                Text("REPS", style = GymType.label, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("RIR", style = GymType.label, color = c.textSecondary, modifier = Modifier.padding(end = 2.dp))
            (0..5).forEach { v ->
                GymChip("$v", selected = rir == v, onClick = { setRir(if (rir == v) null else v) },
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp))
            }
            trailing()
        }
    }
    if (editKg) NumberDialog("Carico (kg)", formatKg(kg), KeyboardType.Decimal, { editKg = false }) { text ->
        parseDecimal(text)?.takeIf { it >= 0 }?.let { setWeight(it); true } ?: false
    }
    if (editReps) NumberDialog("Ripetizioni", reps.toString(), KeyboardType.Number, { editReps = false }) { text ->
        text.trim().toIntOrNull()?.takeIf { it in 0..100 }?.let { setReps(it); true } ?: false
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

