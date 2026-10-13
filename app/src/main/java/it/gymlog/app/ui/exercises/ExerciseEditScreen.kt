package it.gymlog.app.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun ExerciseEditScreen(exerciseId: Long, onBack: () -> Unit) {
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

    Column(Modifier.imePadding()) {
        TopAppBar(
            title = { Text(if (exerciseId == 0L) "Nuovo esercizio" else "Modifica esercizio") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = {
                IconButton(onClick = { scope.launch { confirmDelete = vm.isUsed() } }) {
                    Icon(Icons.Default.Delete, "Elimina")
                }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.name, onValueChange = { v -> vm.edit { it.copy(name = v) } },
                label = { Text("Nome") }, isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it) } },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            EnumDropdown("Muscolo primario", form.primary, Muscle.entries, { it.label }) { m -> vm.edit { it.copy(primary = m) } }

            Text("Muscoli secondari (contano 0,5 serie)", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Muscle.entries.filter { it != form.primary }.forEach { m ->
                    val selected = m in form.secondary
                    FilterChip(
                        selected = selected,
                        onClick = { vm.edit { it.copy(secondary = if (selected) it.secondary - m else it.secondary + m) } },
                        label = { Text(m.label) },
                    )
                }
            }
            EnumDropdown("Tipo", form.bias, LengthBias.entries, { it.label }) { b -> vm.edit { it.copy(bias = b) } }
            OutlinedTextField(
                value = form.notes, onValueChange = { v -> vm.edit { it.copy(notes = v) } },
                label = { Text("Note di esecuzione") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
            )

            Text("Foto del macchinario", style = MaterialTheme.typography.titleSmall)
            photos.forEachIndexed { i, p ->
                PhotoRow(p.filePath, canUp = i > 0, canDown = i < photos.lastIndex,
                    onUp = { vm.movePhoto(p, true) }, onDown = { vm.movePhoto(p, false) }, onDelete = { vm.removePhoto(p) })
            }
            pending.forEach { f -> PhotoRow(f.path, false, false, {}, {}, onDelete = { vm.removePending(f) }) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    val file = vm.newCameraFile()
                    cameraPath = file.path
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Icon(Icons.Default.PhotoCamera, null); Text("Scatta", Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null); Text("Galleria", Modifier.padding(start = 8.dp))
                }
            }

            OutlinedTextField(
                value = form.hevyName, onValueChange = { v -> vm.edit { it.copy(hevyName = v) } },
                label = { Text("Nome in Hevy (per l'importazione)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.loadStep, onValueChange = { v -> vm.edit { it.copy(loadStep = v) } },
                label = { Text("Passo di carico (kg)") },
                supportingText = { Text("Vuoto = usa quello delle impostazioni") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = { vm.save(onBack) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 64.dp),
        ) { Text("Salva", style = MaterialTheme.typography.titleMedium) }
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
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}
