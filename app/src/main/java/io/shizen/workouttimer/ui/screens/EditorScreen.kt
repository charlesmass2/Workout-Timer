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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.data.Exercise
import io.shizen.workouttimer.data.Superset
import io.shizen.workouttimer.data.Workout
import io.shizen.workouttimer.data.estimateDuration
import io.shizen.workouttimer.data.fmtLong
import io.shizen.workouttimer.data.uid
import io.shizen.workouttimer.ui.components.Btn
import io.shizen.workouttimer.ui.components.BtnSize
import io.shizen.workouttimer.ui.components.IconBtn
import io.shizen.workouttimer.ui.components.Pill
import io.shizen.workouttimer.ui.components.PillTone
import io.shizen.workouttimer.ui.components.Stepper
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.components.pressScale
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

@Composable
fun EditorScreen(
    initial: Workout,
    isNew: Boolean,
    onSave: (Workout) -> Unit,
    onCancel: () -> Unit,
) {
    var w by remember { mutableStateOf(initial) }

    fun updSS(i: Int, ss: Superset) {
        w = w.copy(supersets = w.supersets.toMutableList().apply { this[i] = ss })
    }
    fun addSS() {
        w = w.copy(
            supersets = w.supersets + Superset(
                id = uid(), name = "Block ${w.supersets.size + 1}", sets = 3,
                restBetweenSets = 60, restAfter = 120,
                exercises = listOf(Exercise(uid(), "", 30)),
            )
        )
    }
    fun rmSS(i: Int) {
        w = w.copy(supersets = w.supersets.filterIndexed { j, _ -> j != i })
    }

    val valid = w.name.isNotBlank() && w.supersets.isNotEmpty() &&
        w.supersets.all { it.exercises.isNotEmpty() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconBtn("back", onClick = onCancel, size = 40, iconSize = 22, bg = Color.Transparent)
            Text(
                if (isNew) "New workout" else "Edit workout",
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WT.Text,
            )
            Btn("Save", onClick = { onSave(w) }, size = BtnSize.Sm, enabled = valid)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp, end = 14.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WT.Surface)
                        .border(1.dp, WT.Line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Workout name", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Text)
                        EditField(
                            value = w.name,
                            onValueChange = { w = w.copy(name = it) },
                            placeholder = "Name",
                            modifier = Modifier.width(180.dp),
                            textAlign = TextAlign.End,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "BLOCKS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = WT.Faint,
                    )
                    Pill(fmtLong(estimateDuration(w)), tone = PillTone.Default, leading = "clock")
                }
            }

            w.supersets.forEachIndexed { i, ss ->
                item(key = ss.id) {
                    SupersetEditor(
                        ss = ss,
                        index = i,
                        isLast = i == w.supersets.lastIndex,
                        canRemove = w.supersets.size > 1,
                        onChange = { updSS(i, it) },
                        onRemove = { rmSS(i) },
                    )
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, WT.Line, RoundedCornerShape(16.dp))
                        .pressScale(pressed = 0.98f) { addSS() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WtIcon("add", size = 20.dp, color = WT.Muted)
                        Text("Add block", color = WT.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupersetEditor(
    ss: Superset,
    index: Int,
    isLast: Boolean,
    canRemove: Boolean,
    onChange: (Superset) -> Unit,
    onRemove: () -> Unit,
) {
    val multi = ss.exercises.size > 1
    fun updEx(i: Int, ex: Exercise) =
        onChange(ss.copy(exercises = ss.exercises.toMutableList().apply { this[i] = ex }))
    fun addEx() = onChange(ss.copy(exercises = ss.exercises + Exercise(uid(), "", 30)))
    fun rmEx(i: Int) = onChange(ss.copy(exercises = ss.exercises.filterIndexed { j, _ -> j != i }))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                (index + 1).toString().padStart(2, '0'),
                fontFamily = WtFonts.Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WT.Accent,
            )
            EditField(
                value = ss.name,
                onValueChange = { onChange(ss.copy(name = it)) },
                placeholder = "Block name",
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (multi) Pill("SUPERSET", tone = PillTone.Accent)
            if (canRemove) {
                IconBtn("trash", onClick = onRemove, size = 34, iconSize = 17, bg = Color.Transparent, color = WT.Faint)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ss.exercises.forEachIndexed { i, ex ->
                ExerciseRow(
                    ex = ex,
                    canRemove = multi,
                    placeholder = "Exercise ${index + 1}-${i + 1}",
                    onChange = { updEx(i, it) },
                    onRemove = { rmEx(i) },
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, WT.Line, RoundedCornerShape(10.dp))
                .pressScale(pressed = 0.98f) { addEx() },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WtIcon("plus", size = 16.dp, color = WT.Muted)
                Text(
                    if (multi) "Add exercise to block" else "Add exercise to block (make superset)",
                    color = WT.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(WT.Line))

        EditorField("Sets", hint = if (multi) "rounds, alternating each exercise" else "number of rounds") {
            Stepper(ss.sets, onValueChange = { onChange(ss.copy(sets = it)) }, min = 1, max = 50, width = 104)
        }
        EditorField("Rest between sets") {
            Stepper(ss.restBetweenSets, onValueChange = { onChange(ss.copy(restBetweenSets = it)) }, step = 5, min = 0, max = 600, suffix = "s", width = 104)
        }
        if (!isLast) {
            EditorField("Rest after block", hint = "before the next block") {
                Stepper(ss.restAfter, onValueChange = { onChange(ss.copy(restAfter = it)) }, step = 5, min = 0, max = 600, suffix = "s", width = 104)
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    ex: Exercise,
    canRemove: Boolean,
    placeholder: String,
    onChange: (Exercise) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WT.Bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WtIcon("dumbbell", size = 16.dp, color = WT.Faint)
        EditField(
            value = ex.name,
            onValueChange = { onChange(ex.copy(name = it)) },
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Stepper(ex.duration, onValueChange = { onChange(ex.copy(duration = it)) }, step = 5, min = 5, max = 900, suffix = "s", width = 104)
        if (canRemove) {
            IconBtn("x", onClick = onRemove, size = 32, iconSize = 16, bg = Color.Transparent, color = WT.Faint)
        }
    }
}

@Composable
private fun EditorField(label: String, hint: String? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = WT.Text)
            if (hint != null) {
                Text(hint, fontSize = 11.5.sp, color = WT.Faint, modifier = Modifier.padding(top = 1.dp))
            }
        }
        trailing()
    }
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.5.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Box(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = WT.Text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = WtFonts.Sans,
                textAlign = textAlign,
            ),
            cursorBrush = SolidColor(WT.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = WT.Faint,
                fontSize = fontSize,
                fontWeight = fontWeight,
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
            )
        }
    }
}
