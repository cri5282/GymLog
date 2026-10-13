package it.gymlog.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
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
import it.gymlog.app.ui.exercises.ExerciseEditScreen
import it.gymlog.app.ui.exercises.ExerciseListScreen
import it.gymlog.app.ui.settings.SettingsScreen

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
            composable(Routes.TODAY) { Placeholder("Oggi") }
            composable(Routes.HISTORY) { Placeholder("Storico") }
            composable(Routes.EXERCISES) { ExerciseListScreen(onOpen = { nav.navigate(Routes.exerciseEdit(it)) }) }
            composable(Routes.EXERCISE_EDIT, listOf(navArgument("exerciseId") { type = NavType.LongType })) { entry ->
                ExerciseEditScreen(entry.arguments!!.getLong("exerciseId"), onBack = { nav.popBackStack() })
            }
            composable(Routes.PROGRAMS) { Placeholder("Schede") }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.WORKOUT, listOf(navArgument("sessionId") { type = NavType.LongType })) { Placeholder("Allenamento") }
        }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
