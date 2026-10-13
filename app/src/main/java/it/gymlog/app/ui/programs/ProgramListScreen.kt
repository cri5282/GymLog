package it.gymlog.app.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.ScreenTitle
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.common.appViewModel

@Composable
fun ProgramListScreen(onOpen: (Long) -> Unit) {
    val vm = appViewModel { ProgramsViewModel(it.programs) }
    val programs by vm.programs.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column {
            ScreenTitle("Schede", Modifier.padding(horizontal = 16.dp))
            Text(
                "Un programma contiene le schede dei vari giorni. È attivo un programma alla volta.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn {
                items(programs, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { if (p.isActive) Text("Programma attivo") },
                        trailingContent = {
                            if (p.isActive) Icon(Icons.Default.CheckCircle, "Attivo", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable { onOpen(p.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
        LargeFloatingActionButton(onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, "Nuovo programma")
        }
    }
    if (creating) {
        TextInputDialog("Nuovo programma", "", "Nome", onConfirm = { name ->
            creating = false
            vm.create(name, onOpen)
        }, onDismiss = { creating = false })
    }
}
