package it.gymlog.app.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.weekdayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditScreen(programId: Long, onBack: () -> Unit, onOpenDay: (Long) -> Unit) {
    val vm = appViewModel(key = "program-$programId") { ProgramEditViewModel(programId, it.programs) }
    val program by vm.program.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var addingDay by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(program?.name.orEmpty()) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = {
                IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rinomina") }
                IconButton(onClick = { deleting = true }) { Icon(Icons.Default.Delete, "Elimina programma") }
            },
        )
        if (program?.isActive == false) {
            Button(onClick = vm::setActive, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp)) {
                Text("Imposta come programma attivo")
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(days, key = { _, d -> d.id }) { i, d ->
                ListItem(
                    headlineContent = { Text(d.name) },
                    supportingContent = { Text(weekdayLabel(d.dayOfWeek)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { vm.moveDay(d, true) }, enabled = i > 0) { Icon(Icons.Default.ArrowUpward, "Sposta su") }
                            IconButton(onClick = { vm.moveDay(d, false) }, enabled = i < days.lastIndex) { Icon(Icons.Default.ArrowDownward, "Sposta giù") }
                        }
                    },
                    modifier = Modifier.clickable { onOpenDay(d.id) },
                )
                HorizontalDivider()
            }
        }
        OutlinedButton(onClick = { addingDay = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp)) {
            Icon(Icons.Default.Add, null); Text("Aggiungi scheda", Modifier.padding(start = 8.dp))
        }
    }
    if (renaming) TextInputDialog("Rinomina programma", program?.name.orEmpty(), "Nome",
        onConfirm = { vm.rename(it); renaming = false }, onDismiss = { renaming = false })
    if (addingDay) TextInputDialog("Nuova scheda", "", "Nome (es. Push A)",
        onConfirm = { addingDay = false; vm.addDay(it, onOpenDay) }, onDismiss = { addingDay = false })
    if (deleting) ConfirmDialog("Eliminare il programma?",
        "Verranno eliminate le sue schede. Lo storico degli allenamenti resta.", "Elimina",
        onConfirm = { deleting = false; vm.delete(onBack) }, onDismiss = { deleting = false })
}
