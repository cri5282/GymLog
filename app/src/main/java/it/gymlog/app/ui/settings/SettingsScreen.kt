package it.gymlog.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.settings.Accent
import it.gymlog.app.data.settings.AppIcon
import it.gymlog.app.data.settings.OneRmFormula
import it.gymlog.app.data.settings.Sex
import it.gymlog.app.data.settings.ThemeMode
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymSwitchRow
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SecondaryButton
import it.gymlog.app.ui.design.SectionLabel
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.hevyImporter, it.backup, it.csv) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val importState by vm.import.collectAsStateWithLifecycle()
    val backupMessage by vm.backupMessage.collectAsStateWithLifecycle()
    val restartNeeded by vm.restartNeeded.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = Gym.colors
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importHevy(uri, context.contentResolver)
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportBackup(uri, context.contentResolver)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri, context.contentResolver)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setBackupFolder(uri, context.contentResolver)
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
                            Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (selected) c.card else Color.Transparent)
                                .selectable(selected = selected, role = Role.RadioButton) { vm.setThemeMode(mode) },
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
                            Modifier.size(48.dp).clip(CircleShape).background(color)
                                .border(3.dp, if (selected) c.text else Color.Transparent, CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton) { vm.setAccent(a) }
                                .semantics { contentDescription = "Accento ${a.label}" },
                            contentAlignment = Alignment.Center,
                        ) { if (selected) Icon(Icons.Rounded.Check, null, tint = c.onAccent) }
                    }
                }
                Text(settings.accent.label, color = c.textSecondary)
                HorizontalDivider(color = c.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                Text("Icona dell'app", color = c.textSecondary)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon.entries.forEach { icon ->
                        GymChip(
                            icon.label, selected = settings.appIcon == icon,
                            onClick = { vm.setAppIcon(icon, context) }, selectableRole = Role.RadioButton,
                        )
                    }
                }
            }

            SectionLabel("Allenamento")
            GymCard {
                var stepText by remember(settings.defaultLoadStepKg) { mutableStateOf(formatKg(settings.defaultLoadStepKg)) }
                OutlinedTextField(
                    value = stepText,
                    onValueChange = { text -> stepText = text; parseDecimal(text)?.takeIf { it in 0.25..20.0 }?.let(vm::setStep) },
                    label = { Text("Passo di aumento del carico (kg)") },
                    supportingText = { Text("Usato dal suggerimento di progressione, da 0,25 a 20 kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                var restText by remember(settings.defaultRestSeconds) {
                    mutableStateOf(settings.defaultRestSeconds.toString())
                }
                OutlinedTextField(
                    value = restText,
                    onValueChange = { text ->
                        restText = text
                        text.trim().toIntOrNull()?.takeIf { it in 15..600 }?.let(vm::setDefaultRest)
                    },
                    label = { Text("Recupero predefinito (secondi)") },
                    supportingText = { Text("Per esercizi liberi e aggiunti oggi, da 15 a 600 secondi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                Text("Formula del 1RM stimato", color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OneRmFormula.entries.forEach { f ->
                        GymChip(
                            f.label, selected = settings.oneRmFormula == f,
                            onClick = { vm.setFormula(f) }, selectableRole = Role.RadioButton,
                        )
                    }
                }
                HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                GymSwitchRow("Schermo sempre acceso durante l'allenamento", settings.keepScreenOn, vm::setKeepScreenOn)
            }

            SectionLabel("Corpo (facoltativo)")
            GymCard {
                Text("Serve solo per stimare grasso corporeo e FFMI nella sezione Corpo.", color = c.textSecondary)
                Text("Sesso", color = c.textSecondary, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { s ->
                        GymChip(
                            s.label, selected = settings.sex == s,
                            onClick = { vm.setSex(s) }, selectableRole = Role.RadioButton,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberSetting("Altezza (cm)", settings.heightCm?.let(::formatKg).orEmpty(), Modifier.weight(1f), KeyboardType.Decimal) {
                        vm.setHeight(parseDecimal(it))
                    }
                    NumberSetting("Anno di nascita", settings.birthYear?.toString().orEmpty(), Modifier.weight(1f), KeyboardType.Number) {
                        vm.setBirthYear(it.trim().toIntOrNull())
                    }
                }
            }

            SectionLabel("Timer di recupero")
            GymCard {
                GymSwitchRow("Vibrazione a fine recupero", settings.timerVibration, vm::setVibration)
                HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                GymSwitchRow("Suono a fine recupero", settings.timerSound, vm::setSound)
            }

            SectionLabel("Accessibilità")
            GymCard {
                GymSwitchRow(
                    "Chip degli esercizi in allenamento",
                    settings.showExerciseChips,
                    vm::setShowExerciseChips,
                    supportingText = "Mostra la barra per saltare da un esercizio all'altro.",
                )
                HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                GymSwitchRow(
                    "Feedback aptico",
                    settings.haptics,
                    vm::setHaptics,
                    supportingText = "Vibrazione breve quando confermi o rimuovi una serie.",
                )
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

            GymCard {
                Text(
                    "Backup completo: database, foto e impostazioni in un unico zip. Conservalo fuori dal telefono.",
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Esporta backup", {
                    backupPicker.launch("gymlog-backup-${LocalDate.now()}.zip")
                }, Modifier.fillMaxWidth(), Icons.Rounded.FileDownload)
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Ripristina da backup", {
                    restorePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }, Modifier.fillMaxWidth(), Icons.Rounded.Restore)
                HorizontalDivider(color = c.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                GymSwitchRow("Backup automatico settimanale", settings.autoBackup, vm::setAutoBackup)
                SecondaryButton(
                    if (settings.backupTreeUri == null) "Scegli cartella dei backup" else "Cambia cartella dei backup",
                    { folderPicker.launch(null) }, Modifier.fillMaxWidth(), Icons.Rounded.Folder,
                )
                if (settings.backupTreeUri != null) {
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Backup adesso", { vm.backupNow(context.contentResolver) },
                        Modifier.fillMaxWidth(), Icons.Rounded.FileDownload)
                }
                if (settings.lastBackupAt > 0) {
                    Text("Ultimo backup: ${formatDate(settings.lastBackupAt)}", color = c.textSecondary,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }

            GymCard {
                Text("Esporta tutte le serie in CSV per analizzarle con un foglio di calcolo.", color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Esporta CSV allenamenti", {
                    csvPicker.launch("gymlog-allenamenti-${LocalDate.now()}.csv")
                }, Modifier.fillMaxWidth(), Icons.Rounded.TableChart)
            }

            if (busy) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    confirmRestore?.let { uri ->
        ConfirmDialog(
            "Ripristinare il backup?",
            "Allenamenti, esercizi, foto e impostazioni attuali verranno sostituiti con quelli del file. L'app si riavvierà.",
            "Ripristina",
            onConfirm = { confirmRestore = null; vm.restoreBackup(uri, context.contentResolver) },
            onDismiss = { confirmRestore = null },
        )
    }

    backupMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { if (!restartNeeded) vm.dismissBackup() },
            confirmButton = {
                TextButton(onClick = { if (restartNeeded) vm.restartApp() else vm.dismissBackup() }) {
                    Text(if (restartNeeded) "Riavvia ora" else "OK")
                }
            },
            title = { Text("Backup") },
            text = { Text(msg) },
            containerColor = c.card,
        )
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
private fun NumberSetting(
    label: String, value: String, modifier: Modifier = Modifier, type: KeyboardType = KeyboardType.Number,
    onChange: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        singleLine = true, shape = RoundedCornerShape(14.dp), modifier = modifier,
    )
}
