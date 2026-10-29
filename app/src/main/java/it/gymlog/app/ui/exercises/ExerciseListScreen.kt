package it.gymlog.app.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.ExerciseWithDetails
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.design.AccentPill
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.programs.ExerciseThumb

/** Campo di ricerca arrotondato su fondo "raised". */
@Composable
fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Gym.colors
    TextField(
        value = query, onValueChange = onChange, singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.textSecondary) },
        placeholder = { Text("Cerca…", color = c.textSecondary) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.raised, unfocusedContainerColor = c.raised,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun MuscleFilter(selected: Muscle?, onSelect: (Muscle?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GymChip("Tutti", selected = selected == null, onClick = { onSelect(null) })
        Muscle.entries.forEach { m -> GymChip(m.label, selected = selected == m, onClick = { onSelect(if (selected == m) null else m) }) }
    }
}

@Composable
fun ExerciseCard(e: ExerciseWithDetails, onClick: () -> Unit) {
    val c = Gym.colors
    GymCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExerciseThumb(e)
            Column(Modifier.weight(1f)) {
                Text(e.exercise.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(buildString {
                    append(e.exercise.primaryMuscle.label)
                    if (e.exercise.lengthBias != LengthBias.NONE) append(" · ${e.exercise.lengthBias.label}")
                }, color = c.textSecondary)
            }
            if (e.exercise.needsReview) AccentPill("da completare")
            else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
        }
    }
}

@Composable
fun ExerciseListScreen(onOpen: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val vm = appViewModel { ExerciseListViewModel(it.exercises) }
    val list by vm.exercises.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val muscle by vm.muscle.collectAsStateWithLifecycle()
    val c = Gym.colors

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenHeader("Esercizi", "${list.size} esercizi") {
                RoundIconButton(Icons.Rounded.Settings, "Impostazioni", onOpenSettings)
            }
            SearchField(query, { vm.query.value = it }, Modifier.padding(horizontal = 16.dp))
            MuscleFilter(muscle) { vm.muscle.value = it }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                GymCard(onClick = { onOpen(0) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(c.accentContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = c.accent)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Crea il tuo esercizio", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text("nome e muscolo bastano", color = c.textSecondary)
                        }
                        Text("+", style = MaterialTheme.typography.headlineSmall, color = c.accent)
                    }
                }
            }
        }
        items(list, key = { it.exercise.id }) { e ->
            Box(Modifier.padding(horizontal = 16.dp)) { ExerciseCard(e) { onOpen(e.exercise.id) } }
        }
    }
}
