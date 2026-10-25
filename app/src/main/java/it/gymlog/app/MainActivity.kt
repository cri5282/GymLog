package it.gymlog.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.ThemeMode
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymTheme
import it.gymlog.app.ui.nav.AppNavHost
import it.gymlog.app.widget.GymWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        GymWidgetProvider.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GymLogApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            GymTheme(settings) {
                Surface(Modifier.fillMaxSize(), color = Gym.colors.background) { AppNavHost() }
            }
        }
    }
}
