package io.github.malverma.dimsplay.data

import android.content.Context
import androidx.core.content.edit
import io.github.malverma.dimsplay.service.SavedBrightness
import io.github.malverma.dimsplay.service.SavedBrightnessStore

class DimPrefs(context: Context) : SavedBrightnessStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var level: Int
        get() = prefs.getInt(KEY_LEVEL, DEFAULT_LEVEL).coerceIn(MIN_LEVEL, MAX_LEVEL)
        set(value) = prefs.edit { putInt(KEY_LEVEL, value) }

    var extraDim: Boolean
        get() = prefs.getBoolean(KEY_EXTRA_DIM, false)
        set(value) = prefs.edit { putBoolean(KEY_EXTRA_DIM, value) }

    override val savedBrightness: SavedBrightness?
        get() = if (prefs.contains(KEY_SAVED_BRIGHTNESS) && prefs.contains(KEY_SAVED_BRIGHTNESS_MODE)) {
            SavedBrightness(
                brightness = prefs.getInt(KEY_SAVED_BRIGHTNESS, 0),
                mode = prefs.getInt(KEY_SAVED_BRIGHTNESS_MODE, 0),
            )
        } else {
            null
        }

    // Written synchronously: these values must survive the process dying right after the
    // system brightness is lowered, or the original brightness is lost.
    override fun save(saved: SavedBrightness) = prefs.edit(commit = true) {
        putInt(KEY_SAVED_BRIGHTNESS, saved.brightness)
        putInt(KEY_SAVED_BRIGHTNESS_MODE, saved.mode)
    }

    override fun clear() = prefs.edit(commit = true) {
        remove(KEY_SAVED_BRIGHTNESS)
        remove(KEY_SAVED_BRIGHTNESS_MODE)
    }

    private companion object {
        const val FILE_NAME = "dimsplay"
        const val KEY_LEVEL = "level"
        const val KEY_EXTRA_DIM = "extra_dim"
        const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
        const val KEY_SAVED_BRIGHTNESS_MODE = "saved_brightness_mode"
    }
}
