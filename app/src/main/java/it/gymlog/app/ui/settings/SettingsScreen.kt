package it.gymlog.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.settings.OneRmFormula
import it.gymlog.app.ui.common.ScreenTitle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal

@Composable
fun SettingsScreen() {
    val vm = appViewModel { SettingsViewModel(it.settings, it.hevyImporter) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val importState by vm.import.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importHevy(uri, context.contentResolver)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Impostazioni")

        var stepText by remember { mutableStateOf("") }
        LaunchedEffect(settings.defaultLoadStepKg) { stepText = formatKg(settings.defaultLoadStepKg) }
        OutlinedTextField(
            value = stepText,
            onValueChange = { text ->
                stepText = text
                parseDecimal(text)?.takeIf { it in 0.25..20.0 }?.let(vm::setStep)
            },
            label = { Text("Passo di aumento del carico (kg)") },
            supportingText = { Text("Usato dal suggerimento di progressione, da 0,25 a 20 kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Formula del 1RM stimato", style = MaterialTheme.typography.titleMedium)
        OneRmFormula.entries.forEach { f ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(selected = settings.oneRmFormula == f, onClick = { vm.setFormula(f) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = settings.oneRmFormula == f, onClick = null)
                Text(f.label, Modifier.padding(start = 12.dp))
            }
        }

        HorizontalDivider()
        SwitchRow("Vibrazione a fine recupero", settings.timerVibration, vm::setVibration)
        SwitchRow("Suono a fine recupero", settings.timerSound, vm::setSound)
        HorizontalDivider()

        Text("Storico", style = MaterialTheme.typography.titleMedium)
        Text(
            "In Hevy: Profilo → Impostazioni → Esporta e importa dati → Esporta allenamenti. " +
                "Poi scegli qui il file workouts.csv. Puoi ripetere l'importazione: gli allenamenti già presenti vengono saltati.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/octet-stream")) },
            enabled = importState !is ImportState.Running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            if (importState is ImportState.Running) {
                CircularProgressIndicator(Modifier.padding(end = 12.dp))
            } else {
                Icon(Icons.Default.FileUpload, contentDescription = null)
            }
            Text("Importa da Hevy (CSV)", Modifier.padding(start = 8.dp))
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
                    if (s.report.exercisesCreated > 0) {
                        append("\n${s.report.exercisesCreated} esercizi nuovi da completare nella libreria.")
                    }
                    if (s.report.invalidLines.isNotEmpty()) {
                        append("\nRighe non valide ignorate: ${s.report.invalidLines.joinToString()}.")
                    }
                })
            },
        )
        is ImportState.Failed -> AlertDialog(
            onDismissRequest = vm::dismissImport,
            confirmButton = { TextButton(onClick = vm::dismissImport) { Text("OK") } },
            title = { Text("Importazione non riuscita") },
            text = { Text(s.message) },
        )
        else -> {}
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
