package io.shizen.workouttimer.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * The design specifies Manrope (clean geometric sans) for UI text and
 * JetBrains Mono for numerics. We map these to the platform's geometric
 * sans and monospace families so the app stays asset- and network-free
 * while preserving the intended visual rhythm.
 */
object WtFonts {
    val Sans: FontFamily = FontFamily.SansSerif
    val Mono: FontFamily = FontFamily.Monospace
}
