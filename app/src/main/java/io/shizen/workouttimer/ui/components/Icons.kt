package io.shizen.workouttimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.shizen.workouttimer.ui.theme.WT

/** Line-icon path data (24×24 viewBox), ported from the design's icon set. */
val WtIcons: Map<String, String> = mapOf(
    "play" to "M8 5v14l11-7z",
    "pause" to "M7 5h3v14H7zM14 5h3v14h-3z",
    "check" to "M5 13l4 4L19 7",
    "next" to "M6 5l9 7-9 7zM17 5v14",
    "plus" to "M12 5v14M5 12h14",
    "minus" to "M5 12h14",
    "x" to "M6 6l12 12M18 6L6 18",
    "trash" to "M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12",
    "copy" to "M8 8h11v11H8zM5 16V5h11",
    "edit" to "M4 20h4L18 10l-4-4L4 16zM14 6l4 4",
    "chevR" to "M9 6l6 6-6 6",
    "chevL" to "M15 6l-6 6 6 6",
    "chevD" to "M6 9l6 6 6-6",
    "back" to "M11 6l-6 6 6 6M5 12h14",
    "clock" to "M12 12V8M12 12l3.5 2M12 3a9 9 0 100 18 9 9 0 000-18z",
    "history" to "M3 12a9 9 0 109-9 9 9 0 00-7 3.5M3 4v4h4M12 8v4l3 2",
    "dumbbell" to "M3 9v6M6 7v10M18 7v10M21 9v6M6 12h12",
    "flame" to "M12 3c1 3 4 4 4 8a4 4 0 11-8 0c0-2 1-3 2-4 0 2 2 2 2 4",
    "bell" to "M6 9a6 6 0 1112 0c0 5 2 6 2 6H4s2-1 2-6M10 20a2 2 0 004 0",
    "bellOff" to "M6 9a6 6 0 0110-4M18 12c0 3 2 5 2 5H8M4 20s2-1 2-6M10 20a2 2 0 004 0M4 4l16 16",
    "speaker" to "M4 9h3l5-4v14l-5-4H4zM15.5 9a3 3 0 010 6M18.5 6.5a7 7 0 010 11",
    "speakerOff" to "M4 9h3l5-4v14l-5-4H4zM16 9.5l5 5M21 9.5l-5 5",
    "vibrate" to "M11 6h2v12h-2zM7 9v6M17 9v6M3 11v2M21 11v2",
    "vibrateOff" to "M11 6h2v12h-2zM7 9v6M17 9v6M4 4l16 16",
    "drag" to "M9 6h.01M15 6h.01M9 12h.01M15 12h.01M9 18h.01M15 18h.01",
    "star" to "M12 4l2.3 5 5.2.5-4 3.4 1.3 5.1L12 20.5 7.2 23l1.3-5.1-4-3.4 5.2-.5z",
    "add" to "M12 5v14M5 12h14",
    "layers" to "M12 4l8 4-8 4-8-4 8-4zM4 12l8 4 8-4M4 16l8 4 8-4",
)

private val FILLED = setOf("play", "star")

@Composable
fun WtIcon(
    name: String,
    size: Dp = 22.dp,
    color: Color = WT.Text,
    strokeWidth: Float = 2f,
    modifier: Modifier = Modifier,
) {
    val data = WtIcons[name] ?: ""
    val filled = name in FILLED
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(modifier.size(size)) {
        val scale = this.size.minDimension / 24f
        scale(scale, scale, pivot = Offset.Zero) {
            if (filled) {
                drawPath(path, color)
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
