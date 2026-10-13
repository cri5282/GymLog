package it.gymlog.app.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import it.gymlog.app.data.db.RoutineExercise
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.weekdayLabel
import it.gymlog.app.ui.exercises.EnumDropdown
import it.gymlog.app.ui.exercises.ExercisePickerDialog

/** Testo del target, es. "3 × 6-8 @ RIR 1-2 · 150 s". */
fun RoutineExercise.targetLabel(): String {
    val reps = if (repMin == repMax) "$repMin" else "$repMin-$repMax"
    val rir = if (targetRirMin == targetRirMax) "$targetRirMin" else "$targetRirMin-$targetRirMax"
    return "$targetSets × $reps @ RIR $rir · $restSeconds s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditScreen(dayId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "day-$dayId") { DayEditViewModel(dayId, it.programs) }
    val day by vm.day.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineExercise?>(null) }

    Column {
        TopAppBar(
            title = { Text(day?.name.orEmpty()) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = {
                IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rinomina") }
                IconButton(onClick = { deleting = true }) { Icon(Icons.Default.Delete, "Elimina scheda") }
            },
        )
        day?.let { d ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                EnumDropdown("Giorno della settimana", d.dayOfWeek, listOf<Int?>(null) + (1..7).toList(), ::weekdayLabel) { value ->
                    vm.updateDay { it.copy(dayOfWeek = value) }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(exercises, key = { _, e -> e.routineExercise.id }) { i, e ->
                ListItem(
                    headlineContent = { Text(e.exercise.exercise.name) },
                    supportingContent = { Text(e.routineExercise.targetLabel()) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { vm.move(e.routineExercise, true) }, enabled = i > 0) { Icon(Icons.Default.ArrowUpward, "Sposta su") }
                            IconButton(onClick = { vm.move(e.routineExercise, false) }, enabled = i < exercises.lastIndex) { Icon(Icons.Default.ArrowDownward, "Sposta giù") }
                        }
                    },
                    modifier = Modifier.clickable { editing = e.routineExercise },
                )
                HorizontalDivider()
            }
        }
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp)) {
            Icon(Icons.Default.Add, null); Text("Aggiungi esercizio", Modifier.padding(start = 8.dp))
        }
    }

    if (renaming) TextInputDialog("Rinomina scheda", day?.name.orEmpty(), "Nome",
        onConfirm = { name -> vm.updateDay { it.copy(name = name.trim()) }; renaming = false }, onDismiss = { renaming = false })
    if (picking) ExercisePickerDialog("Aggiungi esercizio", onPick = { vm.add(it); picking = false }, onDismiss = { picking = false })
    if (deleting) ConfirmDialog("Eliminare la scheda?", "Lo storico degli allenamenti resta.", "Elimina",
        onConfirm = { deleting = false; vm.delete(onBack) }, onDismiss = { deleting = false })
    editing?.let { re ->
        TargetDialog(re, onSave = { vm.update(it); editing = null }, onRemove = { vm.remove(re); editing = null },
            onDismiss = { editing = null })
    }
}

@Composable
private fun TargetDialog(re: RoutineExercise, onSave: (RoutineExercise) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var sets by remember { mutableStateOf(re.targetSets.toString()) }
    var repMin by remember { mutableStateOf(re.repMin.toString()) }
    var repMax by remember { mutableStateOf(re.repMax.toString()) }
    var rirMin by remember { mutableStateOf(re.targetRirMin.toString()) }
    var rirMax by remember { mutableStateOf(re.targetRirMax.toString()) }
    var rest by remember { mutableStateOf(re.restSeconds.toString()) }
    var notes by remember { mutableStateOf(re.notes) }

    val s = sets.toIntOrNull(); val a = repMin.toIntOrNull(); val b = repMax.toIntOrNull()
    val c = rirMin.toIntOrNull(); val d = rirMax.toIntOrNull(); val r = rest.toIntOrNull()
    val error = when {
        s == null || s !in 1..20 -> "Serie: da 1 a 20"
        a == null || b == null || a !in 1..100 || b !in 1..100 -> "Ripetizioni: da 1 a 100"
        a > b -> "Ripetizioni minime maggiori delle massime"
        c == null || d == null || c !in 0..5 || d !in 0..5 -> "RIR: da 0 a 5"
        c > d -> "RIR minimo maggiore del massimo"
        r == null || r !in 0..900 -> "Recupero: da 0 a 900 secondi"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Target") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Serie", sets) { sets = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Reps min", repMin, Modifier.weight(1f)) { repMin = it }
                    NumberField("Reps max", repMax, Modifier.weight(1f)) { repMax = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("RIR min", rirMin, Modifier.weight(1f)) { rirMin = it }
                    NumberField("RIR max", rirMax, Modifier.weight(1f)) { rirMax = it }
                }
                NumberField("Recupero (secondi)", rest) { rest = it }
                OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onRemove) { Text("Rimuovi dalla scheda", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = error == null, onClick = {
                onSave(re.copy(targetSets = s!!, repMin = a!!, repMax = b!!, targetRirMin = c!!, targetRirMax = d!!,
                    restSeconds = r!!, notes = notes.trim()))
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter(Char::isDigit).take(3)) }, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = modifier,
    )
}
