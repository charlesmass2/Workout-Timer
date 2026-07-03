package io.shizen.workouttimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.R
import io.shizen.workouttimer.data.SATISFACTION
import io.shizen.workouttimer.data.WorkoutResult
import io.shizen.workouttimer.data.fmtDayKey
import io.shizen.workouttimer.data.fmtLong
import io.shizen.workouttimer.data.fmtTime
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.components.Pill
import io.shizen.workouttimer.ui.components.PillTone
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.components.pressScale
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

@Composable
fun HistoryScreen(
    history: List<WorkoutResult>,
    onOpen: (WorkoutResult) -> Unit,
    onClear: () -> Unit,
    onDelete: (String) -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var delEntry by remember { mutableStateOf<WorkoutResult?>(null) }

    // group by day, newest first
    val sorted = history.sortedByDescending { it.endedAt }
    val groups = LinkedHashMap<String, MutableList<WorkoutResult>>()
    sorted.forEach { groups.getOrPut(fmtDayKey(it.endedAt)) { mutableListOf() }.add(it) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow = stringResource(R.string.history_eyebrow), title = stringResource(R.string.history_title)) {
            if (history.isNotEmpty()) {
                IconBtn(
                    "trash", onClick = { confirmClear = true },
                    contentDescription = stringResource(R.string.history_clear_all_cd),
                    size = 38, iconSize = 19, bg = Color.Transparent, color = WT.Faint,
                )
            }
        }

        if (history.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WtIcon("history", size = 40.dp, color = WT.Surface2)
                Text(
                    stringResource(R.string.history_empty_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WT.Muted,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    stringResource(R.string.history_empty_body),
                    fontSize = 13.sp,
                    color = WT.Faint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 24.dp
                ),
            ) {
                groups.forEach { (day, entries) ->
                    item(key = day) {
                        Text(
                            day.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = WT.Faint,
                            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp, top = 6.dp),
                        )
                    }
                    items(entries, key = { it.id }) { h ->
                        Column(Modifier.padding(bottom = 10.dp)) {
                            HistoryRow(h, onOpen = onOpen, onDelete = { delEntry = h })
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_dialog_title),
            body = stringResource(R.string.history_clear_dialog_body),
            confirmLabel = stringResource(R.string.history_clear_dialog_confirm),
            danger = true,
            onConfirm = { onClear(); confirmClear = false },
            onDismiss = { confirmClear = false },
        )
    }
    val del = delEntry
    if (del != null) {
        ConfirmDialog(
            title = stringResource(R.string.history_delete_dialog_title),
            body = stringResource(R.string.history_delete_dialog_body, del.workoutName, fmtDayKey(del.endedAt)),
            confirmLabel = stringResource(R.string.common_delete),
            danger = true,
            onConfirm = { onDelete(del.id); delEntry = null },
            onDismiss = { delEntry = null },
        )
    }
}

@Composable
private fun HistoryRow(
    h: WorkoutResult,
    onOpen: (WorkoutResult) -> Unit,
    onDelete: () -> Unit,
) {
    val sat = SATISFACTION.find { it.key == h.satisfaction }
    val totalReps = h.sets.sumOf { it.reps ?: 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(16.dp))
            .pressScale(pressed = 0.99f) { onOpen(h) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                h.workoutName,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WT.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatChip("clock", fmtLong(h.totalElapsed))
                StatChip("dumbbell", pluralStringResource(R.plurals.sets_count, h.sets.size, h.sets.size))
                if (totalReps > 0) StatChip("flame", pluralStringResource(R.plurals.reps_count, totalReps, totalReps))
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                fmtTime(h.endedAt),
                fontFamily = WtFonts.Mono,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = WT.Muted,
            )
            if (sat != null) Pill(stringResource(sat.labelRes), tone = PillTone.Accent)
        }
        IconBtn(
            "trash", onClick = onDelete,
            contentDescription = stringResource(R.string.common_delete_session),
            size = 36, iconSize = 17, bg = Color.Transparent, color = WT.Faint,
        )
    }
}
