package it.gymlog.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "exhist-$exerciseId") { ExerciseHistoryViewModel(exerciseId, it.workouts, it.exercises) }
    val name by vm.name.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    Column {
        TopAppBar(
            title = { Text(name) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
        )
        if (days.isEmpty()) ListItem(headlineContent = { Text("Nessuna serie registrata.") })
        LazyColumn {
            items(days, key = { it.session.id }) { d ->
                ListItem(
                    headlineContent = { Text(formatDate(d.session.startedAt)) },
                    supportingContent = { Text(d.sets.joinToString("   ") { it.label() }) },
                    overlineContent = { Text(d.session.title) },
                )
                HorizontalDivider()
            }
        }
    }
}
