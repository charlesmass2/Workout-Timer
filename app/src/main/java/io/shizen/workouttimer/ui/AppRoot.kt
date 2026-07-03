package io.shizen.workouttimer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.shizen.workouttimer.AppViewModel
import io.shizen.workouttimer.NavCmd
import io.shizen.workouttimer.R
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
import kotlinx.coroutines.delay

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
    LaunchedEffect(navController, vm) {
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
        // Native "push" transitions: the new screen slides in from the right while
        // the old one shifts left in parallax; reversed when popping the stack.
        val slideSpec = tween<IntOffset>(300, easing = FastOutSlowInEasing)
        NavHost(
            navController = navController,
            startDestination = Routes.TABS,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(slideSpec) { it } },
            exitTransition = { slideOutHorizontally(slideSpec) { -it / 3 } },
            popEnterTransition = { slideInHorizontally(slideSpec) { -it / 3 } },
            popExitTransition = { slideOutHorizontally(slideSpec) { it } },
        ) {
            screen(Routes.TABS) {
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

            screen(Routes.EDITOR) {
                val target = editing
                if (target != null) {
                    EditorScreen(
                        initial = target.workout,
                        isNew = target.isNew,
                        onSave = vm::saveWorkout,
                        onCancel = vm::cancelEditor,
                    )
                } else {
                    RecoverTo(navController, Routes.EDITOR)
                }
            }

            screen(Routes.ACTIVE) {
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
                } else {
                    // Session lost (e.g. process death): the resume prompt offers a restart.
                    RecoverTo(navController, Routes.ACTIVE)
                }
            }

            screen(Routes.SUMMARY) {
                val r = result
                if (r != null) {
                    SummaryScreen(
                        result = r,
                        onSave = vm::saveResult,
                        onDiscard = vm::discardResult,
                    )
                } else {
                    RecoverTo(navController, Routes.SUMMARY)
                }
            }

            screen(Routes.DETAIL) {
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
                } else {
                    RecoverTo(navController, Routes.DETAIL) { vm.setTab(Tab.HISTORY) }
                }
            }

            screen(Routes.EDIT_HISTORY) {
                val entry = detail
                if (entry != null) {
                    SummaryScreen(
                        result = entry,
                        onSave = vm::updateHistory,
                        onDiscard = { navController.popBackStack() },
                        isEdit = true,
                    )
                } else {
                    RecoverTo(navController, Routes.EDIT_HISTORY) { vm.setTab(Tab.HISTORY) }
                }
            }
        }

        if (resumePrompt) {
            ConfirmDialog(
                title = stringResource(R.string.resume_dialog_title),
                body = vm.resumeWorkoutName?.let { stringResource(R.string.resume_dialog_body_named, it) }
                    ?: stringResource(R.string.resume_dialog_body),
                confirmLabel = stringResource(R.string.resume_dialog_confirm),
                danger = false,
                onConfirm = vm::resumeSession,
                onDismiss = vm::dismissResume,
            )
        }
    }
}

/**
 * A destination wrapped in an opaque background, so overlapping screens don't
 * show through each other during slide transitions.
 */
private fun NavGraphBuilder.screen(route: String, content: @Composable () -> Unit) {
    composable(route) {
        Box(Modifier.fillMaxSize().background(WT.Bg)) { content() }
    }
}

/**
 * Recover from a destination whose in-memory state was lost (e.g. process death
 * restored the back stack but not the ViewModel): return to the tabs root so the
 * user never lands on a blank screen.
 *
 * The state can also be transiently null during a legitimate transition — e.g.
 * finishing a workout clears the active session just before the SUMMARY command
 * is processed. A short settle delay lets any in-flight navigation land first,
 * and the route guard then ensures we only pop if the destination is still the
 * one whose state is missing. If recomposition brings the state back, this
 * composable leaves the tree and the effect is cancelled before it fires.
 */
@Composable
private fun RecoverTo(
    navController: NavController,
    expectedRoute: String,
    beforePop: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        delay(150)
        if (navController.currentDestination?.route == expectedRoute) {
            beforePop()
            navController.popBackStack(Routes.TABS, inclusive = false)
        }
    }
}

@Composable
private fun BottomNav(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(
        Tab.HOME to ("dumbbell" to stringResource(R.string.tab_workouts)),
        Tab.HISTORY to ("history" to stringResource(R.string.tab_history)),
    )
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
