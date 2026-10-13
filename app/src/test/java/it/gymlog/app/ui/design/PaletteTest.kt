package it.gymlog.app.ui.design

import androidx.compose.ui.graphics.Color
import it.gymlog.app.data.settings.Accent
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    @Test fun darkGreen_matchesSpec() {
        val c = gymColors(Accent.GREEN, dark = true)
        assertEquals(Color(0xFF000000), c.background)
        assertEquals(Color(0xFF1C1C1E), c.card)
        assertEquals(Color(0xFF2C2C2E), c.raised)
        assertEquals(Color(0xFF32D74B), c.accent)
        assertEquals(Color(0xFF000000), c.onAccent)
        assertEquals(Color(0xFFFF9F1C), c.resume)
    }

    @Test fun lightBlue_matchesSpec() {
        val c = gymColors(Accent.BLUE, dark = false)
        assertEquals(Color(0xFFF2F2F7), c.background)
        assertEquals(Color(0xFFFFFFFF), c.card)
        assertEquals(Color(0xFF1F63E0), c.accent)
        assertEquals(Color(0xFFFFFFFF), c.onAccent)
    }

    @Test fun orangeAccent_resumeIsYellow() {
        assertEquals(Color(0xFFFFD60A), gymColors(Accent.ORANGE, dark = true).resume)
    }
}
