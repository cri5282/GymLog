package it.gymlog.app.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymType

/** Scelta di un esercizio dalla libreria, con ricerca e filtro muscoli, in un pannello a tutta altezza. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(title: String, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val vm = appViewModel(key = "picker") { ExerciseListViewModel(it.exercises) }
    val list by vm.exercises.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val muscle by vm.muscle.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Gym.colors.background) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Text(title, style = GymType.cardTitle, color = Gym.colors.text, modifier = Modifier.padding(horizontal = 16.dp))
            SearchField(query, { vm.query.value = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            MuscleFilter(muscle) { vm.muscle.value = it }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.exercise.id }) { e ->
                    Box(Modifier.padding(horizontal = 16.dp)) { ExerciseCard(e) { onPick(e.exercise.id) } }
                }
            }
        }
    }
}
