package io.shizen.workouttimer.timer

import android.os.SystemClock
import io.shizen.workouttimer.data.Repository
import io.shizen.workouttimer.data.Session
import io.shizen.workouttimer.data.SetResult
import io.shizen.workouttimer.data.Settings
import io.shizen.workouttimer.data.Step
import io.shizen.workouttimer.data.StepKind
import io.shizen.workouttimer.data.WorkoutResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** Immutable snapshot the UI renders from. */
data class ActiveState(
    val steps: List<Step>,
    val idx: Int,
    val remaining: Double,
    val running: Boolean,
    val started: Boolean,
    val globalElapsed: Double,
    val reps: Map<Int, Int>,
    val workoutName: String,
) {
    val current: Step get() = steps[idx]
    val totalWork: Int get() = steps.count { it.kind == StepKind.WORK }
    val doneWork: Int get() = steps.filterIndexed { i, s -> s.kind == StepKind.WORK && i < idx }.size
}

/**
 * Drives a live workout: the dedicated per-step timer plus the global stopwatch.
 * Ticks ~10x/second and advances through the schedule, firing feedback at each
 * natural timer end. A faithful port of the prototype's ActiveWorkout engine.
 */
class ActiveController(
    private val session: Session,
    private val repo: Repository,
    private val feedback: Feedback,
    private val scope: CoroutineScope,
    private val settingsProvider: () -> Settings,
    private val onFinished: (WorkoutResult) -> Unit,
) {
    private val steps = session.steps

    private var idx = session.idx
    private var remaining =
        if (session.remaining > 0) session.remaining else steps[session.idx].duration.toDouble()
    private var running = false // never auto-resume into a running state
    private var started = session.started
    private var globalElapsed = session.globalElapsed
    private val reps = session.reps.toMutableMap()
    private val manual = session.manual.toMutableSet()
    private val actuals = session.actuals.toMutableMap()

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<ActiveState> = _state

    private var job: Job? = null
    private var lastTs = 0L
    private var persistCounter = 0
    private var done = false

    init {
        job = scope.launch {
            lastTs = SystemClock.elapsedRealtime()
            while (true) {
                delay(100)
                val now = SystemClock.elapsedRealtime()
                val dt = (now - lastTs) / 1000.0
                lastTs = now
                if (running && started) {
                    remaining -= dt
                    globalElapsed += dt
                    if (remaining <= 0) {
                        recordActual(natural = true)
                        val ni = idx + 1
                        if (ni >= steps.size) {
                            finish()
                            return@launch
                        }
                        idx = ni
                        remaining = steps[ni].duration.toDouble()
                        feedback.fire(settingsProvider())
                        persist()
                    } else if (++persistCounter >= 10) {
                        persistCounter = 0
                        persist()
                    }
                    emit()
                }
            }
        }
    }

    private fun snapshot() = ActiveState(
        steps = steps, idx = idx, remaining = remaining, running = running,
        started = started, globalElapsed = globalElapsed, reps = reps.toMap(),
        workoutName = session.workoutName,
    )

    private fun emit() { _state.value = snapshot() }

    private fun persist() {
        repo.saveSession(
            session.copy(
                idx = idx, remaining = remaining, started = started,
                globalElapsed = globalElapsed, reps = reps.toMap(),
                manual = manual.toList(), actuals = actuals.toMap(),
            )
        )
    }

    private fun stepIndexOfWork(wi: Int) =
        steps.indexOfFirst { it.kind == StepKind.WORK && it.workIndex == wi }

    private fun recordActual(natural: Boolean) {
        val cur = steps[idx]
        if (cur.kind == StepKind.WORK) {
            val spent = if (natural) cur.duration.toDouble()
            else cur.duration - max(0.0, remaining)
            actuals[cur.workIndex] = max(0.0, spent)
        }
    }

    // ── User actions ───────────────────────────────────────

    fun start() {
        started = true
        running = true
        lastTs = SystemClock.elapsedRealtime()
        persist(); emit()
    }

    fun togglePause() {
        running = !running
        if (running) lastTs = SystemClock.elapsedRealtime()
        persist(); emit()
    }

    /** Manually move to the next step (finish set / skip rest / skip countdown). */
    fun advance() {
        recordActual(natural = false)
        val ni = idx + 1
        if (ni >= steps.size) { finish(); return }
        idx = ni
        remaining = steps[ni].duration.toDouble()
        persist(); emit()
    }

    /** Log reps for the current work step. */
    fun setReps(value: Int) {
        val cur = steps[idx]
        if (cur.kind != StepKind.WORK) return
        applyReps(cur, value)
    }

    /** Log reps for an explicit work step (e.g. the set just finished, during rest). */
    fun setRepsForWorkIndex(workIndex: Int, value: Int) {
        val step = steps.firstOrNull { it.kind == StepKind.WORK && it.workIndex == workIndex } ?: return
        applyReps(step, value)
    }

    private fun applyReps(cur: Step, value: Int) {
        val wi = cur.workIndex
        reps[wi] = value
        manual.add(wi)
        // autofill following sets of the same exercise that weren't set manually
        steps.forEach { s ->
            if (s.kind == StepKind.WORK && s.workIndex > wi &&
                s.supersetIndex == cur.supersetIndex && s.exerciseName == cur.exerciseName &&
                !manual.contains(s.workIndex)
            ) {
                reps[s.workIndex] = value
            }
        }
        persist(); emit()
    }

    /** Finish from the confirm dialog: record the in-progress step then close out. */
    fun finishNow() {
        recordActual(natural = false)
        finish()
    }

    private fun finish() {
        if (done) return
        done = true
        running = false
        job?.cancel()
        repo.saveSession(null)
        onFinished(buildResult())
    }

    /** Called when leaving the screen without finishing — just stop ticking. */
    fun detach() {
        job?.cancel()
    }

    private fun buildResult(): WorkoutResult {
        return WorkoutResult(
            id = session.id,
            workoutId = session.workoutId,
            workoutName = session.workoutName,
            startedAt = session.startedAt,
            endedAt = System.currentTimeMillis(),
            totalElapsed = globalElapsed.roundToInt(),
            estimated = session.estimated,
            satisfaction = null,
            breathless = null,
            sets = steps.filter { it.kind == StepKind.WORK }.map { s ->
                val passed = idx > stepIndexOfWork(s.workIndex)
                val actual = actuals[s.workIndex] ?: (if (passed) s.duration.toDouble() else 0.0)
                SetResult(
                    workIndex = s.workIndex,
                    supersetIndex = s.supersetIndex,
                    supersetName = s.supersetName,
                    exerciseName = s.exerciseName,
                    setNumber = s.setNumber,
                    totalSets = s.totalSets,
                    planned = s.duration,
                    actual = actual.roundToInt(),
                    reps = reps[s.workIndex],
                )
            },
        )
    }
}
