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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.data.Workout
import io.shizen.workouttimer.data.estimateDuration
import io.shizen.workouttimer.data.fmtLong
import io.shizen.workouttimer.data.totalWorkSteps
import io.shizen.workouttimer.ui.components.Btn
import io.shizen.workouttimer.ui.components.BtnSize
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.components.MenuRow
import io.shizen.workouttimer.ui.components.WtBottomSheet
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.components.pressScale
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

@Composable
fun HomeScreen(
    workouts: List<Workout>,
    onStart: (Workout) -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
) {
    var menu by remember { mutableStateOf<Workout?>(null) }
    var confirmDel by remember { mutableStateOf<Workout?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow = "Workouts", title = "Your library")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(workouts, key = { it.id }) { w ->
                WorkoutCard(w, onStart = onStart, onMenu = { menu = it })
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, WT.Line, RoundedCornerShape(16.dp))
                        .pressScale(pressed = 0.98f, onClick = onNew),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WtIcon("add", size = 20.dp, color = WT.Muted)
                        Text("New workout", color = WT.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val current = menu
    if (current != null) {
        WtBottomSheet(onDismiss = { menu = null }, title = current.name) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MenuRow("edit", "Edit workout", onClick = { onEdit(current.id); menu = null })
                MenuRow("copy", "Duplicate", onClick = { onDuplicate(current.id); menu = null })
                if (workouts.size > 1) {
                    MenuRow("trash", "Delete", danger = true, onClick = { confirmDel = current; menu = null })
                }
            }
        }
    }

    val del = confirmDel
    if (del != null) {
        ConfirmDialog(
            title = "Delete workout?",
            body = "\"${del.name}\" will be removed. History is kept.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = { onDelete(del.id); confirmDel = null },
            onDismiss = { confirmDel = null },
        )
    }
}

@Composable
private fun WorkoutCard(
    w: Workout,
    onStart: (Workout) -> Unit,
    onMenu: (Workout) -> Unit,
) {
    val dur = estimateDuration(w)
    val sets = totalWorkSteps(w)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    w.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    color = WT.Text,
                )
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StatChip("clock", fmtLong(dur))
                    StatChip("layers", "${w.supersets.size} blocks")
                    StatChip("dumbbell", "$sets sets")
                }
            }
            IconBtn(
                "drag", onClick = { onMenu(w) },
                size = 36, iconSize = 20, bg = androidx.compose.ui.graphics.Color.Transparent,
                color = WT.Faint,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            w.supersets.forEachIndexed { i, ss ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WT.Bg)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        (i + 1).toString().padStart(2, '0'),
                        fontFamily = WtFonts.Mono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WT.Accent,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        ss.exercises.joinToString("  +  ") { it.name },
                        modifier = Modifier.weight(1f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WT.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "×${ss.sets}",
                        fontFamily = WtFonts.Mono,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WT.Muted,
                    )
                }
            }
        }

        Btn(
            "Start workout",
            onClick = { onStart(w) },
            icon = "play",
            size = BtnSize.Md,
            fillMaxWidth = true,
            modifier = Modifier.height(50.dp),
        )
    }
}
