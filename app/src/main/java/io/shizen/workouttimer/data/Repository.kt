package io.shizen.workouttimer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple persistence backed by SharedPreferences with JSON payloads.
 * Mirrors the localStorage-based storage in the design prototype.
 */
class Repository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("workout_timer", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── Workouts ───────────────────────────────────────────
    fun loadWorkouts(): List<Workout> {
        val raw = prefs.getString(KEY_WORKOUTS, null)
        if (raw != null) {
            runCatching { json.decodeFromString<List<Workout>>(raw) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        val def = listOf(makeDefaultWorkout())
        saveWorkouts(def)
        return def
    }

    fun saveWorkouts(list: List<Workout>) =
        prefs.edit().putString(KEY_WORKOUTS, json.encodeToString(list)).apply()

    // ── History ────────────────────────────────────────────
    fun loadHistory(): List<WorkoutResult> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<WorkoutResult>>(raw) }.getOrDefault(emptyList())
    }

    fun saveHistory(list: List<WorkoutResult>) =
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(list)).apply()

    // ── Session (resume) ───────────────────────────────────
    fun loadSession(): Session? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString<Session>(raw) }.getOrNull()
    }

    fun saveSession(session: Session?) {
        prefs.edit().apply {
            if (session != null) putString(KEY_SESSION, json.encodeToString(session))
            else remove(KEY_SESSION)
        }.apply()
    }

    // ── Settings ───────────────────────────────────────────
    fun loadSettings(): Settings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return Settings()
        return runCatching { json.decodeFromString<Settings>(raw) }.getOrDefault(Settings())
    }

    fun saveSettings(settings: Settings) =
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()

    private companion object {
        const val KEY_WORKOUTS = "wt_workouts_v1"
        const val KEY_HISTORY = "wt_history_v1"
        const val KEY_SESSION = "wt_session_v1"
        const val KEY_SETTINGS = "wt_settings_v1"
    }
}
