package io.shizen.workouttimer.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.R
import io.shizen.workouttimer.data.Step
import io.shizen.workouttimer.data.StepKind
import io.shizen.workouttimer.timer.ActiveState
import io.shizen.workouttimer.ui.components.Dot
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.components.Stepper
import io.shizen.workouttimer.ui.screens.stepColor
import io.shizen.workouttimer.ui.screens.stepLabelRes
import io.shizen.workouttimer.ui.screens.timerText
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WorkoutTimerTheme
import io.shizen.workouttimer.ui.theme.WtFonts
import kotlinx.coroutines.flow.StateFlow

/** Overlay content bound to the live workout state; renders nothing once the workout ends. */
@Composable
fun WorkoutOverlay(
    stateFlow: StateFlow<ActiveState?>,
    onSetReps: (workIndex: Int, value: Int) -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val state by stateFlow.collectAsState()
    val current = state ?: return
    WorkoutTimerTheme {
        OverlayCard(current, onSetReps, onClose, onOpenApp)
    }
}

/**
 * Compact card shown over other apps: phase, remaining time, the set whose reps
 * can be adjusted (current set while working, the one just done while resting)
 * and the next set. Tapping the card brings the app back.
 */
@Composable
private fun OverlayCard(
    state: ActiveState,
    onSetReps: (workIndex: Int, value: Int) -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val cur = state.current
    val color = stepColor(cur)
    val frac = (1.0 - state.remaining / cur.duration).coerceIn(0.0, 1.0).toFloat()
    val repsStep = when (cur.kind) {
        StepKind.WORK -> cur
        StepKind.REST -> state.prevWork
        StepKind.COUNTDOWN -> null
    }
    val repsRole = stringResource(if (cur.kind == StepKind.WORK) R.string.active_role_now else R.string.active_role_last_set)
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(shape)
            .background(WT.Surface)
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenApp,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // phase + close
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dot(color, sizeDp = 7)
            Text(
                stringResource(stepLabelRes(cur)),
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = color,
            )
            IconBtn(
                "x",
                onClick = onClose,
                contentDescription = stringResource(R.string.overlay_hide),
                size = 28,
                iconSize = 14,
                bg = WT.Surface2,
                color = WT.Muted,
            )
        }

        // timer
        Text(
            timerText(cur.kind, state.remaining),
            fontFamily = WtFonts.Mono,
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            color = WT.Text,
            letterSpacing = (-1).sp,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(WT.Surface2),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(frac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }

        // set being logged + reps counter
        if (repsStep != null) {
            OverlayStepLine(role = repsRole, step = repsStep)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.active_reps),
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = WT.Muted,
                )
                Stepper(
                    value = state.reps[repsStep.workIndex] ?: 0,
                    onValueChange = { value -> onSetReps(repsStep.workIndex, value) },
                    min = 0,
                    max = 999,
                    width = 120,
                )
            }
        }

        // next set
        Box(Modifier.fillMaxWidth().height(1.dp).background(WT.Line))
        OverlayStepLine(role = stringResource(R.string.active_role_next), step = state.nextWork)
    }
}

/** Role caption, set number and exercise name of a WORK step (a dash when there is none). */
@Composable
private fun OverlayStepLine(role: String, step: Step?) {
    Column(Modifier.fillMaxWidth().alpha(if (step != null) 1f else 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                role,
                modifier = Modifier.weight(1f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = WT.Faint,
            )
            Text(
                if (step != null) {
                    stringResource(R.string.active_set_progress, step.setNumber, step.totalSets)
                } else {
                    stringResource(R.string.active_set_progress_empty)
                },
                fontFamily = WtFonts.Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WT.Muted,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            step?.exerciseName ?: "-",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WT.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
