package io.shizen.workouttimer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.shizen.workouttimer.AppViewModel
import io.shizen.workouttimer.NavCmd
import io.shizen.workouttimer.Routes
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
    val navController = rememberNavController()
    val tab by vm.tab.collectAsState()
    val workouts by vm.workouts.collectAsState()
    val history by vm.history.collectAsState()
    val settings by vm.settings.collectAsState()
    val activeState by vm.activeState.collectAsState()
    val resumePrompt by vm.resumePrompt.collectAsState()
    val editing by vm.editing.collectAsState()
    val result by vm.result.collectAsState()
    val detail by vm.detail.collectAsState()

    // Execute the ViewModel's one-shot navigation commands on the NavController.
    LaunchedEffect(navController) {
        vm.nav.collect { cmd ->
            when (cmd) {
                is NavCmd.To -> navController.navigate(cmd.route) {
                    launchSingleTop = true
                    cmd.popUpToRoute?.let { popUpTo(it) { inclusive = cmd.inclusive } }
                }
                NavCmd.Back -> navController.popBackStack()
                is NavCmd.PopTo -> navController.popBackStack(cmd.route, inclusive = false)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WT.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.TABS,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.TABS) {
                // On the History tab, system back returns to the Workouts tab first.
                BackHandler(enabled = tab == Tab.HISTORY) { vm.setTab(Tab.HOME) }
                Column(Modifier.fillMaxSize()) {
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

            composable(Routes.EDITOR) {
                val target = editing
                if (target != null) {
                    EditorScreen(
                        initial = target.workout,
                        isNew = target.isNew,
                        onSave = vm::saveWorkout,
                        onCancel = vm::cancelEditor,
                    )
                }
            }

            composable(Routes.ACTIVE) {
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
                        onSetRepsForWork = controller::setRepsForWorkIndex,
                        onFinishNow = controller::finishNow,
                        onToggleVibrate = vm::toggleVibrate,
                        onToggleSound = vm::toggleSound,
                    )
                }
            }

            composable(Routes.SUMMARY) {
                val r = result
                if (r != null) {
                    SummaryScreen(
                        result = r,
                        onSave = vm::saveResult,
                        onDiscard = vm::discardResult,
                    )
                }
            }

            composable(Routes.DETAIL) {
                val entry = detail
                if (entry != null) {
                    HistoryDetailScreen(
                        entry = entry,
                        onBack = { navController.popBackStack() },
                        onEdit = vm::editHistory,
                        onDelete = { id ->
                            vm.deleteHistory(id)
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(Routes.EDIT_HISTORY) {
                val entry = detail
                if (entry != null) {
                    SummaryScreen(
                        result = entry,
                        onSave = vm::updateHistory,
                        onDiscard = { navController.popBackStack() },
                        isEdit = true,
                    )
                }
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
