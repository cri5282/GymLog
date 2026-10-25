package it.gymlog.app.ui.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.ExerciseWithDetails
import it.gymlog.app.data.db.RoutineExercise
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.PhotoImage
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.weekdayLabel
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.SecondaryButton
import it.gymlog.app.ui.exercises.ExercisePickerDialog

/** "2:30" da 150 secondi. */
fun formatRest(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Testo del target, es. "3 × 6-8 · RIR 1-2 · 2:30". */
fun RoutineExercise.targetLabel(): String {
    val reps = if (repMin == repMax) "$repMin" else "$repMin-$repMax"
    val rir = if (targetRirMin == targetRirMax) "$targetRirMin" else "$targetRirMin-$targetRirMax"
    return "$targetSets × $reps · RIR $rir · ${formatRest(restSeconds)}"
}

/** Miniatura dell'esercizio: prima foto o icona su fondo "raised". */
@Composable
fun ExerciseThumb(exercise: ExerciseWithDetails, size: Int = 56) {
    val photo = exercise.sortedPhotos.firstOrNull()
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(Gym.colors.raised), contentAlignment = Alignment.Center) {
        if (photo != null) PhotoImage(photo.filePath, Modifier.fillMaxSize(), ContentScale.Crop)
        else Icon(Icons.Rounded.FitnessCenter, null, tint = Gym.colors.textSecondary)
    }
}

@Composable
fun DayEditScreen(dayId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "day-$dayId") { DayEditViewModel(dayId, it.programs) }
    val day by vm.day.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineExercise?>(null) }
    val c = Gym.colors

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.Delete, "Elimina scheda", { deleting = true }, size = 44)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    TextButton(onClick = { renaming = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(day?.name.orEmpty(), style = GymType.screenTitle, color = c.text)
                    }
                    Text("${exercises.size} esercizi · ${weekdayLabel(day?.dayOfWeek)} · tocca il nome per rinominare",
                        color = c.textSecondary)
                }
            }
            itemsIndexed(exercises, key = { _, e -> e.routineExercise.id }) { i, e ->
                GymCard(onClick = { editing = e.routineExercise }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExerciseThumb(e.exercise)
                        Column(Modifier.weight(1f)) {
                            Text(e.exercise.exercise.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(
                                e.routineExercise.targetLabel() +
                                    (e.routineExercise.supersetGroup?.let { " · Superserie $it" } ?: ""),
                                color = c.textSecondary,
                            )
                        }
                        Column {
                            IconButton(onClick = { vm.move(e.routineExercise, true) }, enabled = i > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, "Sposta su", tint = if (i > 0) c.text else c.border)
                            }
                            IconButton(onClick = { vm.move(e.routineExercise, false) }, enabled = i < exercises.lastIndex) {
                                Icon(Icons.Rounded.KeyboardArrowDown, "Sposta giù", tint = if (i < exercises.lastIndex) c.text else c.border)
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton("Aggiungi esercizio", { picking = true }, Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), Icons.Rounded.Add)
    }

    if (renaming) TextInputDialog("Rinomina scheda", day?.name.orEmpty(), "Nome",
        onConfirm = { name -> vm.updateDay { it.copy(name = name.trim()) }; renaming = false }, onDismiss = { renaming = false })
    if (picking) ExercisePickerDialog("Aggiungi esercizio", onPick = { vm.add(it); picking = false }, onDismiss = { picking = false })
    if (deleting) ConfirmDialog("Eliminare la scheda?", "Lo storico degli allenamenti resta.", "Elimina",
        onConfirm = { deleting = false; vm.delete(onBack) }, onDismiss = { deleting = false })
    editing?.let { re ->
        val name = exercises.firstOrNull { it.routineExercise.id == re.id }?.exercise?.exercise?.name.orEmpty()
        TargetSheet(name, re, onSave = { vm.update(it); editing = null }, onRemove = { vm.remove(re); editing = null },
            onDismiss = { editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetSheet(name: String, re: RoutineExercise, onSave: (RoutineExercise) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var sets by remember { mutableStateOf(re.targetSets.toString()) }
    var repMin by remember { mutableStateOf(re.repMin.toString()) }
    var repMax by remember { mutableStateOf(re.repMax.toString()) }
    var rirMin by remember { mutableStateOf(re.targetRirMin.toString()) }
    var rirMax by remember { mutableStateOf(re.targetRirMax.toString()) }
    var rest by remember { mutableStateOf(re.restSeconds.toString()) }
    var notes by remember { mutableStateOf(re.notes) }
    var superset by remember { mutableStateOf(re.supersetGroup?.toString().orEmpty()) }

    val s = sets.toIntOrNull(); val a = repMin.toIntOrNull(); val b = repMax.toIntOrNull()
    val c = rirMin.toIntOrNull(); val d = rirMax.toIntOrNull(); val r = rest.toIntOrNull()
    val error = when {
        s == null || s !in 1..20 -> "Serie: da 1 a 20"
        a == null || b == null || a !in 1..100 || b !in 1..100 -> "Ripetizioni: da 1 a 100"
        a > b -> "Ripetizioni minime maggiori delle massime"
        c == null || d == null || c !in 0..5 || d !in 0..5 -> "RIR: da 0 a 5"
        c > d -> "RIR minimo maggiore del massimo"
        r == null || r !in 0..900 -> "Recupero: da 0 a 900 secondi"
        superset.isNotBlank() && superset.toIntOrNull() !in 1..99 -> "Superserie: da 1 a 99 (vuoto = nessuna)"
        else -> null
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Gym.colors.background) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(name, style = GymType.cardTitle, color = Gym.colors.text)
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
            NumberField("Superserie (vuoto = nessuna)", superset) { superset = it }
            OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Gym.colors.error) }
            PrimaryButton("Salva", enabled = error == null, modifier = Modifier.fillMaxWidth(), onClick = {
                onSave(re.copy(targetSets = s!!, repMin = a!!, repMax = b!!, targetRirMin = c!!, targetRirMax = d!!,
                    restSeconds = r!!, notes = notes.trim(), supersetGroup = superset.trim().toIntOrNull()))
            })
            SecondaryButton("Rimuovi dalla scheda", onRemove, Modifier.fillMaxWidth(), Icons.Rounded.Delete)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter(Char::isDigit).take(3)) }, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = modifier,
        shape = RoundedCornerShape(14.dp),
    )
}
