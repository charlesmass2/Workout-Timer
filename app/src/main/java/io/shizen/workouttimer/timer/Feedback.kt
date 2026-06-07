package io.shizen.workouttimer.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.shizen.workouttimer.data.Settings

/**
 * Plays the end-of-timer feedback: a ~2 second pattern of beeps and/or
 * vibration, gated by the user's sound / vibrate settings.
 */
class Feedback(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Volatile
    private var tone: ToneGenerator? = null

    /** A ~2s pattern: six pulses, accelerating slightly toward the end. */
    private val vibrationPattern =
        longArrayOf(0, 300, 150, 300, 150, 300, 150, 350)

    fun fire(settings: Settings) {
        if (settings.vibrate) vibrate()
        if (settings.sound) beep()
    }

    private fun vibrate() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(vibrationPattern, -1)
            }
        }
    }

    private fun beep() {
        runCatching {
            val gen = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 90).also { tone = it }
            // Five short tones over ~2s, mirroring the prototype's beep cadence.
            val pattern = listOf(
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_BEEP2,
            )
            Thread {
                pattern.forEach { t ->
                    runCatching {
                        gen.startTone(t, 180)
                        Thread.sleep(400)
                    }
                }
            }.start()
        }
    }

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }
}
