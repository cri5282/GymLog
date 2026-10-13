package it.gymlog.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import it.gymlog.app.GymLogApp
import it.gymlog.app.ui.exercises.ExerciseEditScreen
import it.gymlog.app.ui.history.ExerciseHistoryScreen
import it.gymlog.app.ui.history.HistoryScreen
import it.gymlog.app.ui.history.SessionDetailScreen
import it.gymlog.app.ui.workout.WorkoutScreen
import it.gymlog.app.ui.exercises.ExerciseListScreen
import it.gymlog.app.ui.programs.DayEditScreen
import it.gymlog.app.ui.programs.ProgramEditScreen
import it.gymlog.app.ui.programs.ProgramListScreen
import it.gymlog.app.ui.settings.SettingsScreen
import it.gymlog.app.ui.today.TodayScreen

object Routes {
    const val TODAY = "today"
    const val WORKOUT = "workout/{sessionId}"
    const val HISTORY = "history"
    const val SESSION = "history/{sessionId}"
    const val EXERCISE_HISTORY = "exerciseHistory/{exerciseId}"
    const val EXERCISES = "exercises"
    const val EXERCISE_EDIT = "exercise/{exerciseId}"
    const val PROGRAMS = "programs"
    const val PROGRAM_EDIT = "program/{programId}"
    const val DAY_EDIT = "day/{dayId}"
    const val SETTINGS = "settings"

    fun workout(sessionId: Long) = "workout/$sessionId"
    fun session(sessionId: Long) = "history/$sessionId"
    fun exerciseHistory(exerciseId: Long) = "exerciseHistory/$exerciseId"
    fun exerciseEdit(exerciseId: Long) = "exercise/$exerciseId"
    fun programEdit(programId: Long) = "program/$programId"
    fun dayEdit(dayId: Long) = "day/$dayId"
}

private data class TopLevel(val route: String, val label: String, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.TODAY, "Oggi", Icons.Default.Today),
    TopLevel(Routes.HISTORY, "Storico", Icons.Default.History),
    TopLevel(Routes.EXERCISES, "Esercizi", Icons.Default.FitnessCenter),
    TopLevel(Routes.PROGRAMS, "Schede", Icons.AutoMirrored.Filled.ListAlt),
    TopLevel(Routes.SETTINGS, "Impostazioni", Icons.Default.Settings),
)

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            if (current?.startsWith("workout/") != true) {
                NavigationBar {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.route,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.padding(padding)) {
            composable(Routes.TODAY) { TodayScreen(onOpenWorkout = { nav.navigate(Routes.workout(it)) }) }
            composable(Routes.HISTORY) { HistoryScreen(onOpen = { nav.navigate(Routes.session(it)) }) }
            composable(Routes.SESSION, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                SessionDetailScreen(entry.arguments!!.getLong("sessionId"), onBack = { nav.popBackStack() },
                    onOpenExercise = { nav.navigate(Routes.exerciseHistory(it)) })
            }
            composable(Routes.EXERCISE_HISTORY, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                ExerciseHistoryScreen(entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() })
            }
            composable(Routes.EXERCISES) { ExerciseListScreen(onOpen = { nav.navigate(Routes.exerciseEdit(it)) }) }
            composable(Routes.EXERCISE_EDIT, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                ExerciseEditScreen(entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate(Routes.exerciseHistory(it)) })
            }
            composable(Routes.PROGRAMS) { ProgramListScreen(onOpen = { nav.navigate(Routes.programEdit(it)) }) }
            composable(Routes.PROGRAM_EDIT, listOf(navArgument("programId") { type = NavType.LongType })) { entry ->
                ProgramEditScreen(entry.arguments!!.getLong("programId"), onBack = { nav.popBackStack() },
                    onOpenDay = { nav.navigate(Routes.dayEdit(it)) })
            }
            composable(Routes.DAY_EDIT, listOf(navArgument("dayId") { type = NavType.LongType })) { entry ->
                DayEditScreen(entry.arguments!!.getLong("dayId"), onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.WORKOUT, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                val container = (LocalContext.current.applicationContext as GymLogApp).container
                WorkoutScreen(entry.arguments!!.getLong("sessionId"), container.restTimer, onExit = { nav.popBackStack() })
            }
        }
    }
}

