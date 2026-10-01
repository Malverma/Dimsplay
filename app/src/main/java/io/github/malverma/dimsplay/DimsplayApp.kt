package io.github.malverma.dimsplay

import android.app.Application
import io.github.malverma.dimsplay.data.DimPrefs
import io.github.malverma.dimsplay.data.DimState
import io.github.malverma.dimsplay.service.BrightnessController
import io.github.malverma.dimsplay.service.SettingsBrightness

class DimsplayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = DimPrefs(this)
        DimState.setLevel(prefs.level)
        DimState.setExtraDim(prefs.extraDim)
        // A fresh process means dimming is not running, so any brightness still saved was left
        // behind by a service that died before restoring it. Put it back now.
        BrightnessController(SettingsBrightness(this), prefs).restore()
    }
}
