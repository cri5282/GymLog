package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.domain.Target
import it.gymlog.app.ui.common.PhotoCarousel
import it.gymlog.app.ui.common.formatKg

fun Target.label(): String {
    val reps = if (repMin == repMax) "$repMin" else "$repMin-$repMax"
    val rir = if (rirMin == rirMax) "$rirMin" else "$rirMin-$rirMax"
    return "$sets × $reps @ RIR $rir"
}

/** "80 kg × 8, 8, 7" (stesso carico) oppure "80×8, 82,5×7". */
fun lastTimeLabel(sets: List<SetDraft>): String {
    if (sets.isEmpty()) return "Nessuno storico"
    val loads = sets.map { it.weightKg }.distinct()
    return if (loads.size == 1) "${formatKg(loads[0])} kg × " + sets.joinToString { "${it.reps}" }
    else sets.joinToString { "${formatKg(it.weightKg)}×${it.reps}" }
}

@Composable
fun ExercisePage(page: PageState, rowActions: (Int) -> RowActions, onApplySuggestion: () -> Unit, onAddSet: () -> Unit) {
    val p = page.planned
    val e = p.exercise.exercise
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PhotoCarousel(p.exercise.sortedPhotos.map { it.filePath }) }
        item {
            Text(e.name, style = MaterialTheme.typography.headlineSmall)
            Text(p.target?.label() ?: "Esercizio aggiunto oggi", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text("Ultima volta: ${lastTimeLabel(p.lastTime)}", style = MaterialTheme.typography.bodyMedium)
            if (e.executionNotes.isNotBlank()) {
                Text(e.executionNotes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
        p.suggestedKg?.let { kg ->
            item {
                Card(
                    onClick = onApplySuggestion,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null) },
                        headlineContent = { Text("Prova ${formatKg(kg)} kg (+${formatKg(page.stepKg)})") },
                        supportingContent = { Text("L'ultima volta hai chiuso tutte le serie al massimo delle ripetizioni. Tocca per applicare.") },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        }
        itemsIndexed(page.rows) { i, row -> SetRow(i, row, rowActions(i)) }
        item {
            OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Default.Add, null); Text("Aggiungi serie", Modifier.padding(start = 8.dp))
            }
        }
    }
}
