package io.shizen.workouttimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.AppViewModel
import io.shizen.workouttimer.Screen
import io.shizen.workouttimer.Tab
import io.shizen.workouttimer.ui.components.ConfirmDialog
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.components.pressScale
import io.shizen.workouttimer.ui.screens.ActiveScreen
import io.shizen.workouttimer.ui.screens.EditorScreen
import io.shizen.workouttimer.ui.screens.HistoryDetailScreen
import io.shizen.workouttimer.ui.screens.HistoryScreen
import io.shizen.workouttimer.ui.screens.HomeScreen
import io.shizen.workouttimer.ui.screens.SummaryScreen
import io.shizen.workouttimer.ui.theme.WT

@Composable
fun AppRoot(vm: AppViewModel) {
    val screen by vm.screen.collectAsState()
    val tab by vm.tab.collectAsState()
    val workouts by vm.workouts.collectAsState()
    val history by vm.history.collectAsState()
    val settings by vm.settings.collectAsState()
    val activeState by vm.activeState.collectAsState()
    val resumePrompt by vm.resumePrompt.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(WT.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (val s = screen) {
            is Screen.Active -> {
                val st = activeState
                val controller = vm.controller
                if (st != null && controller != null) {
                    ActiveScreen(
                        state = st,
                        settings = settings,
                        onStart = controller::start,
                        onTogglePause = controller::togglePause,
                        onAdvance = controller::advance,
                        onSetReps = controller::setReps,
                        onFinishNow = controller::finishNow,
                        onToggleVibrate = vm::toggleVibrate,
                        onToggleSound = vm::toggleSound,
                    )
                }
            }

            is Screen.Editor -> EditorScreen(
                initial = s.workout,
                isNew = s.isNew,
                onSave = vm::saveWorkout,
                onCancel = vm::cancelEditor,
            )

            is Screen.Summary -> SummaryScreen(
                result = s.result,
                onSave = vm::saveResult,
                onDiscard = vm::discardResult,
            )

            is Screen.Detail -> HistoryDetailScreen(
                entry = s.entry,
                onBack = vm::backToHistory,
                onDelete = { vm.deleteHistory(it); vm.backToHistory() },
            )

            is Screen.Tabs -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            workouts = workouts,
                            onStart = vm::startWorkout,
                            onEdit = vm::editWorkout,
                            onDuplicate = vm::duplicateWorkout,
                            onDelete = vm::deleteWorkout,
                            onNew = vm::newWorkout,
                        )
                        Tab.HISTORY -> HistoryScreen(
                            history = history,
                            onOpen = vm::openDetail,
                            onClear = vm::clearHistory,
                            onDelete = vm::deleteHistory,
                        )
                    }
                }
                BottomNav(tab = tab, onTab = vm::setTab)
            }
        }

        if (resumePrompt) {
            ConfirmDialog(
                title = "Resume workout?",
                body = vm.resumeWorkoutName?.let { "You have \"$it\" in progress." }
                    ?: "You have a workout in progress.",
                confirmLabel = "Resume",
                danger = false,
                onConfirm = vm::resumeSession,
                onDismiss = vm::dismissResume,
            )
        }
    }
}

@Composable
private fun BottomNav(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.HOME to ("dumbbell" to "Workouts"), Tab.HISTORY to ("history" to "History"))
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(WT.Line))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WT.Surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            items.forEach { (key, meta) ->
                val (icon, label) = meta
                val on = tab == key
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressScale(pressed = 0.96f) { onTab(key) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    WtIcon(icon, size = 22.dp, color = if (on) WT.Accent else WT.Faint, strokeWidth = if (on) 2.4f else 2f)
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        color = if (on) WT.Accent else WT.Faint,
                    )
                }
            }
        }
    }
}
