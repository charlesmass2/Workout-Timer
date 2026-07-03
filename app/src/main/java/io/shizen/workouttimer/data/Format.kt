package io.shizen.workouttimer.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val EN = Locale.US

/** mm:ss */
fun fmtClock(totalSec: Double): String {
    val t = max(0, totalSec.roundToInt())
    val m = t / 60
    val s = t % 60
    return "$m:" + s.toString().padStart(2, '0')
}

fun fmtClock(totalSec: Int): String = fmtClock(totalSec.toDouble())

/** Compact human duration: "1h 05m" / "12m 30s" / "45s" */
fun fmtLong(totalSec: Int): String {
    val t = max(0, totalSec)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return when {
        h > 0 -> "${h}h " + m.toString().padStart(2, '0') + "m"
        m > 0 -> "${m}m " + s.toString().padStart(2, '0') + "s"
        else -> "${s}s"
    }
}

fun fmtDate(ts: Long): String =
    SimpleDateFormat("EEE, MMM d", EN).format(Date(ts))

fun fmtTime(ts: Long): String =
    SimpleDateFormat("h:mm a", EN).format(Date(ts))

fun fmtDayKey(ts: Long): String =
    SimpleDateFormat("EEEE, MMMM d, yyyy", EN).format(Date(ts))

/** Signed compact duration: "+12m 30s" / "-45s". */
fun signedLong(deltaSec: Int): String =
    (if (deltaSec >= 0) "+" else "-") + fmtLong(abs(deltaSec))
