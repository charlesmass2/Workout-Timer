package io.shizen.workouttimer.data

import androidx.annotation.StringRes
import io.shizen.workouttimer.R
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Generate a short random id, mirroring the design's `uid()`. */
fun uid(): String = Random.nextLong().toULong().toString(36).take(7)

// ── Workout model ───────────────────────────────────────────

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val duration: Int, // seconds
)

@Serializable
data class Superset(
    val id: String,
    val name: String,
    val sets: Int,
    val restBetweenSets: Int, // seconds
    val restAfter: Int,       // seconds, rest after the whole block
    val exercises: List<Exercise>,
)

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val createdAt: Long,
    val supersets: List<Superset>,
)

// ── Schedule steps ──────────────────────────────────────────

enum class StepKind { COUNTDOWN, WORK, REST }
enum class RestType { SET, SUPERSET }

@Serializable
data class Step(
    val kind: StepKind,
    val duration: Int,
    // work-only
    val supersetIndex: Int = -1,
    val supersetName: String = "",
    val exerciseIndex: Int = -1,
    val exerciseId: String = "",
    val exerciseName: String = "",
    val exCount: Int = 0,
    val setNumber: Int = 0,
    val totalSets: Int = 0,
    val workIndex: Int = -1,
    // rest-only
    val restType: RestType? = null,
    val fromSuperset: String = "",
    val nextSuperset: String = "",
    val nextExercise: String = "",
    val supersetNameRest: String = "",
)

// ── History result ──────────────────────────────────────────

@Serializable
data class SetResult(
    val workIndex: Int,
    val supersetIndex: Int,
    val supersetName: String,
    val exerciseName: String,
    val setNumber: Int,
    val totalSets: Int,
    val planned: Int,
    val actual: Int,
    val reps: Int? = null,
)

@Serializable
data class WorkoutResult(
    val id: String,
    val workoutId: String,
    val workoutName: String,
    val startedAt: Long,
    val endedAt: Long,
    val totalElapsed: Int,
    val estimated: Int,
    val satisfaction: Int? = null,
    val breathless: Int? = null,
    val sets: List<SetResult>,
)

// ── Settings ────────────────────────────────────────────────

@Serializable
data class Settings(
    val sound: Boolean = true,
    val vibrate: Boolean = true,
)

// ── Persisted in-progress session (for resume) ──────────────

@Serializable
data class Session(
    val id: String,
    val workoutId: String,
    val workoutName: String,
    val steps: List<Step>,
    val startedAt: Long,
    val estimated: Int,
    val idx: Int = 0,
    val remaining: Double = 0.0,
    val started: Boolean = false,
    val globalElapsed: Double = 0.0,
    val reps: Map<Int, Int> = emptyMap(),
    val manual: List<Int> = emptyList(),
    val actuals: Map<Int, Double> = emptyMap(),
)

// ── Rating scales ───────────────────────────────────────────

data class Rating(val key: Int, @StringRes val labelRes: Int)

val SATISFACTION = listOf(
    Rating(1, R.string.rating_poor),
    Rating(2, R.string.rating_okay),
    Rating(3, R.string.rating_good),
    Rating(4, R.string.rating_great),
)
val BREATHLESS = listOf(
    Rating(1, R.string.rating_easy),
    Rating(2, R.string.rating_moderate),
    Rating(3, R.string.rating_hard),
    Rating(4, R.string.rating_maxed),
)
