package io.shizen.workouttimer.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.shizen.workouttimer.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A small window drawn over other apps (TYPE_APPLICATION_OVERLAY) hosting Compose
 * [content]. It can be dragged anywhere on screen, and remembers its position
 * between show/hide cycles. Requires the "Display over other apps" permission.
 */
class FloatingOverlay(
    context: Context,
    private val content: @Composable () -> Unit,
) {
    private val context = context.applicationContext
    private val windowManager = this.context.getSystemService(WindowManager::class.java)
    private val density = this.context.resources.displayMetrics.density

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Never steal focus from the app below, and keep the screen awake like the active screen does.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = (12 * density).roundToInt()
        y = (96 * density).roundToInt()
    }

    private var root: DragFrame? = null
    private var owner: OverlayOwner? = null

    /** Add the window if it is not already shown and the permission is granted. */
    fun show() {
        if (root != null || !canDraw(context)) {
            return
        }
        val themed = ContextThemeWrapper(context, R.style.Theme_WorkoutTimer)
        val newOwner = OverlayOwner()
        var startX = 0
        var startY = 0
        val frame = DragFrame(
            context = themed,
            onDragStart = {
                startX = params.x
                startY = params.y
            },
            onDragBy = { dx, dy ->
                // Gravity is END, so x grows towards the left.
                params.x = startX - dx
                params.y = startY + dy
                root?.let { view -> runCatching { windowManager.updateViewLayout(view, params) } }
            },
        )
        frame.setViewTreeLifecycleOwner(newOwner)
        frame.setViewTreeSavedStateRegistryOwner(newOwner)
        frame.addView(ComposeView(themed).apply { setContent(content) })
        newOwner.resume()
        val added = runCatching { windowManager.addView(frame, params) }.isSuccess
        if (!added) {
            newOwner.destroy()
            return
        }
        root = frame
        owner = newOwner
    }

    /** Remove the window if shown. */
    fun hide() {
        val frame = root ?: return
        runCatching { windowManager.removeView(frame) }
        owner?.destroy()
        root = null
        owner = null
    }

    companion object {
        /** Whether the user granted the "Display over other apps" permission. */
        fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Open the system screen where the user grants the "Display over other apps" permission. */
        fun requestPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }
}

/**
 * Root of the overlay window. Taps go through to the Compose content; once a
 * gesture moves beyond the touch slop it is intercepted and turned into a drag,
 * using raw screen coordinates so moving the window does not skew the deltas.
 */
@SuppressLint("ViewConstructor")
private class DragFrame(
    context: Context,
    private val onDragStart: () -> Unit,
    private val onDragBy: (dx: Int, dy: Int) -> Unit,
) : FrameLayout(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        return dragging
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        if (dragging && ev.actionMasked == MotionEvent.ACTION_MOVE) {
            onDragBy((ev.rawX - downX).roundToInt(), (ev.rawY - downY).roundToInt())
        }
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            dragging = false
        }
        return true
    }

    private fun track(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
                onDragStart()
            }
            MotionEvent.ACTION_MOVE -> {
                val moved = abs(ev.rawX - downX) > touchSlop || abs(ev.rawY - downY) > touchSlop
                if (!dragging && moved) {
                    dragging = true
                }
            }
        }
    }
}

/** Minimal lifecycle + saved-state owner so Compose can run outside an Activity. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun resume() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
