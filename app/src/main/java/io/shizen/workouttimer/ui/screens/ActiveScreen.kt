package io.shizen.workouttimer.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onFinishNow: () -> Unit,
    onToggleVibrate: () -> Unit,
    onToggleSound: () -> Unit,
) {
    KeepScreenOn()
    var confirmEnd by remember { mutableStateOf(false) }

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
    val label = when (phase) {
        StepKind.COUNTDOWN -> "GET READY"
        StepKind.REST -> if (cur.restType == RestType.SUPERSET) "BLOCK REST" else "REST"
        StepKind.WORK -> "WORK"
    }
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
            IconBtn("chevD", onClick = { confirmEnd = true }, size = 38, iconSize = 22, bg = Color.Transparent, color = WT.Muted)
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
                    "$doneWork/$totalWork SETS",
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
                            "SET ${cur.setNumber} / ${cur.totalSets}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WT.Muted,
                            fontFamily = WtFonts.Mono,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(WT.Surface)
                                .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("REPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = WT.Muted)
                            Stepper(
                                value = state.reps[cur.workIndex] ?: 0,
                                onValueChange = onSetReps,
                                min = 0, max = 999, width = 120,
                            )
                        }
                    }
                    StepKind.REST -> {
                        val next = if (cur.restType == RestType.SUPERSET) cur.nextSuperset else cur.nextExercise
                        Text("Catch your breath", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Muted, textAlign = TextAlign.Center)
                        Text("Next up: $next", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text, textAlign = TextAlign.Center)
                    }
                    StepKind.COUNTDOWN -> {
                        Text("Starting in…", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Muted)
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
                Btn("Start", onClick = onStart, icon = "play", size = BtnSize.Lg, fillMaxWidth = true)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn(
                        if (state.running) "Pause" else "Resume",
                        onClick = onTogglePause,
                        icon = if (state.running) "pause" else "play",
                        size = BtnSize.Lg,
                        variant = if (state.running) BtnVariant.Ghost else BtnVariant.Primary,
                        modifier = Modifier.width(130.dp),
                    )
                    Btn(
                        when (phase) {
                            StepKind.COUNTDOWN -> "Skip"
                            StepKind.REST -> "Skip rest"
                            StepKind.WORK -> "Finish set"
                        },
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
                    active = settings.vibrate,
                    iconSize = 20,
                )
                IconBtn(
                    if (settings.sound) "speaker" else "speakerOff",
                    onClick = onToggleSound,
                    active = settings.sound,
                    iconSize = 20,
                )
                Spacer(Modifier.weight(1f))
                Btn("Finish", onClick = { confirmEnd = true }, variant = BtnVariant.Danger, size = BtnSize.Sm, icon = "check")
            }
        }
    }

    if (confirmEnd) {
        ConfirmDialog(
            title = "Finish workout?",
            body = "You'll go to the summary where you can log reps and how it felt.",
            confirmLabel = "Finish & review",
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

// ── Set dots ────────────────────────────────────────────────
@Composable
private fun SetDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        (0 until total).forEach { i ->
            val done = i < current - 1
            val isCur = i == current - 1
            Box(
                Modifier
                    .width(if (isCur) 22.dp else 9.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            isCur -> WT.Accent
                            done -> WT.Accent.copy(alpha = 0.5f)
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
    val midLabel = if (cur.kind == StepKind.COUNTDOWN) "GET READY"
    else if (cur.restType == RestType.SUPERSET) "BLOCK REST" else "REST"

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            ((supersetName.ifEmpty { "Up next" }) + (if (showSuper) " · superset" else "")).uppercase(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = WT.Accent,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ContextPart(prev, active = false, role = "DONE", modifier = Modifier.weight(1f))
            WtIcon("chevR", size = 14.dp, color = WT.Faint)
            if (cur.kind == StepKind.WORK) {
                ContextPart(now, active = true, role = "NOW", modifier = Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Dot(midColor, sizeDp = 6)
                        Text(midLabel, color = midColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            WtIcon("chevR", size = 14.dp, color = WT.Faint)
            ContextPart(next, active = false, role = "NEXT", modifier = Modifier.weight(1f))
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
