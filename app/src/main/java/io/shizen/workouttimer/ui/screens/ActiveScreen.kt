package io.shizen.workouttimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.R
import io.shizen.workouttimer.data.Settings
import io.shizen.workouttimer.data.Step
import io.shizen.workouttimer.data.StepKind
import io.shizen.workouttimer.data.RestType
import io.shizen.workouttimer.data.fmtClock
import io.shizen.workouttimer.timer.ActiveState
import io.shizen.workouttimer.ui.components.Btn
import io.shizen.workouttimer.ui.components.BtnSize
import io.shizen.workouttimer.ui.components.BtnVariant
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.Dot
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.components.Stepper
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts
import kotlin.math.ceil

@Composable
fun ActiveScreen(
    state: ActiveState,
    settings: Settings,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onAdvance: () -> Unit,
    onSetReps: (Int) -> Unit,
    onSetRepsForWork: (Int, Int) -> Unit,
    onFinishNow: () -> Unit,
    onToggleVibrate: () -> Unit,
    onToggleSound: () -> Unit,
) {
    KeepScreenOn()
    var confirmEnd by remember { mutableStateOf(false) }
    // System back asks to finish; while the dialog is open, let back dismiss it.
    BackHandler(enabled = !confirmEnd) { confirmEnd = true }

    val steps = state.steps
    val cur = state.current
    val phase = cur.kind
    val dur = cur.duration
    val frac = (1.0 - state.remaining / dur).coerceIn(0.0, 1.0).toFloat()
    val color = when (phase) {
        StepKind.COUNTDOWN -> WT.Warn
        StepKind.REST -> WT.Rest
        StepKind.WORK -> WT.Accent
    }
    val label = stringResource(
        when (phase) {
            StepKind.COUNTDOWN -> R.string.active_label_get_ready
            StepKind.REST -> if (cur.restType == RestType.SUPERSET) R.string.active_label_block_rest else R.string.active_label_rest
            StepKind.WORK -> R.string.active_label_work
        }
    )
    val totalWork = state.totalWork
    val doneWork = state.doneWork
    val notStarted = !state.started

    Column(Modifier.fillMaxSize().background(WT.Bg)) {
        // top bar
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn("chevD", onClick = { confirmEnd = true }, contentDescription = stringResource(R.string.active_end_workout), size = 38, iconSize = 22, bg = Color.Transparent, color = WT.Muted)
            Text(
                state.workoutName,
                modifier = Modifier.weight(1f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = WT.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmtClock(state.globalElapsed),
                    fontFamily = WtFonts.Mono,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WT.Text,
                )
                Text(
                    stringResource(R.string.active_sets_progress, doneWork, totalWork),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = WT.Faint,
                )
            }
        }

        // overall progress
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WT.Surface),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (totalWork == 0) 0f else doneWork.toFloat() / totalWork)
                    .background(WT.Accent.copy(alpha = 0.6f)),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContextStrip(steps, state.idx)

            if (phase == StepKind.REST) {
                val nextWork = nextWorkStep(steps, state.idx)
                val prevWork = prevWorkStep(steps, state.idx)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RestSetColumn(
                        role = stringResource(R.string.active_role_last_set),
                        step = prevWork,
                        reps = prevWork?.let { state.reps[it.workIndex] } ?: 0,
                        onRepsChange = { r -> prevWork?.let { onSetRepsForWork(it.workIndex, r) } },
                        isNext = false,
                        modifier = Modifier.weight(1f),
                    )
                    RestSetColumn(
                        role = stringResource(R.string.active_role_next_up),
                        step = nextWork,
                        reps = nextWork?.let { state.reps[it.workIndex] } ?: 0,
                        onRepsChange = { r -> nextWork?.let { onSetRepsForWork(it.workIndex, r) } },
                        isNext = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TimerHero(phase, state.remaining, frac, color, label)
                Spacer(Modifier.height(22.dp))
                when (phase) {
                    StepKind.WORK -> {
                        SetDots(cur.totalSets, cur.setNumber)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.active_set_progress, cur.setNumber, cur.totalSets),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WT.Muted,
                            fontFamily = WtFonts.Mono,
                        )
                        Spacer(Modifier.height(12.dp))
                        RepsInput(
                            label = stringResource(R.string.active_reps),
                            value = state.reps[cur.workIndex] ?: 0,
                            onValueChange = onSetReps,
                        )
                    }
                    StepKind.REST -> {
                        Text(stringResource(R.string.active_catch_breath), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Muted, textAlign = TextAlign.Center)
                    }
                    StepKind.COUNTDOWN -> {
                        Text(stringResource(R.string.active_starting_in), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Muted)
                    }
                }
            }
        }

        // control bar
        Box(Modifier.fillMaxWidth().height(1.dp).background(WT.Line))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WT.Surface)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (notStarted) {
                Btn(stringResource(R.string.active_start), onClick = onStart, icon = "play", size = BtnSize.Lg, fillMaxWidth = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(
                        stringResource(if (state.running) R.string.active_pause else R.string.active_resume),
                        onClick = onTogglePause,
                        icon = if (state.running) "pause" else "play",
                        size = BtnSize.Lg,
                        variant = if (state.running) BtnVariant.Ghost else BtnVariant.Primary,
                        modifier = Modifier.width(130.dp),
                    )
                    Btn(
                        stringResource(
                            when (phase) {
                                StepKind.COUNTDOWN -> R.string.active_skip
                                StepKind.REST -> R.string.active_skip_rest
                                StepKind.WORK -> R.string.active_finish_set
                            }
                        ),
                        onClick = onAdvance,
                        icon = "next",
                        size = BtnSize.Lg,
                        variant = if (phase == StepKind.REST) BtnVariant.Rest else BtnVariant.Primary,
                        fillMaxWidth = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBtn(
                    if (settings.vibrate) "vibrate" else "vibrateOff",
                    onClick = onToggleVibrate,
                    contentDescription = stringResource(if (settings.vibrate) R.string.active_vibration_on else R.string.active_vibration_off),
                    active = settings.vibrate,
                    iconSize = 20,
                )
                IconBtn(
                    if (settings.sound) "speaker" else "speakerOff",
                    onClick = onToggleSound,
                    contentDescription = stringResource(if (settings.sound) R.string.active_sound_on else R.string.active_sound_off),
                    active = settings.sound,
                    iconSize = 20,
                )
                Spacer(Modifier.weight(1f))
                Btn(stringResource(R.string.active_finish), onClick = { confirmEnd = true }, variant = BtnVariant.Danger, size = BtnSize.Sm, icon = "check")
            }
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = stringResource(R.string.active_finish_dialog_title),
            body = stringResource(R.string.active_finish_dialog_body),
            confirmLabel = stringResource(R.string.active_finish_dialog_confirm),
            danger = false,
            onConfirm = { confirmEnd = false; onFinishNow() },
            onDismiss = { confirmEnd = false },
        )
    }
}

// ── Timer hero (bar style) ──────────────────────────────────
@Composable
private fun TimerHero(phase: StepKind, remaining: Double, frac: Float, color: Color, label: String) {
    val isCount = phase == StepKind.COUNTDOWN
    val big = if (isCount) ceil(remaining).coerceAtLeast(0.0).toInt().toString() else fmtClock(remaining)
    val fontSize = if (isCount) 120.sp else if (big.length > 4) 76.sp else 92.sp
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dot(color, sizeDp = 7)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = color)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            big,
            fontFamily = WtFonts.Mono,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = WT.Text,
            letterSpacing = (-2).sp,
        )
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(WT.Surface2),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(frac)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color),
            )
        }
    }
}

/** The first WORK step at or after [idx] (used to preview the upcoming set during rest). */
private fun nextWorkStep(steps: List<Step>, idx: Int): Step? {
    var i = idx + 1
    while (i < steps.size) {
        if (steps[i].kind == StepKind.WORK) return steps[i]
        i++
    }
    return null
}

/** The most recent WORK step before [idx] (the set just finished, to log reps during rest). */
private fun prevWorkStep(steps: List<Step>, idx: Int): Step? {
    var i = idx - 1
    while (i >= 0) {
        if (steps[i].kind == StepKind.WORK) return steps[i]
        i--
    }
    return null
}

/** Label + stepper for logging reps of a set. */
@Composable
private fun RepsInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = WT.Muted)
        Stepper(value = value, onValueChange = onValueChange, min = 0, max = 999, width = 120)
    }
}

/** One column of the rest layout: the previous or upcoming set, with its set number and reps. */
@Composable
private fun RestSetColumn(
    role: String,
    step: Step?,
    reps: Int,
    onRepsChange: (Int) -> Unit,
    isNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .scale(if (isNext) 1f else 0.92f)
            .clip(shape)
            .background(WT.Surface)
            .background(if (isNext) WT.Accent.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (isNext) WT.Accent.copy(alpha = 0.6f) else WT.Line, shape)
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .alpha(if (step != null) 1f else 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(role, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = WT.Faint)
        Text(
            step?.exerciseName ?: "—",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WT.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (step != null) {
            SetDots(step.totalSets, step.setNumber, color = if (isNext) WT.Accent else WT.Muted, blinkCurrent = isNext)
        }
        Text(
            if (step != null) stringResource(R.string.active_set_progress, step.setNumber, step.totalSets)
            else stringResource(R.string.active_set_progress_empty),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WT.Muted,
            fontFamily = WtFonts.Mono,
        )
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.active_reps), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = WT.Muted)
        if (step != null) {
            Stepper(value = reps, onValueChange = onRepsChange, min = 0, max = 999, width = 116)
        }
    }
}

// ── Set dots ────────────────────────────────────────────────
@Composable
private fun SetDots(total: Int, current: Int, color: Color = WT.Accent, blinkCurrent: Boolean = false) {
    val blinkAlpha = if (blinkCurrent) {
        rememberInfiniteTransition(label = "setDotBlink").animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
            label = "setDotBlinkAlpha",
        ).value
    } else 1f
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        (0 until total).forEach { i ->
            val done = i < current - 1
            val isCur = i == current - 1
            Box(
                Modifier
                    .width(if (isCur) 22.dp else 9.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .alpha(if (isCur) blinkAlpha else 1f)
                    .background(
                        when {
                            isCur -> color
                            done -> color.copy(alpha = 0.5f)
                            else -> WT.Surface2
                        }
                    ),
            )
        }
    }
}

// ── Context strip (prev → now/rest → next) ──────────────────
@Composable
private fun ContextStrip(steps: List<Step>, idx: Int) {
    val cur = steps[idx]
    fun findWork(dir: Int): Step? {
        var i = idx + dir
        while (i in steps.indices) {
            if (steps[i].kind == StepKind.WORK) return steps[i]
            i += dir
        }
        return null
    }
    var supersetName: String
    var prev = findWork(-1)
    val next = findWork(1)
    val now = if (cur.kind == StepKind.WORK) cur else null
    if (cur.kind == StepKind.WORK) {
        supersetName = cur.supersetName
        if (prev != null && prev.supersetIndex != cur.supersetIndex) prev = null
    } else {
        supersetName = when {
            cur.restType == RestType.SUPERSET -> cur.fromSuperset
            cur.supersetNameRest.isNotEmpty() -> cur.supersetNameRest
            else -> prev?.supersetName ?: ""
        }
    }
    val multi = now != null && now.exCount > 1
    val showSuper = multi && !supersetName.contains("super", ignoreCase = true)
    val midColor = if (cur.kind == StepKind.COUNTDOWN) WT.Warn else WT.Rest
    val midLabel = stringResource(
        if (cur.kind == StepKind.COUNTDOWN) R.string.active_label_get_ready
        else if (cur.restType == RestType.SUPERSET) R.string.active_label_block_rest else R.string.active_label_rest
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        val stripBase = supersetName.ifEmpty { stringResource(R.string.active_up_next) }
        Text(
            (if (showSuper) stringResource(R.string.active_superset_suffix, stripBase) else stripBase).uppercase(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = WT.Accent,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ContextPart(prev, active = false, role = stringResource(R.string.active_role_done), modifier = Modifier.weight(1f))
            WtIcon("chevR", size = 14.dp, color = WT.Faint)
            if (cur.kind == StepKind.WORK) {
                ContextPart(now, active = true, role = stringResource(R.string.active_role_now), modifier = Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Dot(midColor, sizeDp = 6)
                        Text(midLabel, color = midColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            WtIcon("chevR", size = 14.dp, color = WT.Faint)
            ContextPart(next, active = false, role = stringResource(R.string.active_role_next), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ContextPart(ex: Step?, active: Boolean, role: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(horizontal = 2.dp)
            .alpha(if (ex != null) 1f else 0.3f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            role,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = WT.Faint,
        )
        Text(
            ex?.exerciseName ?: "—",
            fontSize = if (active) 14.sp else 12.5.sp,
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (active) WT.Text else WT.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
