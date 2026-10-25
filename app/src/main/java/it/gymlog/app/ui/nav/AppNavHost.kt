package it.gymlog.app.ui.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.gymlog.app.GymLogApp
import it.gymlog.app.ui.body.BodyScreen
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.exercises.ExerciseEditScreen
import it.gymlog.app.ui.exercises.ExerciseListScreen
import it.gymlog.app.ui.history.ExerciseHistoryScreen
import it.gymlog.app.ui.home.HomeScreen
import it.gymlog.app.ui.history.HistoryScreen
import it.gymlog.app.ui.history.SessionDetailScreen
import it.gymlog.app.ui.programs.DayEditScreen
import it.gymlog.app.ui.plan.PlanScreen
import it.gymlog.app.ui.settings.SettingsScreen
import it.gymlog.app.ui.stats.ExerciseStatsScreen
import it.gymlog.app.ui.stats.StatsScreen
import it.gymlog.app.ui.workout.WorkoutScreen

object Routes {
    const val HOME = "home"
    const val PLAN = "plan"
    const val STATS = "stats"
    const val EXERCISES = "exercises"
    const val SETTINGS = "settings"
    const val BODY = "body"
    const val WORKOUT = "workout/{sessionId}"
    const val HISTORY = "history"
    const val SESSION = "history/{sessionId}"
    const val EXERCISE_HISTORY = "exerciseHistory/{exerciseId}"
    const val EXERCISE_STATS = "exerciseStats/{exerciseId}"
    const val EXERCISE_EDIT = "exercise/{exerciseId}"
    const val DAY_EDIT = "day/{dayId}"

    fun workout(sessionId: Long) = "workout/$sessionId"
    fun session(sessionId: Long) = "history/$sessionId"
    fun exerciseHistory(exerciseId: Long) = "exerciseHistory/$exerciseId"
    fun exerciseStats(exerciseId: Long) = "exerciseStats/$exerciseId"
    fun exerciseEdit(exerciseId: Long) = "exercise/$exerciseId"
    fun dayEdit(dayId: Long) = "day/$dayId"

    val withBottomBar = setOf(HOME, PLAN, STATS, BODY, EXERCISES)
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val start = appViewModel(key = "start") { StartViewModel(it.programs, it.workouts) }
    val resuming by start.resuming.collectAsStateWithLifecycle()
    val days by start.days.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val openWorkout: (Long) -> Unit = { nav.navigate(Routes.workout(it)) { launchSingleTop = true } }

    Scaffold(
        containerColor = Gym.colors.background,
        bottomBar = {
            if (current in Routes.withBottomBar) {
                BottomBar(
                    current = current,
                    resuming = resuming,
                    onNavigate = { route ->
                        nav.navigate(route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onStart = { start.onStart(open = openWorkout, pick = { picking = true }) },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkout = openWorkout,
                    onOpenSession = { nav.navigate(Routes.session(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onPickRoutine = { picking = true },
                )
            }
            composable(Routes.PLAN) { PlanScreen(onOpenRoutine = { nav.navigate(Routes.dayEdit(it)) }) }
            composable(Routes.STATS) {
                StatsScreen(
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onOpenSession = { nav.navigate(Routes.session(it)) },
                    onOpenExerciseStats = { nav.navigate(Routes.exerciseStats(it)) },
                    onOpenBody = { nav.navigate(Routes.BODY) },
                )
            }
            composable(Routes.BODY) {
                BodyScreen(onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.HISTORY) { HistoryScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.session(it)) }) }
            composable(Routes.SESSION, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                SessionDetailScreen(entry.arguments!!.getLong("sessionId"), onBack = { nav.popBackStack() },
                    onOpenExercise = { nav.navigate(Routes.exerciseHistory(it)) })
            }
            composable(Routes.EXERCISE_HISTORY, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("exerciseId")
                ExerciseHistoryScreen(id, onBack = { nav.popBackStack() },
                    onOpenStats = { nav.navigate(Routes.exerciseStats(id)) })
            }
            composable(Routes.EXERCISE_STATS, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("exerciseId")
                ExerciseStatsScreen(id, onBack = { nav.popBackStack() },
                    onOpenHistory = { nav.navigate(Routes.exerciseHistory(id)) })
            }
            composable(Routes.EXERCISES) { ExerciseListScreen(onOpen = { nav.navigate(Routes.exerciseEdit(it)) }) }
            composable(Routes.EXERCISE_EDIT, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                ExerciseEditScreen(entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate(Routes.exerciseHistory(it)) })
            }
            composable(Routes.DAY_EDIT, listOf(navArgument("dayId") { type = NavType.LongType })) { entry ->
                DayEditScreen(entry.arguments!!.getLong("dayId"), onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.WORKOUT, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                val container = (LocalContext.current.applicationContext as GymLogApp).container
                WorkoutScreen(entry.arguments!!.getLong("sessionId"), container.restTimer, onExit = {
                    // Ignora un secondo tocco durante l'animazione di uscita (altrimenti toglierebbe anche la Home).
                    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
                })
            }
        }
    }
    if (picking) RoutinePickerSheet(days, onPick = { id -> picking = false; start.startDay(id, openWorkout) },
        onDismiss = { picking = false })
}
