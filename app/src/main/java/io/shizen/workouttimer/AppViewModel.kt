package io.shizen.workouttimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.shizen.workouttimer.data.Exercise
import io.shizen.workouttimer.data.Repository
import io.shizen.workouttimer.data.Session
import io.shizen.workouttimer.data.Settings
import io.shizen.workouttimer.data.Superset
import io.shizen.workouttimer.data.Workout
import io.shizen.workouttimer.data.WorkoutResult
import io.shizen.workouttimer.data.buildSchedule
import io.shizen.workouttimer.data.uid
import io.shizen.workouttimer.timer.ActiveController
import io.shizen.workouttimer.timer.ActiveState
import io.shizen.workouttimer.timer.Feedback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val COUNTDOWN_SECONDS = 5

enum class Tab { HOME, HISTORY }

/** Navigation graph routes. */
object Routes {
    const val TABS = "tabs"
    const val EDITOR = "editor"
    const val ACTIVE = "active"
    const val SUMMARY = "summary"
    const val DETAIL = "detail"
    const val EDIT_HISTORY = "edit_history"
}

/** One-shot navigation commands, executed by the NavController in AppRoot. */
sealed interface NavCmd {
    data class To(
        val route: String,
        val popUpToRoute: String? = null,
        val inclusive: Boolean = false,
    ) : NavCmd

    data object Back : NavCmd

    /** Pop the back stack until [route] is on top. */
    data class PopTo(val route: String) : NavCmd
}

/** What the editor screen is editing. */
data class EditorTarget(val workout: Workout, val isNew: Boolean)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val feedback = Feedback(app)

    private val _workouts = MutableStateFlow(repo.loadWorkouts())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    private val _history = MutableStateFlow(repo.loadHistory())
    val history: StateFlow<List<WorkoutResult>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(repo.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _tab = MutableStateFlow(Tab.HOME)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    private val _nav = MutableSharedFlow<NavCmd>(extraBufferCapacity = 16)
    val nav: SharedFlow<NavCmd> = _nav.asSharedFlow()

    // Data backing the secondary destinations.
    private val _editing = MutableStateFlow<EditorTarget?>(null)
    val editing: StateFlow<EditorTarget?> = _editing.asStateFlow()

    private val _result = MutableStateFlow<WorkoutResult?>(null)
    val result: StateFlow<WorkoutResult?> = _result.asStateFlow()

    private val _detail = MutableStateFlow<WorkoutResult?>(null)
    val detail: StateFlow<WorkoutResult?> = _detail.asStateFlow()

    private val _resumePrompt = MutableStateFlow(false)
    val resumePrompt: StateFlow<Boolean> = _resumePrompt.asStateFlow()

    private var pendingSession: Session? = null
    val resumeWorkoutName: String? get() = pendingSession?.workoutName

    var controller: ActiveController? = null
        private set
    private var collectorJob: kotlinx.coroutines.Job? = null
    private val _activeState = MutableStateFlow<ActiveState?>(null)
    val activeState: StateFlow<ActiveState?> = _activeState.asStateFlow()

    init {
        val s = repo.loadSession()
        if (s != null && s.idx < s.steps.size) {
            pendingSession = s
            _resumePrompt.value = true
        }
    }

    private fun navigate(cmd: NavCmd) { _nav.tryEmit(cmd) }

    // ── Tabs ───────────────────────────────────────────────
    fun setTab(tab: Tab) { _tab.value = tab }

    // ── Settings ───────────────────────────────────────────
    fun toggleSound() = updateSettings { it.copy(sound = !it.sound) }
    fun toggleVibrate() = updateSettings { it.copy(vibrate = !it.vibrate) }
    private inline fun updateSettings(block: (Settings) -> Settings) {
        val next = block(_settings.value)
        _settings.value = next
        repo.saveSettings(next)
    }

    // ── Resume ─────────────────────────────────────────────
    fun resumeSession() {
        val s = pendingSession ?: return
        attachController(s)
        _resumePrompt.value = false
        navigate(NavCmd.To(Routes.ACTIVE))
    }

    fun dismissResume() {
        pendingSession = null
        repo.saveSession(null)
        _resumePrompt.value = false
    }

    // ── Start / run a workout ──────────────────────────────
    fun startWorkout(workout: Workout) {
        val steps = buildSchedule(workout, COUNTDOWN_SECONDS)
        val session = Session(
            id = uid(),
            workoutId = workout.id,
            workoutName = workout.name,
            steps = steps,
            startedAt = System.currentTimeMillis(),
            estimated = steps.sumOf { it.duration },
            idx = 0,
            remaining = steps.first().duration.toDouble(),
        )
        repo.saveSession(session)
        attachController(session)
        navigate(NavCmd.To(Routes.ACTIVE))
    }

    private fun attachController(session: Session) {
        controller?.detach()
        collectorJob?.cancel()
        val c = ActiveController(
            session = session,
            repo = repo,
            feedback = feedback,
            scope = viewModelScope,
            settingsProvider = { _settings.value },
            onFinished = { result -> onFinish(result) },
        )
        controller = c
        // mirror the controller's state into our exposed flow
        collectorJob = viewModelScope.launch {
            c.state.collect { _activeState.value = it }
        }
    }

    private fun onFinish(result: WorkoutResult) {
        controller?.detach()
        controller = null
        _activeState.value = null
        _result.value = result
        // The finished workout replaces the active screen on the back stack.
        navigate(NavCmd.To(Routes.SUMMARY, popUpToRoute = Routes.ACTIVE, inclusive = true))
    }

    // ── Summary result ─────────────────────────────────────
    fun saveResult(out: WorkoutResult) {
        _history.value = listOf(out) + _history.value
        repo.saveHistory(_history.value)
        _result.value = null
        _tab.value = Tab.HISTORY
        navigate(NavCmd.PopTo(Routes.TABS))
    }

    fun discardResult() {
        _result.value = null
        navigate(NavCmd.PopTo(Routes.TABS))
    }

    // ── History ────────────────────────────────────────────
    fun openDetail(entry: WorkoutResult) {
        _detail.value = entry
        navigate(NavCmd.To(Routes.DETAIL))
    }

    fun editHistory(entry: WorkoutResult) {
        _detail.value = entry
        navigate(NavCmd.To(Routes.EDIT_HISTORY))
    }

    /** Save edits to an existing history entry, keeping its place in the list. */
    fun updateHistory(out: WorkoutResult) {
        _history.value = _history.value.map { if (it.id == out.id) out else it }
        repo.saveHistory(_history.value)
        _detail.value = out
        navigate(NavCmd.Back)
    }

    fun deleteHistory(id: String) {
        _history.value = _history.value.filterNot { it.id == id }
        repo.saveHistory(_history.value)
    }

    fun clearHistory() {
        _history.value = emptyList()
        repo.saveHistory(_history.value)
    }

    // ── Workout library editing ────────────────────────────
    fun newWorkout() {
        val w = Workout(
            id = uid(), name = "New workout", createdAt = System.currentTimeMillis(),
            supersets = listOf(
                Superset(
                    id = uid(), name = "Block 1", sets = 3,
                    restBetweenSets = 60, restAfter = 120,
                    exercises = listOf(Exercise(uid(), "", 30)),
                )
            ),
        )
        _editing.value = EditorTarget(w, isNew = true)
        navigate(NavCmd.To(Routes.EDITOR))
    }

    fun editWorkout(id: String) {
        val w = _workouts.value.find { it.id == id } ?: return
        _editing.value = EditorTarget(w, isNew = false)
        navigate(NavCmd.To(Routes.EDITOR))
    }

    fun duplicateWorkout(id: String) {
        val list = _workouts.value
        val i = list.indexOfFirst { it.id == id }
        if (i < 0) return
        val src = list[i]
        val copy = src.copy(
            id = uid(),
            name = src.name + " copy",
            supersets = src.supersets.map { ss ->
                ss.copy(id = uid(), exercises = ss.exercises.map { it.copy(id = uid()) })
            },
        )
        val next = list.toMutableList().apply { add(i + 1, copy) }
        _workouts.value = next
        repo.saveWorkouts(next)
    }

    fun deleteWorkout(id: String) {
        _workouts.value = _workouts.value.filterNot { it.id == id }
        repo.saveWorkouts(_workouts.value)
    }

    fun saveWorkout(w: Workout) {
        val fixed = w.copy(
            supersets = w.supersets.mapIndexed { i, ss ->
                ss.copy(exercises = ss.exercises.mapIndexed { j, e ->
                    if (e.name.isBlank()) e.copy(name = "Exercise ${i + 1}-${j + 1}") else e
                })
            }
        )
        val list = _workouts.value
        val idx = list.indexOfFirst { it.id == fixed.id }
        _workouts.value = if (idx < 0) list + fixed
        else list.toMutableList().apply { this[idx] = fixed }
        repo.saveWorkouts(_workouts.value)
        _editing.value = null
        navigate(NavCmd.Back)
    }

    fun cancelEditor() {
        _editing.value = null
        navigate(NavCmd.Back)
    }

    override fun onCleared() {
        controller?.detach()
        feedback.release()
        super.onCleared()
    }
}
