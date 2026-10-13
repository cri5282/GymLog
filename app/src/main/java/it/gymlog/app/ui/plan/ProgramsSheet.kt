package it.gymlog.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.Program
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.SecondaryButton

/** Pannello programmi: scegli l'attivo, crea, rinomina, elimina. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsSheet(
    programs: List<Program>,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Program?>(null) }
    var deleting by remember { mutableStateOf<Program?>(null) }
    val c = Gym.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Programmi", style = GymType.cardTitle, color = c.text)
            Text("È attivo un programma alla volta: la settimana e lo Start usano quello.", color = c.textSecondary)
            programs.forEach { p ->
                GymCard(onClick = { onSelect(p.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = if (p.isActive) c.accent else c.border)
                        Text(p.name, style = MaterialTheme.typography.titleMedium, color = c.text,
                            modifier = Modifier.weight(1f).padding(start = 12.dp))
                        IconButton(onClick = { renaming = p }) { Icon(Icons.Rounded.Edit, "Rinomina", tint = c.textSecondary) }
                        if (programs.size > 1) IconButton(onClick = { deleting = p }) { Icon(Icons.Rounded.Delete, "Elimina", tint = c.textSecondary) }
                    }
                }
            }
            SecondaryButton("Nuovo programma", { creating = true }, Modifier.fillMaxWidth(), Icons.Rounded.Add)
            Spacer(Modifier.height(16.dp))
        }
    }
    if (creating) TextInputDialog("Nuovo programma", "", "Nome", onConfirm = { onCreate(it.trim()); creating = false }, onDismiss = { creating = false })
    renaming?.let { p -> TextInputDialog("Rinomina programma", p.name, "Nome", onConfirm = { onRename(p.id, it); renaming = null }, onDismiss = { renaming = null }) }
    deleting?.let { p -> ConfirmDialog("Eliminare \"${p.name}\"?", "Verranno eliminate le sue schede. Lo storico degli allenamenti resta.", "Elimina",
        onConfirm = { onDelete(p.id); deleting = null }, onDismiss = { deleting = null }) }
}
