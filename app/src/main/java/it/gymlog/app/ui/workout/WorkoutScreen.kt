package it.gymlog.app.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.timer.TimerState
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.exercises.ExercisePickerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Picker { ADD, REPLACE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(sessionId: Long, timer: RestTimer, onExit: () -> Unit) {
    val vm = appViewModel(key = "workout-$sessionId") { WorkoutViewModel(sessionId, it.workouts, it.settings, timer) }
    val state by vm.state.collectAsStateWithLifecycle()
    val timerState by vm.timerState.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    val pager = rememberPagerState { state.pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notificationsDenied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(state.title)
                    if (state.pages.isNotEmpty()) {
                        Text("Esercizio ${pager.currentPage + 1}/${state.pages.size}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            navigationIcon = { IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
            actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Altro") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Aggiungi esercizio") }, onClick = { menu = false; picker = Picker.ADD })
                    val canReplace = state.pages.getOrNull(pager.currentPage)?.rows?.none { it.done } == true
                    DropdownMenuItem(text = { Text("Sostituisci esercizio (solo oggi)") }, enabled = canReplace,
                        onClick = { menu = false; picker = Picker.REPLACE })
                    DropdownMenuItem(text = { Text("Termina allenamento") }, onClick = { menu = false; confirmFinish = true })
                }
            },
        )
        if (notificationsDenied) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, onClick = { notificationsDenied = false }) {
                Text("Senza notifiche il timer suona solo con l'app aperta. Tocca per chiudere.",
                    Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Box(Modifier.weight(1f)) {
            if (state.pages.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nessun esercizio. Aggiungine uno dal menu ⋮")
                    Button(onClick = { picker = Picker.ADD }, Modifier.padding(top = 16.dp).heightIn(min = 56.dp)) { Text("Aggiungi esercizio") }
                }
            } else {
                HorizontalPager(pager, Modifier.fillMaxSize(), key = { state.pages.getOrNull(it)?.planned?.order ?: it }) { index ->
                    val page = state.pages.getOrNull(index) ?: return@HorizontalPager
                    ExercisePage(
                        page = page,
                        rowActions = { row ->
                            RowActions(
                                weightStep = { vm.changeWeight(index, row, it) },
                                setWeight = { vm.setWeight(index, row, it) },
                                repsStep = { vm.changeReps(index, row, it) },
                                setReps = { vm.setReps(index, row, it) },
                                setRir = { vm.setRir(index, row, it) },
                                setType = { vm.setType(index, row, it) },
                                setNote = { vm.setNote(index, row, it) },
                                confirm = { vm.confirm(index, row) },
                            )
                        },
                        onApplySuggestion = { vm.applySuggestion(index) },
                        onAddSet = { vm.addSet(index) },
                    )
                }
            }
        }
        RestTimerBar(timerState, onAdd = { vm.addRestSeconds(30) }, onSkip = vm::skipRest)
    }

    when (picker) {
        Picker.ADD -> ExercisePickerDialog("Aggiungi esercizio", onPick = { id ->
            picker = null
            vm.addExercise(id)
            scope.launch { delay(300); pager.animateScrollToPage(state.pages.lastIndex.coerceAtLeast(0)) }
        }, onDismiss = { picker = null })
        Picker.REPLACE -> ExercisePickerDialog("Sostituisci con", onPick = { id ->
            picker = null; vm.replaceExercise(pager.currentPage, id)
        }, onDismiss = { picker = null })
        null -> {}
    }
    if (confirmFinish) ConfirmDialog("Terminare l'allenamento?",
        "Le serie non confermate con ✓ non verranno salvate.", "Termina",
        onConfirm = { confirmFinish = false; vm.finish() }, onDismiss = { confirmFinish = false })
    state.recap?.let { RecapDialog(it, onClose = onExit) }
}

@Composable
fun RestTimerBar(state: TimerState, onAdd: () -> Unit, onSkip: () -> Unit) {
    val end = state.endAtMillis ?: return
    val remaining by produceState(state.remainingSeconds(System.currentTimeMillis()), end) {
        while (true) {
            value = state.remainingSeconds(System.currentTimeMillis())
            if (value == 0) break
            delay(250)
        }
    }
    Surface(color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (remaining > 0) "Recupero" else "Recupero finito", style = MaterialTheme.typography.labelLarge)
                Text("%d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.heightIn(min = 56.dp)) { Text("+30 s") }
            Button(onClick = onSkip, modifier = Modifier.heightIn(min = 56.dp)) { Text(if (remaining > 0) "Salta" else "Chiudi") }
        }
    }
}
