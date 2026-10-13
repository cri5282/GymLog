package it.gymlog.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.settings.Accent
import it.gymlog.app.data.settings.OneRmFormula
import it.gymlog.app.data.settings.ThemeMode
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SectionLabel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.hevyImporter) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val importState by vm.import.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = Gym.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importHevy(uri, context.contentResolver)
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
        }
        ScreenHeader("Impostazioni")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("Aspetto")
            GymCard {
                Text("Tema", color = c.textSecondary)
                Row(Modifier.padding(vertical = 8.dp).clip(RoundedCornerShape(14.dp)).background(c.raised).padding(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = settings.themeMode == mode
                        Box(
                            Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (selected) c.card else Color.Transparent).clickable { vm.setThemeMode(mode) },
                            contentAlignment = Alignment.Center,
                        ) { Text(mode.label, color = if (selected) c.text else c.textSecondary, textAlign = TextAlign.Center) }
                    }
                }
                Text("Colore d'accento", color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Accent.entries.forEach { a ->
                        val color = Color(if (c.dark) a.dark else a.light)
                        val selected = settings.accent == a
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(color)
                                .border(3.dp, if (selected) c.text else Color.Transparent, CircleShape)
                                .clickable { vm.setAccent(a) },
                            contentAlignment = Alignment.Center,
                        ) { if (selected) Icon(Icons.Rounded.Check, a.label, tint = c.onAccent) }
                    }
                }
                Text(settings.accent.label, color = c.textSecondary)
            }

            SectionLabel("Allenamento")
            GymCard {
                var stepText by remember { mutableStateOf("") }
                LaunchedEffect(settings.defaultLoadStepKg) { stepText = formatKg(settings.defaultLoadStepKg) }
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { text -> stepText = text; parseDecimal(text)?.takeIf { it in 0.25..20.0 }?.let(vm::setStep) },
                    label = { Text("Passo di aumento del carico (kg)") },
                    supportingText = { Text("Usato dal suggerimento di progressione, da 0,25 a 20 kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                Text("Formula del 1RM stimato", color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OneRmFormula.entries.forEach { f ->
                        it.gymlog.app.ui.design.GymChip(f.label, selected = settings.oneRmFormula == f, onClick = { vm.setFormula(f) })
                    }
                }
                HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                SwitchRow("Schermo sempre acceso durante l'allenamento", settings.keepScreenOn, vm::setKeepScreenOn)
            }

            SectionLabel("Timer di recupero")
            GymCard {
                SwitchRow("Vibrazione a fine recupero", settings.timerVibration, vm::setVibration)
                HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                SwitchRow("Suono a fine recupero", settings.timerSound, vm::setSound)
            }

            SectionLabel("Dati")
            GymCard {
                Text(
                    "In Hevy: Profilo → Impostazioni → Esporta e importa dati → Esporta allenamenti, poi scegli qui il file " +
                        "workouts.csv. Puoi ripetere l'importazione: gli allenamenti già presenti vengono saltati.",
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                if (importState is ImportState.Running) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.accent) }
                } else {
                    PrimaryButton("Importa da Hevy (CSV)", {
                        picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/octet-stream"))
                    }, Modifier.fillMaxWidth(), Icons.Rounded.FileUpload)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    when (val s = importState) {
        is ImportState.Done -> AlertDialog(
            onDismissRequest = vm::dismissImport,
            confirmButton = { TextButton(onClick = vm::dismissImport) { Text("OK") } },
            title = { Text("Importazione completata") },
            text = {
                Text(buildString {
                    append("Importati ${s.report.sessionsImported} allenamenti")
                    if (s.report.sessionsSkipped > 0) append(", ${s.report.sessionsSkipped} saltati perché già presenti")
                    append(".")
                    if (s.report.exercisesCreated > 0) append("\n${s.report.exercisesCreated} esercizi nuovi da completare nella libreria.")
                    if (s.report.invalidLines.isNotEmpty()) append("\nRighe non valide ignorate: ${s.report.invalidLines.joinToString()}.")
                })
            },
            containerColor = c.card,
        )
        is ImportState.Failed -> AlertDialog(
            onDismissRequest = vm::dismissImport,
            confirmButton = { TextButton(onClick = vm::dismissImport) { Text("OK") } },
            title = { Text("Importazione non riuscita") },
            text = { Text(s.message) },
            containerColor = c.card,
        )
        else -> {}
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Gym.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent))
    }
}
