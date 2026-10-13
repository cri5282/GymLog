package it.gymlog.app.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.appViewModel

/** Scelta di un esercizio dalla libreria, con ricerca. */
@Composable
fun ExercisePickerDialog(title: String, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val vm = appViewModel(key = "picker") { ExerciseListViewModel(it.exercises) }
    val list by vm.exercises.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Cerca") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(list, key = { it.exercise.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.exercise.name) },
                            supportingContent = { Text(e.exercise.primaryMuscle.label) },
                            modifier = Modifier.clickable { onPick(e.exercise.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
