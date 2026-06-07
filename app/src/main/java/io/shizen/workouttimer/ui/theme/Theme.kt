package io.shizen.workouttimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
fun WorkoutTimerTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = WT.Accent,
        onPrimary = WT.OnAccent,
        background = WT.Bg,
        onBackground = WT.Text,
        surface = WT.Surface,
        onSurface = WT.Text,
        error = WT.Danger,
    )
    val baseStyle = TextStyle(fontFamily = WtFonts.Sans, color = WT.Text)
    val typography = Typography().run {
        copy(
            displayLarge = displayLarge.merge(baseStyle),
            displayMedium = displayMedium.merge(baseStyle),
            displaySmall = displaySmall.merge(baseStyle),
            headlineLarge = headlineLarge.merge(baseStyle),
            headlineMedium = headlineMedium.merge(baseStyle),
            headlineSmall = headlineSmall.merge(baseStyle),
            titleLarge = titleLarge.merge(baseStyle),
            titleMedium = titleMedium.merge(baseStyle),
            titleSmall = titleSmall.merge(baseStyle),
            bodyLarge = bodyLarge.merge(baseStyle),
            bodyMedium = bodyMedium.merge(baseStyle),
            bodySmall = bodySmall.merge(baseStyle),
            labelLarge = labelLarge.merge(baseStyle),
            labelMedium = labelMedium.merge(baseStyle),
            labelSmall = labelSmall.merge(baseStyle),
        )
    }
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
