package io.github.malverma.dimsplay.service

import android.content.Context
import androidx.core.content.ContextCompat
import io.github.malverma.dimsplay.data.DimPrefs
import io.github.malverma.dimsplay.data.DimState

/** Entry points the UI uses to drive dimming. */
object DimController {
    fun start(context: Context) {
        // Flip the state right away so the switch responds instantly and repeated slider events
        // don't send repeated starts. The service resets it if the overlay can't be shown.
        DimState.setRunning(true)
        ContextCompat.startForegroundService(context, DimService.intent(context, DimService.ACTION_START))
    }

    fun stop(context: Context) {
        if (DimState.running.value) context.startService(DimService.intent(context, DimService.ACTION_STOP))
    }

    /** Applies live; the running service follows [DimState.level]. Call [saveLevel] when settled. */
    fun setLevel(level: Int) = DimState.setLevel(level)

    fun saveLevel(context: Context) {
        DimPrefs(context).level = DimState.level.value
    }

    fun setExtraDim(context: Context, enabled: Boolean) {
        DimPrefs(context).extraDim = enabled
        DimState.setExtraDim(enabled)
    }
}
