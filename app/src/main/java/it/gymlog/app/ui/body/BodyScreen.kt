package it.gymlog.app.ui.body

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.BodyEntry
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SectionLabel
import it.gymlog.app.ui.stats.Sparkline
import java.time.LocalDate

@Composable
fun BodyScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = appViewModel { BodyViewModel(it.body, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Gym.colors
    var editing by remember { mutableStateOf<BodyEntry?>(null) }
    var formOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<BodyEntry?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
            }
            ScreenHeader("Corpo", "Peso, misure e pliche") {
                TextButton(onClick = { editing = null; formOpen = true }) {
                    Icon(Icons.Rounded.Add, null, tint = c.accent)
                    Text("Aggiungi", color = c.accent)
                }
            }
            if (state.entries.isEmpty()) {
                EmptyState(Icons.Rounded.MonitorWeight, "Nessuna misurazione. Tutte le voci sono facoltative: aggiungi solo quelle che ti interessano.")
                PrimaryButton("Aggiungi misurazione", { editing = null; formOpen = true }, Modifier.fillMaxWidth(), Icons.Rounded.Add)
            }
        }

        state.latest?.let { latest ->
            item {
                GymCard {
                    Text("Ultima misurazione · ${formatDate(LocalDate.ofEpochDay(latest.epochDay))}", color = c.textSecondary)
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        latest.weightKg?.let {
                            Column {
                                Text(formatKg(it), style = GymType.stat, color = c.text)
                                Text("kg", style = GymType.label, color = c.textSecondary)
                            }
                        }
                        state.movingAverage.lastOrNull()?.let { (_, avg) ->
                            Column {
                                Text(formatKg(avg), style = GymType.number, color = c.accent)
                                Text("media 7 gg", style = GymType.label, color = c.textSecondary)
                            }
                        }
                    }
                    if (state.estimates.isNotEmpty()) {
                        HorizontalDivider(color = c.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
                        Text(state.estimates.entries.joinToString(" · ") { (k, v) -> "$k ${formatKg(v)}%" },
                            style = GymType.number, color = c.text)
                        Text("Grasso corporeo stimato", style = GymType.label, color = c.textSecondary)
                    }
                    if (state.ffmi != null) {
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("FFMI ${formatKg(state.ffmi!!)}", color = c.text)
                            state.ffmiNormalized?.let { Text("normalizzato ${formatKg(it)}", color = c.textSecondary) }
                        }
                    }
                    if (state.estimates.isEmpty() && state.ffmi == null) {
                        Text("Per grasso e FFMI servono altezza, anno di nascita e sesso (facoltativi).",
                            color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = onOpenSettings) { Text("Apri Impostazioni", color = c.accent) }
                    }
                }
            }
        }

        if (state.movingAverage.size >= 2) {
            item {
                GymCard {
                    Text("Peso e media a 7 giorni", color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Sparkline(state.movingAverage.map { it.second }, Modifier.fillMaxWidth().height(120.dp))
                    Row(Modifier.padding(top = 4.dp)) {
                        Text("min ${formatKg(state.movingAverage.minOf { it.second })} kg", style = GymType.label, color = c.textSecondary)
                        Spacer(Modifier.weight(1f))
                        Text("max ${formatKg(state.movingAverage.maxOf { it.second })} kg", style = GymType.label, color = c.textSecondary)
                    }
                }
            }
        }

        if (state.entries.isNotEmpty()) {
            item { SectionLabel("Misurazioni") }
            items(state.entries, key = { it.id }) { entry ->
                GymCard(onClick = { editing = entry; formOpen = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDate(LocalDate.ofEpochDay(entry.epochDay)), style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(summary(entry), color = c.textSecondary)
                        }
                        IconButton(onClick = { deleting = entry }) { Icon(Icons.Rounded.Delete, "Elimina", tint = c.textSecondary) }
                    }
                }
            }
        }
    }

    if (formOpen) {
        BodyFormSheet(
            existing = editing,
            onSave = { vm.save(it); formOpen = false },
            onDismiss = { formOpen = false },
        )
    }
    deleting?.let { entry ->
        ConfirmDialog("Eliminare la misurazione?", "I dati di ${formatDate(LocalDate.ofEpochDay(entry.epochDay))} verranno rimossi.", "Elimina",
            onConfirm = { vm.delete(entry); deleting = null }, onDismiss = { deleting = null })
    }
}

private fun summary(e: BodyEntry): String = buildString {
    if (e.weightKg != null) append("${formatKg(e.weightKg)} kg")
    val measures = listOfNotNull(e.neckCm, e.chestCm, e.waistCm, e.hipsCm, e.armCm, e.thighCm, e.calfCm).size
    if (measures > 0) { if (isNotEmpty()) append(" · "); append("$measures circonferenze") }
    val folds = listOfNotNull(e.skinChest, e.skinMidaxillary, e.skinTriceps, e.skinSubscapular, e.skinAbdomen, e.skinSuprailiac, e.skinThigh).size
    if (folds > 0) { if (isNotEmpty()) append(" · "); append("$folds pliche") }
    if (isEmpty()) append("Solo note")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyFormSheet(existing: BodyEntry?, onSave: (BodyEntry) -> Unit, onDismiss: () -> Unit) {
    val c = Gym.colors
    val today = LocalDate.now()
    var dateText by remember { mutableStateOf(LocalDate.ofEpochDay(existing?.epochDay ?: today.toEpochDay()).toString()) }
    fun initial(v: Double?) = v?.let { formatKg(it) }.orEmpty()
    var weight by remember { mutableStateOf(initial(existing?.weightKg)) }
    var neck by remember { mutableStateOf(initial(existing?.neckCm)) }
    var chest by remember { mutableStateOf(initial(existing?.chestCm)) }
    var waist by remember { mutableStateOf(initial(existing?.waistCm)) }
    var hips by remember { mutableStateOf(initial(existing?.hipsCm)) }
    var arm by remember { mutableStateOf(initial(existing?.armCm)) }
    var thigh by remember { mutableStateOf(initial(existing?.thighCm)) }
    var calf by remember { mutableStateOf(initial(existing?.calfCm)) }
    var skinChest by remember { mutableStateOf(initial(existing?.skinChest)) }
    var skinMidaxillary by remember { mutableStateOf(initial(existing?.skinMidaxillary)) }
    var skinTriceps by remember { mutableStateOf(initial(existing?.skinTriceps)) }
    var skinSubscapular by remember { mutableStateOf(initial(existing?.skinSubscapular)) }
    var skinAbdomen by remember { mutableStateOf(initial(existing?.skinAbdomen)) }
    var skinSuprailiac by remember { mutableStateOf(initial(existing?.skinSuprailiac)) }
    var skinThigh by remember { mutableStateOf(initial(existing?.skinThigh)) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val values = listOf(weight, neck, chest, waist, hips, arm, thigh, calf,
        skinChest, skinMidaxillary, skinTriceps, skinSubscapular, skinAbdomen, skinSuprailiac, skinThigh)
        .map(::parseDecimal).filterNotNull().filter { it > 0 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (existing == null) "Nuova misurazione" else "Modifica misurazione", style = GymType.cardTitle, color = c.text)
            Text("Compila solo le voci che vuoi registrare.", color = c.textSecondary)

            OutlinedTextField(dateText, { dateText = it; error = null }, label = { Text("Data (AAAA-MM-GG)") },
                singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

            SectionLabel("Peso")
            NumField("Peso (kg)", weight) { weight = it }

            SectionLabel("Circonferenze (cm)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Collo", neck, Modifier.weight(1f)) { neck = it }
                NumField("Petto", chest, Modifier.weight(1f)) { chest = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Vita", waist, Modifier.weight(1f)) { waist = it }
                NumField("Fianchi", hips, Modifier.weight(1f)) { hips = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Braccio", arm, Modifier.weight(1f)) { arm = it }
                NumField("Coscia", thigh, Modifier.weight(1f)) { thigh = it }
            }
            NumField("Polpaccio", calf) { calf = it }

            SectionLabel("Pliche (mm)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Petto", skinChest, Modifier.weight(1f)) { skinChest = it }
                NumField("Ascellare", skinMidaxillary, Modifier.weight(1f)) { skinMidaxillary = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Tricipite", skinTriceps, Modifier.weight(1f)) { skinTriceps = it }
                NumField("Sottoscapolare", skinSubscapular, Modifier.weight(1f)) { skinSubscapular = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Addome", skinAbdomen, Modifier.weight(1f)) { skinAbdomen = it }
                NumField("Sovrailiaca", skinSuprailiac, Modifier.weight(1f)) { skinSuprailiac = it }
            }
            NumField("Coscia", skinThigh) { skinThigh = it }

            OutlinedTextField(notes, { notes = it }, label = { Text("Note (facoltative)") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = c.error) }
            PrimaryButton("Salva", enabled = values.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                val day = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                if (day == null) {
                    error = "Data non valida: usa AAAA-MM-GG"
                } else {
                    onSave(
                        BodyEntry(
                            id = existing?.id ?: 0,
                            epochDay = day.toEpochDay(),
                            weightKg = parseDecimal(weight)?.takeIf { it > 0 },
                            neckCm = parseDecimal(neck)?.takeIf { it > 0 },
                            chestCm = parseDecimal(chest)?.takeIf { it > 0 },
                            waistCm = parseDecimal(waist)?.takeIf { it > 0 },
                            hipsCm = parseDecimal(hips)?.takeIf { it > 0 },
                            armCm = parseDecimal(arm)?.takeIf { it > 0 },
                            thighCm = parseDecimal(thigh)?.takeIf { it > 0 },
                            calfCm = parseDecimal(calf)?.takeIf { it > 0 },
                            skinChest = parseDecimal(skinChest)?.takeIf { it > 0 },
                            skinMidaxillary = parseDecimal(skinMidaxillary)?.takeIf { it > 0 },
                            skinTriceps = parseDecimal(skinTriceps)?.takeIf { it > 0 },
                            skinSubscapular = parseDecimal(skinSubscapular)?.takeIf { it > 0 },
                            skinAbdomen = parseDecimal(skinAbdomen)?.takeIf { it > 0 },
                            skinSuprailiac = parseDecimal(skinSuprailiac)?.takeIf { it > 0 },
                            skinThigh = parseDecimal(skinThigh)?.takeIf { it > 0 },
                            notes = notes.trim(),
                            measuredAt = existing?.measuredAt ?: System.currentTimeMillis(),
                        )
                    )
                }
            })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { text -> onChange(text.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp), modifier = modifier,
    )
}
