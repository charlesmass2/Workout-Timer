package io.shizen.workouttimer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette converted from the design's oklch tokens to sRGB.
 * Technical / data-focused fitness-watch aesthetic, dark theme.
 */
object WT {
    val Bg = Color(0xFF0B0D11)
    val Surface = Color(0xFF14171B)
    val Surface2 = Color(0xFF202328)
    val Line = Color(0xFF2A2E33)
    val Text = Color(0xFFF3F5F8)
    val Muted = Color(0xFF9399A0)
    val Faint = Color(0xFF646970)
    val Accent = Color(0xFF4CC157)
    val Rest = Color(0xFF4EB8E9)
    val Danger = Color(0xFFE8605B)
    val Warn = Color(0xFFE7B643)

    /** High-contrast text colour for placement on the accent / rest fills. */
    val OnAccent = Color(0xFF06140D)
    val OnRest = Color(0xFF04121C)
}
