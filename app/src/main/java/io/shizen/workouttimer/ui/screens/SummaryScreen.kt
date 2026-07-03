package io.shizen.workouttimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.R
import io.shizen.workouttimer.data.BREATHLESS
import io.shizen.workouttimer.data.Rating
import io.shizen.workouttimer.data.SATISFACTION
import io.shizen.workouttimer.data.SetResult
import io.shizen.workouttimer.data.WorkoutResult
import io.shizen.workouttimer.data.fmtDate
import io.shizen.workouttimer.data.fmtLong
import io.shizen.workouttimer.ui.components.Btn
import io.shizen.workouttimer.ui.components.BtnVariant
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.components.pressScale
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

private class RepGroup(val exerciseName: String, val supersetName: String, val sets: MutableList<SetResult> = mutableListOf())

@Composable
fun SummaryScreen(
    result: WorkoutResult,
    onSave: (WorkoutResult) -> Unit,
    onDiscard: () -> Unit,
    isEdit: Boolean = false,
) {
    // Discarding a finished workout loses it forever, so ask first; cancelling an
    // edit is harmless and goes straight through.
    var confirmDiscard by remember { mutableStateOf(false) }
    fun requestDiscard() {
        if (isEdit) onDiscard() else confirmDiscard = true
    }
    // System back behaves like the visible Discard / Cancel action.
    // While the dialog is open, let back dismiss it.
    BackHandler(enabled = !confirmDiscard) { requestDiscard() }
    val reps = remember {
        mutableStateMapOf<Int, Int>().apply {
            result.sets.forEach { if (it.reps != null) put(it.workIndex, it.reps) }
        }
    }
    var sat by remember { mutableStateOf(result.satisfaction) }
    var brt by remember { mutableStateOf(result.breathless) }

    val groups = remember(result) {
        val map = LinkedHashMap<String, RepGroup>()
        result.sets.forEach { s ->
            val key = "${s.supersetIndex}|${s.exerciseName}"
            map.getOrPut(key) { RepGroup(s.exerciseName, s.supersetName) }.sets.add(s)
        }
        map.values.toList()
    }

    fun save() {
        val out = result.copy(
            satisfaction = sat,
            breathless = brt,
            sets = result.sets.map { it.copy(reps = reps[it.workIndex]) },
        )
        onSave(out)
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(WT.Accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                WtIcon("check", size = 30.dp, color = WT.Accent, strokeWidth = 2.6f)
            }
            Text(stringResource(if (isEdit) R.string.common_edit_session else R.string.summary_title), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text, modifier = Modifier.padding(top = 10.dp))
            Text(result.workoutName, fontSize = 13.sp, color = WT.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SumStat(stringResource(R.string.summary_stat_time), fmtLong(result.totalElapsed))
                SumStat(stringResource(R.string.summary_stat_date), fmtDate(result.endedAt))
                SumStat(stringResource(R.string.summary_stat_sets), "${result.sets.size}")
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SectionLabel(stringResource(R.string.summary_how_feel)) }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WT.Surface)
                        .border(1.dp, WT.Line, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Text(stringResource(R.string.common_satisfaction), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WT.Muted, modifier = Modifier.padding(bottom = 8.dp))
                        RatingPicker(SATISFACTION, sat, { sat = it }, WT.Accent)
                    }
                    Column {
                        Text(stringResource(R.string.common_breathlessness), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = WT.Muted, modifier = Modifier.padding(bottom = 8.dp))
                        RatingPicker(BREATHLESS, brt, { brt = it }, WT.Rest)
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.summary_log_reps), sub = stringResource(R.string.summary_log_reps_sub)) }
            groups.forEach { g ->
                item { RepGroupCard(g, reps) }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(WT.Line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WT.Surface)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Btn(stringResource(if (isEdit) R.string.common_cancel else R.string.summary_discard), onClick = { requestDiscard() }, variant = BtnVariant.Ghost)
            Btn(stringResource(if (isEdit) R.string.summary_save_changes else R.string.summary_save_to_history), onClick = { save() }, icon = "check", fillMaxWidth = true, modifier = Modifier.weight(1f))
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.summary_discard_dialog_title),
            body = stringResource(R.string.summary_discard_dialog_body),
            confirmLabel = stringResource(R.string.summary_discard),
            danger = true,
            onConfirm = { confirmDiscard = false; onDiscard() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SumStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = WtFonts.Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WT.Text)
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = WT.Faint, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun RatingPicker(options: List<Rating>, value: Int?, onChange: (Int) -> Unit, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o ->
            val on = value == o.key
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) color.copy(alpha = 0.16f) else WT.Surface2)
                    .border(1.5.dp, if (on) color else WT.Line, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = color),
                    ) { onChange(o.key) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 4).forEach { j ->
                        Box(
                            Modifier
                                .width(5.dp)
                                .height((5 + j * 4).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (j <= i) (if (on) color else WT.Muted) else WT.Line),
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(stringResource(o.labelRes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) color else WT.Muted)
            }
        }
    }
}

@Composable
private fun RepGroupCard(group: RepGroup, reps: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Int>) {
    var all by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(group.exerciseName, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text)
                Text(
                    stringResource(
                        R.string.summary_group_sub,
                        group.supersetName,
                        pluralStringResource(R.plurals.sets_count, group.sets.size, group.sets.size),
                    ),
                    fontSize = 11.5.sp, color = WT.Faint, fontWeight = FontWeight.SemiBold,
                )
            }
            // Set-all control
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WT.Surface2)
                    .border(1.dp, WT.Line, RoundedCornerShape(10.dp))
                    .pressScale(pressed = 0.97f) {
                        val v = all.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        group.sets.forEach { reps[it.workIndex] = v }
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NumberField(
                    value = all,
                    onValueChange = { all = it.filter(Char::isDigit).take(3) },
                    placeholder = "—",
                    width = 34.dp,
                    fontSize = 15.sp,
                )
                Text(stringResource(R.string.summary_set_all), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WT.Accent)
            }
        }

        // 3-column grid built from rows so it composes safely inside a LazyColumn
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.sets.chunked(3).forEach { rowSets ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSets.forEach { s ->
                        RepCell(s, reps, Modifier.weight(1f))
                    }
                    repeat(3 - rowSets.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun RepCell(
    s: SetResult,
    reps: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(WT.Bg)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(stringResource(R.string.summary_set_n, s.setNumber), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = WT.Faint, fontFamily = WtFonts.Mono)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniBtn("−") {
                val cur = reps[s.workIndex] ?: 0
                reps[s.workIndex] = (cur - 1).coerceAtLeast(0)
            }
            NumberField(
                value = reps[s.workIndex]?.toString() ?: "",
                onValueChange = { txt ->
                    val digits = txt.filter(Char::isDigit).take(3)
                    if (digits.isEmpty()) reps.remove(s.workIndex)
                    else reps[s.workIndex] = digits.toInt()
                },
                placeholder = "—",
                width = 40.dp,
                fontSize = 18.sp,
            )
            MiniBtn("+") {
                val cur = reps[s.workIndex] ?: 0
                reps[s.workIndex] = cur + 1
            }
        }
    }
}

@Composable
private fun MiniBtn(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(20.dp, 28.dp).pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 18.sp, color = WT.Muted)
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    width: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = WT.Text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = WtFonts.Mono,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(WT.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(placeholder, color = WT.Faint, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = WtFonts.Mono, textAlign = TextAlign.Center)
        }
    }
}
