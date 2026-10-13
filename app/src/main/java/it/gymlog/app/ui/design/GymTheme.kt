package it.gymlog.app.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.data.settings.ThemeMode

val LocalGymColors = staticCompositionLocalOf { gymColors(it.gymlog.app.data.settings.Accent.GREEN, dark = true) }

/** Accesso rapido ai colori e agli stili di GymLog. */
object Gym {
    val colors: GymColors
        @Composable @ReadOnlyComposable get() = LocalGymColors.current
}

/** Stili di testo del §2 della spec. */
object GymType {
    val screenTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp)
    val cardTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    val number = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
    val timer = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
    val stat = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
    val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
}

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GymTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = gymColors(settings.accent, dark)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent, onPrimary = c.onAccent,
        primaryContainer = c.accentContainer, onPrimaryContainer = c.text,
        secondary = c.accent, onSecondary = c.onAccent,
        secondaryContainer = c.raised, onSecondaryContainer = c.text,
        tertiary = c.resume, onTertiary = c.onAccent,
        background = c.background, onBackground = c.text,
        surface = c.card, onSurface = c.text,
        surfaceVariant = c.raised, onSurfaceVariant = c.textSecondary,
        surfaceContainerLowest = c.background, surfaceContainerLow = c.card,
        surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerHighest = c.raised,
        inverseSurface = c.raised, inverseOnSurface = c.text,
        outline = c.border, outlineVariant = c.border,
        error = c.error,
    )
    val base = Typography()
    val typography = base.copy(
        headlineLarge = GymType.screenTitle,
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    CompositionLocalProvider(LocalGymColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }
}
