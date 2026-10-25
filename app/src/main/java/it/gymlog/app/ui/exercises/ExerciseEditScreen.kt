package it.gymlog.app.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.SecondaryButton
import it.gymlog.app.ui.design.SectionLabel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.ui.common.PhotoImage
import it.gymlog.app.ui.common.appViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    onHistory: (Long) -> Unit,
    /** Nascosto quando la modifica avviene dentro un allenamento (non c'è navigazione). */
    showHistory: Boolean = true,
    /** Nascosto durante l'allenamento: eliminare l'esercizio lascerebbe serie orfane. */
    showDelete: Boolean = true,
) {
    val vm = appViewModel(key = "exercise-$exerciseId") { ExerciseEditViewModel(exerciseId, it.exercises, it.photos) }
    val form by vm.form.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val pending by vm.pendingPhotos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraPath?.let { vm.onPhotoTaken(File(it), ok) }
        cameraPath = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onPhotoPicked(uri, context.contentResolver)
    }
    var confirmDelete by remember { mutableStateOf<Boolean?>(null) }

    val c = Gym.colors
    Column(Modifier.imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
            Spacer(Modifier.weight(1f))
            if (showHistory && exerciseId != 0L) RoundIconButton(Icons.Rounded.History, "Storico", { onHistory(exerciseId) }, size = 44)
            Spacer(Modifier.width(8.dp))
            if (showDelete) RoundIconButton(Icons.Rounded.Delete, "Elimina", { scope.launch { confirmDelete = vm.isUsed() } }, size = 44)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(if (exerciseId == 0L) "Nuovo esercizio" else "Modifica esercizio", style = GymType.screenTitle, color = c.text,
                modifier = Modifier.padding(bottom = 8.dp))
            SectionLabel("Dati")
            GymCard {
                OutlinedTextField(
                    value = form.name, onValueChange = { v -> vm.edit { it.copy(name = v) } },
                    label = { Text("Nome") }, isError = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it) } },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                EnumDropdown("Muscolo primario", form.primary, Muscle.entries, { it.label }) { m -> vm.edit { it.copy(primary = m) } }
                Spacer(Modifier.height(8.dp))
                EnumDropdown("Tipo", form.bias, LengthBias.entries, { it.label }) { b -> vm.edit { it.copy(bias = b) } }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.notes, onValueChange = { v -> vm.edit { it.copy(notes = v) } },
                    label = { Text("Note di esecuzione") }, minLines = 2, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
            }
            SectionLabel("Muscoli secondari · contano 0,5 serie")
            GymCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Muscle.entries.filter { it != form.primary }.forEach { m ->
                        val selected = m in form.secondary
                        GymChip(m.label, selected = selected,
                            onClick = { vm.edit { it.copy(secondary = if (selected) it.secondary - m else it.secondary + m) } })
                    }
                }
            }
            SectionLabel("Foto del macchinario")
            GymCard {
                photos.forEachIndexed { i, p ->
                    PhotoRow(p.filePath, canUp = i > 0, canDown = i < photos.lastIndex,
                        onUp = { vm.movePhoto(p, true) }, onDown = { vm.movePhoto(p, false) }, onDelete = { vm.removePhoto(p) })
                }
                pending.forEach { f -> PhotoRow(f.path, false, false, {}, {}, onDelete = { vm.removePending(f) }) }
                if (photos.isEmpty() && pending.isEmpty()) Text("Nessuna foto: scattane una al macchinario per riconoscerlo al volo.",
                    color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Scatta", {
                        val file = vm.newCameraFile()
                        cameraPath = file.path
                        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                    }, Modifier.weight(1f), Icons.Rounded.PhotoCamera)
                    SecondaryButton("Galleria", {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, Modifier.weight(1f), Icons.Rounded.PhotoLibrary)
                }
            }
            SectionLabel("Avanzate")
            GymCard {
                Row(
                    Modifier.fillMaxWidth().clickable { vm.edit { it.copy(single = !it.single) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Unilaterale (single)", style = MaterialTheme.typography.bodyLarge)
                        Text("Peso, reps e RIR separati per sinistra e destra", color = Gym.colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = form.single, onCheckedChange = { v -> vm.edit { it.copy(single = v) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = Gym.colors.accent, checkedThumbColor = Gym.colors.onAccent))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.hevyName, onValueChange = { v -> vm.edit { it.copy(hevyName = v) } },
                    label = { Text("Nome in Hevy (per l'importazione)") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = form.hevyNameError != null, supportingText = form.hevyNameError?.let { { Text(it) } },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = form.loadStep, onValueChange = { v -> vm.edit { it.copy(loadStep = v) } },
                    label = { Text("Passo di carico (kg)") },
                    supportingText = { Text("Vuoto = usa quello delle impostazioni") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Salva", { vm.save(onBack) }, Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding())
    }

    confirmDelete?.let { used ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (used) "Archiviare l'esercizio?" else "Eliminare l'esercizio?") },
            text = {
                Text(
                    if (used) "È presente nello storico o in una scheda: verrà nascosto dalla libreria ma lo storico resta."
                    else "L'esercizio e le sue foto verranno eliminati."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; vm.delete { onBack() } }) { Text(if (used) "Archivia" else "Elimina") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun PhotoRow(path: String, canUp: Boolean, canDown: Boolean, onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PhotoImage(path, Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onUp, enabled = canUp) { Icon(Icons.Default.ArrowUpward, "Sposta su") }
            IconButton(onClick = onDown, enabled = canDown) { Icon(Icons.Default.ArrowDownward, "Sposta giù") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Elimina foto") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(label: String, value: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = text(value), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}
