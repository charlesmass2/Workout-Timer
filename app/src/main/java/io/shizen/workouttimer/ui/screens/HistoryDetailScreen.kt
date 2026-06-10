package io.shizen.workouttimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.data.BREATHLESS
import io.shizen.workouttimer.data.SATISFACTION
import io.shizen.workouttimer.data.SetResult
import io.shizen.workouttimer.data.WorkoutResult
import io.shizen.workouttimer.data.fmtClock
import io.shizen.workouttimer.data.fmtDate
import io.shizen.workouttimer.data.fmtLong
import io.shizen.workouttimer.data.fmtTime
import io.shizen.workouttimer.data.signedLong
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

private class Group(val name: String, val block: String, val sets: MutableList<SetResult> = mutableListOf())

@Composable
fun HistoryDetailScreen(
    entry: WorkoutResult,
    onBack: () -> Unit,
    onEdit: (WorkoutResult) -> Unit,
    onDelete: (String) -> Unit,
) {
    var del by remember { mutableStateOf(false) }
    val sat = SATISFACTION.find { it.key == entry.satisfaction }
    val brt = BREATHLESS.find { it.key == entry.breathless }

    val groups = LinkedHashMap<String, Group>()
    entry.sets.forEach { s ->
        val key = "${s.supersetIndex}|${s.exerciseName}"
        groups.getOrPut(key) { Group(s.exerciseName, s.supersetName) }.sets.add(s)
    }
    val delta = entry.totalElapsed - entry.estimated
    val totalReps = entry.sets.sumOf { it.reps ?: 0 }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconBtn("back", onClick = onBack, size = 40, iconSize = 22, bg = Color.Transparent)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.workoutName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WT.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${fmtDate(entry.endedAt)} · ${fmtTime(entry.startedAt)}–${fmtTime(entry.endedAt)}",
                    fontSize = 12.sp,
                    color = WT.Muted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconBtn("edit", onClick = { onEdit(entry) }, size = 40, iconSize = 19, bg = Color.Transparent, color = WT.Muted)
            IconBtn("trash", onClick = { del = true }, size = 40, iconSize = 19, bg = Color.Transparent, color = WT.Faint)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Total time", fmtLong(entry.totalElapsed), signedLong(delta), Modifier.weight(1f))
                        Metric("Planned", fmtLong(entry.estimated), null, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric("Total reps", if (totalReps > 0) "$totalReps" else "—", null, Modifier.weight(1f))
                        Metric("Sets done", "${entry.sets.size}", null, Modifier.weight(1f))
                    }
                }
            }

            if (sat != null || brt != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (sat != null) FeelCard("Satisfaction", sat.label, sat.key, WT.Accent, Modifier.weight(1f))
                        if (brt != null) FeelCard("Breathlessness", brt.label, brt.key, WT.Rest, Modifier.weight(1f))
                    }
                }
            }

            item { SectionLabel("Exercise breakdown") }

            groups.values.forEach { g ->
                item { ExerciseBreakdown(g) }
            }
        }
    }

    if (del) {
        ConfirmDialog(
            title = "Delete this session?",
            body = "This session will be permanently removed from your history.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = { onDelete(entry.id) },
            onDismiss = { del = false },
        )
    }
}

@Composable
private fun Metric(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = WT.Faint)
        Text(
            value,
            fontFamily = WtFonts.Mono,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WT.Text,
            modifier = Modifier.padding(top = 3.dp),
        )
        if (sub != null) {
            Text(sub, fontSize = 11.sp, color = WT.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun FeelCard(label: String, value: String, rank: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = WT.Faint)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (0 until 4).forEach { j ->
                    Box(
                        Modifier
                            .width(5.dp)
                            .height((6 + j * 4).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (j < rank) color else WT.Line),
                    )
                }
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text)
        }
    }
}

@Composable
private fun ExerciseBreakdown(g: Group) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(g.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text)
            Text(g.block, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WT.Faint, fontFamily = WtFonts.Mono)
        }
        // header
        BreakdownRow("Set", "Planned", "Actual", "Reps", header = true)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
            g.sets.forEach { s ->
                val over = s.actual > s.planned
                BreakdownRow(
                    set = "${s.setNumber}",
                    planned = fmtClock(s.planned),
                    actual = fmtClock(s.actual),
                    reps = s.reps?.toString() ?: "—",
                    actualColor = if (over) WT.Warn else WT.Text,
                    repsColor = if (s.reps != null) WT.Text else WT.Faint,
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    set: String,
    planned: String,
    actual: String,
    reps: String,
    header: Boolean = false,
    actualColor: Color = WT.Text,
    repsColor: Color = WT.Text,
) {
    val rowMod = if (header) Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 0.dp)
    else Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(9.dp))
        .background(WT.Bg)
        .padding(horizontal = 2.dp, vertical = 8.dp)
    Row(rowMod, verticalAlignment = Alignment.CenterVertically) {
        val cellFont = if (header) 10.sp else 13.sp
        val labelColor = WT.Faint
        Text(
            if (header) "SET" else set,
            modifier = Modifier.width(46.dp),
            textAlign = if (header) TextAlign.Start else TextAlign.Center,
            fontFamily = if (header) WtFonts.Sans else WtFonts.Mono,
            fontSize = cellFont,
            fontWeight = FontWeight.Bold,
            color = if (header) labelColor else WT.Accent,
            letterSpacing = if (header) 0.6.sp else 0.sp,
        )
        BreakCell(if (header) "PLANNED" else planned, header, if (header) labelColor else WT.Muted, FontWeight.Normal)
        BreakCell(if (header) "ACTUAL" else actual, header, if (header) labelColor else actualColor, FontWeight.SemiBold)
        BreakCell(if (header) "REPS" else reps, header, if (header) labelColor else repsColor, FontWeight.Bold)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BreakCell(
    text: String,
    header: Boolean,
    color: Color,
    weight: FontWeight,
) {
    Text(
        text,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center,
        fontFamily = if (header) WtFonts.Sans else WtFonts.Mono,
        fontSize = if (header) 10.sp else 13.sp,
        fontWeight = if (header) FontWeight.Bold else weight,
        color = color,
        letterSpacing = if (header) 0.6.sp else 0.sp,
    )
}
