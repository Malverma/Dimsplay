package io.github.malverma.dimsplay.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/** Owns the full-screen black overlay window. Its opacity is the window alpha. */
class OverlayController(context: Context) {
    private val context: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        context.createDisplayContext(display).createWindowContext(LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else {
        context
    }
    private val windowManager = this.context.getSystemService(WindowManager::class.java)
    private var view: View? = null

    // The window alpha (not the view alpha) is what Android 12+ compares against the 0.8 limit
    // for letting touches through, so the view stays opaque black and the window carries the dim.
    private val params = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
        LayoutParams.TYPE_APPLICATION_OVERLAY,
        LayoutParams.FLAG_NOT_TOUCHABLE or
            LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        title = "Dimsplay overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            // By default the window is inset to stay clear of the navigation bar; cover it too.
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /** Adds the overlay. Returns false if it could not be added, e.g. the permission is missing. */
    fun show(alpha: Float): Boolean {
        if (view != null) return setAlpha(alpha)
        if (!Settings.canDrawOverlays(context)) return false
        val overlay = View(context).apply { setBackgroundColor(Color.BLACK) }
        params.alpha = alpha
        return try {
            windowManager.addView(overlay, params)
            view = overlay
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    /** Updates the existing overlay in place. Returns false if the window is gone. */
    fun setAlpha(alpha: Float): Boolean {
        val overlay = view ?: return false
        if (params.alpha == alpha) return true
        params.alpha = alpha
        return try {
            windowManager.updateViewLayout(overlay, params)
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    fun hide() {
        val overlay = view ?: return
        view = null
        try {
            windowManager.removeView(overlay)
        } catch (e: RuntimeException) {
            // Already removed by the system, e.g. after the permission was revoked.
        }
    }
}
