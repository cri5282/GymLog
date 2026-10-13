package it.gymlog.app.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.ui.common.ScreenTitle
import it.gymlog.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(onOpen: (Long) -> Unit) {
    val vm = appViewModel { ExerciseListViewModel(it.exercises) }
    val list by vm.exercises.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val muscle by vm.muscle.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle("Esercizi", Modifier.padding(horizontal = 16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Cerca") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = muscle == null, onClick = { vm.muscle.value = null }, label = { Text("Tutti") })
                Muscle.entries.forEach { m ->
                    FilterChip(
                        selected = muscle == m,
                        onClick = { vm.muscle.value = if (muscle == m) null else m },
                        label = { Text(m.label) },
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
                items(list, key = { it.exercise.id }) { e ->
                    ListItem(
                        headlineContent = { Text(e.exercise.name) },
                        supportingContent = {
                            Text(buildString {
                                append(e.exercise.primaryMuscle.label)
                                if (e.secondary.isNotEmpty()) append(" · " + e.secondary.joinToString { it.muscle.label })
                            })
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (e.photos.isNotEmpty()) Icon(Icons.Default.PhotoCamera, "Ha foto")
                                if (e.exercise.needsReview) {
                                    AssistChip(onClick = { onOpen(e.exercise.id) }, label = { Text("da completare") },
                                        modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpen(e.exercise.id) },
                    )
                    androidx.compose.material3.HorizontalDivider()
                }
            }
        }
        LargeFloatingActionButton(
            onClick = { onOpen(0) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, "Nuovo esercizio") }
    }
}
