package it.gymlog.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.gymlog.app.data.settings.Accent

/** Ruoli di colore di GymLog (spec §2). */
data class GymColors(
    val background: Color,
    val card: Color,
    val raised: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val resume: Color,
    val error: Color,
    val dark: Boolean,
) {
    /** Colori dei tipi di serie non allenanti, leggibili su entrambi i temi. */
    val warmup: Color get() = if (dark) Color(0xFFFFD60A) else Color(0xFF7A5F00)
    val drop: Color get() = if (dark) Color(0xFFA06BFF) else Color(0xFF6A35C8)
}

fun gymColors(accent: Accent, dark: Boolean): GymColors {
    val a = Color(if (dark) accent.dark else accent.light)
    val orange = accent == Accent.ORANGE
    return if (dark) GymColors(
        background = Color(0xFF000000),
        card = Color(0xFF1C1C1E),
        raised = Color(0xFF2C2C2E),
        border = Color(0xFF3A3A3C),
        text = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8E8E93),
        accent = a,
        onAccent = Color(0xFF000000),
        accentContainer = a.copy(alpha = 0.18f),
        resume = if (orange) Color(0xFFFFD60A) else Color(0xFFFF9F1C),
        error = Color(0xFFFF453A),
        dark = true,
    ) else GymColors(
        background = Color(0xFFF2F2F7),
        card = Color(0xFFFFFFFF),
        raised = Color(0xFFE5E5EA),
        border = Color(0xFFD1D1D6),
        text = Color(0xFF000000),
        textSecondary = Color(0xFF6C6C70),
        accent = a,
        onAccent = readableOn(a),
        accentContainer = a.copy(alpha = 0.14f),
        resume = if (orange) Color(0xFFB38F00) else Color(0xFFD97A00),
        error = Color(0xFFD70015),
        dark = false,
    )
}

/** Nero o bianco, scegliendo il contrasto maggiore sul colore di sfondo. */
private fun readableOn(background: Color): Color {
    val l = background.luminance()
    val black = (l + 0.05) / 0.05
    val white = 1.05 / (l + 0.05)
    return if (black >= white) Color.Black else Color.White
}
