package it.gymlog.app.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.GymLogApp
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.domain.activeRowIndex
import it.gymlog.app.domain.sessionProgress
import it.gymlog.app.timer.RestTimer
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.exercises.ExerciseEditScreen
import it.gymlog.app.ui.exercises.ExercisePickerDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class Picker { ADD, REPLACE }

@Composable
fun WorkoutScreen(sessionId: Long, timer: RestTimer, onExit: () -> Unit) {
    val vm = appViewModel(key = "workout-$sessionId") { WorkoutViewModel(sessionId, it.workouts, it.settings, timer, it.exercises) }
    val state by vm.state.collectAsStateWithLifecycle()
    val timerState by vm.timerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = (context.applicationContext as GymLogApp).container
    val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
    var picker by remember { mutableStateOf<Picker?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Long?>(null) }
    var notesOpen by remember { mutableStateOf<String?>(null) }
    val pager = rememberPagerState { state.pages.size }
    val scope = rememberCoroutineScope()
    val c = Gym.colors

    // Schermo acceso finché la schermata è visibile (se attivo nelle impostazioni).
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var notificationsDenied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    var cameraFor by rememberSaveable { mutableStateOf<Pair<Int, String>?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraFor?.let { (page, path) ->
            val f = File(path)
            if (ok && f.exists() && f.length() > 0) vm.addPhoto(page, f) else f.delete()
        }
        cameraFor = null
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.accent) }
        return
    }
    val (done, total) = sessionProgress(state.pages.map { p -> p.rows.map { it.done } })
    val timerVisible = timerState.endAtMillis != null
    // Spazio in fondo alla pagina = altezza reale della barra del timer, così non copre l'ultima serie.
    var timerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val bottomPadding = if (timerVisible) with(density) { timerHeightPx.toDp() } + 16.dp else 32.dp

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WorkoutTopBar(
                title = state.title, startedAt = state.startedAt, done = done, total = total,
                canReplace = state.pages.getOrNull(pager.currentPage)?.rows?.none { it.done } == true,
                onClose = onExit, onFinish = { confirmFinish = true },
                onAddExercise = { picker = Picker.ADD }, onReplaceExercise = { picker = Picker.REPLACE },
                onEditExercise = { state.pages.getOrNull(pager.currentPage)?.planned?.exercise?.exercise?.id?.let { editingExercise = it } },
            )
            if (notificationsDenied) {
                Surface(color = c.error.copy(alpha = 0.2f), onClick = { notificationsDenied = false }) {
                    Text("Senza notifiche il timer suona solo con l'app aperta. Tocca per chiudere.",
                        Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = c.text)
                }
            }
            if (state.pages.isEmpty()) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    EmptyState(Icons.Rounded.FitnessCenter, "Nessun esercizio. Aggiungine uno per iniziare.") {
                        PrimaryButton("Aggiungi esercizio", { picker = Picker.ADD }, icon = Icons.Rounded.Add)
                    }
                }
            } else {
                HorizontalPager(pager, Modifier.weight(1f),
                    key = { i -> state.pages.getOrNull(i)?.planned?.let { "${it.order}:${it.exercise.exercise.id}" } ?: "i$i" }) { index ->
                    val page = state.pages.getOrNull(index) ?: return@HorizontalPager
                    val active = activeRowIndex(page.rows.map { it.done }, state.selectedRow[index], allowDone = true)
                    ExercisePage(
                        page = page, index = index, count = state.pages.size, active = active, bottomPadding = bottomPadding,
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
                                select = { vm.selectRow(index, row) },
                                unconfirm = { vm.unconfirm(index, row) },
                                weightStepR = { vm.changeWeight(index, row, it, right = true) },
                                setWeightR = { vm.setWeight(index, row, it, right = true) },
                                repsStepR = { vm.changeReps(index, row, it, right = true) },
                                setRepsR = { vm.setReps(index, row, it, right = true) },
                                setRirR = { vm.setRir(index, row, it, right = true) },
                            )
                        },
                        onApplySuggestion = { vm.applySuggestion(index) },
                        onAddSet = { vm.addSet(index) },
                        onAddPhoto = {
                            val file = container.photos.newCameraFile()
                            cameraFor = index to file.path
                            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                        },
                        onShowNotes = { notesOpen = page.planned.exercise.exercise.executionNotes },
                    )
                }
            }
        }
        RestTimerBar(timerState, onAdjust = vm::addRestSeconds, onSkip = vm::skipRest,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().onSizeChanged { timerHeightPx = it.height })
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
    editingExercise?.let { id ->
        Dialog(
            onDismissRequest = {
                editingExercise = null
                vm.refreshExercise(pager.currentPage)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = c.background) {
                ExerciseEditScreen(
                    id,
                    onBack = { editingExercise = null; vm.refreshExercise(pager.currentPage) },
                    onHistory = {},
                    showHistory = false,
                    showDelete = false,
                )
            }
        }
    }
    notesOpen?.let { notes ->
        AlertDialog(onDismissRequest = { notesOpen = null }, title = { Text("Note di esecuzione") }, text = { Text(notes) },
            confirmButton = { TextButton(onClick = { notesOpen = null }) { Text("OK") } }, containerColor = c.card)
    }
    if (confirmFinish) ConfirmDialog("Terminare l'allenamento?",
        "Le serie non confermate con ✓ non verranno salvate.", "Termina",
        onConfirm = { confirmFinish = false; vm.finish() }, onDismiss = { confirmFinish = false })
    state.recap?.let { RecapSheet(it, state.records, onClose = onExit) }
}
