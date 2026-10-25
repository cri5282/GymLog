package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.domain.SetDraft
import it.gymlog.app.domain.Target
import it.gymlog.app.ui.common.PhotoCarousel
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.design.AccentPill
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.SecondaryButton
import it.gymlog.app.ui.programs.formatRest

fun Target.label(rest: Int): String {
    val reps = if (repMin == repMax) "$repMin" else "$repMin-$repMax"
    val rir = if (rirMin == rirMax) "$rirMin" else "$rirMin-$rirMax"
    return "$sets × $reps · RIR $rir · recupero ${formatRest(rest)}"
}

/** "80 kg × 8, 8, 7" (stesso carico) oppure "80×8, 82,5×7". */
fun lastTimeLabel(sets: List<SetDraft>): String {
    if (sets.isEmpty()) return "Nessuno storico"
    if (sets.any { it.split }) {
        return sets.joinToString { "SX ${formatKg(it.weightKg)}×${it.reps} · DX ${formatKg(it.weightKgR ?: it.weightKg)}×${it.repsR ?: it.reps}" }
    }
    val loads = sets.map { it.weightKg }.distinct()
    return if (loads.size == 1) "${formatKg(loads[0])} kg × " + sets.joinToString { "${it.reps}" }
    else sets.joinToString { "${formatKg(it.weightKg)}×${it.reps}" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePage(
    page: PageState, index: Int, count: Int, active: Int?, bottomPadding: Dp,
    rowActions: (Int) -> RowActions, onApplySuggestion: () -> Unit, onAddSet: () -> Unit,
    onAddPhoto: () -> Unit, onShowNotes: () -> Unit,
) {
    val p = page.planned
    val e = p.exercise.exercise
    val c = Gym.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("ESERCIZIO ${index + 1}/$count", style = GymType.label, color = c.textSecondary) }
        item {
            val photos = p.exercise.sortedPhotos.map { it.filePath }
            if (photos.isNotEmpty()) PhotoCarousel(photos, height = 240)
            else GymCard(Modifier.aspectRatio(16 / 9f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SecondaryButton("Aggiungi foto del macchinario", onAddPhoto, icon = Icons.Rounded.PhotoCamera)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, style = GymType.cardTitle, color = c.text, modifier = Modifier.weight(1f))
                if (e.executionNotes.isNotBlank()) IconButton(onClick = onShowNotes) { Icon(Icons.Outlined.Info, "Note di esecuzione", tint = c.text) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 6.dp)) {
                GymChip(e.primaryMuscle.label)
                if (e.lengthBias != LengthBias.NONE) GymChip(e.lengthBias.label)
                p.supersetGroup?.let { GymChip("Superserie $it") }
                p.bestKg?.takeIf { it > 0 }?.let { GymChip("Record: ${formatKg(it)} kg") }
            }
            Text(p.target?.label(p.restSeconds) ?: "Aggiunto oggi · recupero ${formatRest(p.restSeconds)}",
                color = c.accent, style = MaterialTheme.typography.titleSmall)
            Text("Ultima volta: ${lastTimeLabel(p.lastTime)}", color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        p.suggestedKg?.let { kg ->
            item {
                AccentPill("Hai chiuso tutte le serie al massimo — prova ${formatKg(kg)} kg", Icons.Rounded.Lightbulb,
                    onClick = onApplySuggestion, modifier = Modifier.fillMaxWidth())
            }
        }
        item { SetTable(page.rows, active, rowActions, onAddSet, single = page.single) }
    }
}

