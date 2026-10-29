package it.gymlog.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UnfoldMore
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import it.gymlog.app.data.export.RoutineTextExporter
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.TextInputDialog
import it.gymlog.app.ui.common.WEEKDAYS
import it.gymlog.app.GymLogApp
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.weekdayLabel
import it.gymlog.app.ui.design.AccentPill
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.IconBadge
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SectionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(onOpenRoutine: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val vm = appViewModel { PlanViewModel(it.programs) }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Gym.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { RoutineTextExporter((context.applicationContext as GymLogApp).container.db) }
    var programsOpen by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pdfDay by remember { mutableStateOf<Long?>(null) }
    val pdfExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val dayId = pdfDay
        if (uri != null && dayId != null) {
            scope.launch {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { exporter.exportDayPdf(dayId, it) }
                        ?: error("Impossibile scrivere il file.")
                }.isSuccess
                android.widget.Toast.makeText(
                    context,
                    if (ok) "PDF salvato" else "Esportazione non riuscita",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
        pdfDay = null
    }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item {
            ScreenHeader("Piano", null) {
                RoundIconButton(Icons.Rounded.Settings, "Impostazioni", onOpenSettings)
            }
            GymCard(Modifier.padding(horizontal = 16.dp), onClick = { programsOpen = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("PROGRAMMA ATTIVO", style = GymType.label, color = c.textSecondary)
                        Text(s.program?.name ?: "Nessun programma", style = MaterialTheme.typography.titleMedium, color = c.text)
                    }
                    Icon(Icons.Rounded.UnfoldMore, "Cambia programma", tint = c.textSecondary)
                }
            }
            SectionLabel("Settimana", Modifier.padding(start = 16.dp, top = 12.dp))
        }
        items((1..7).toList()) { dow ->
            val routine = s.week[dow - 1]
            GymCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { assigning = dow }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(WEEKDAYS[dow - 1], style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    if (routine != null) AccentPill(routine.name, Icons.Rounded.FitnessCenter) else GymChip("Riposo")
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Schede", Modifier.weight(1f))
                AccentPill("Nuova", Icons.Rounded.Add, onClick = { creating = true })
            }
        }
        items(s.routines, key = { it.day.id }) { r ->
            GymCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { onOpenRoutine(r.day.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Rounded.FitnessCenter)
                    Column(Modifier.weight(1f)) {
                        Text(r.day.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text("${r.exerciseCount} esercizi · ${weekdayLabel(r.day.dayOfWeek)}", color = c.textSecondary)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val text = exporter.exportDayText(r.day.id)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, r.day.name)
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Condividi scheda"))
                        }
                    }) {
                        Icon(Icons.Rounded.Share, "Condividi scheda come testo", tint = c.textSecondary)
                    }
                    IconButton(onClick = {
                        pdfDay = r.day.id
                        pdfExport.launch("${r.day.name}.pdf")
                    }) {
                        Icon(Icons.Rounded.PictureAsPdf, "Esporta scheda in PDF", tint = c.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
                }
            }
        }
    }

    if (programsOpen) ProgramsSheet(s.programs, onSelect = { vm.selectProgram(it); programsOpen = false },
        onCreate = { vm.createProgram(it); programsOpen = false }, onRename = vm::renameProgram, onDelete = vm::deleteProgram,
        onDismiss = { programsOpen = false })
    if (creating) TextInputDialog("Nuova scheda", "", "Nome (es. Push A)",
        onConfirm = { creating = false; vm.createRoutine(it.trim(), onOpenRoutine) }, onDismiss = { creating = false })
    assigning?.let { dow ->
        ModalBottomSheet(onDismissRequest = { assigning = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.background) {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scheda del ${WEEKDAYS[dow - 1].lowercase()}", style = GymType.cardTitle, color = c.text)
                s.routines.forEach { r ->
                    val selected = s.week[dow - 1]?.id == r.day.id
                    GymCard(onClick = { vm.assign(dow, r.day.id); assigning = null }, color = if (selected) c.accentContainer else c.card) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(Icons.Rounded.FitnessCenter)
                            Column(Modifier.weight(1f)) {
                                Text(r.day.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                                r.day.dayOfWeek?.takeIf { it != dow }?.let { Text("ora il ${WEEKDAYS[it - 1].lowercase()}: verrà spostata", color = c.textSecondary) }
                            }
                        }
                    }
                }
                GymCard(onClick = { vm.assign(dow, null); assigning = null }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.SelfImprovement, color = c.border)
                        Text("Riposo", style = MaterialTheme.typography.titleMedium, color = c.text)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
