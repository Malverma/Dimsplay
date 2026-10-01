package io.github.malverma.dimsplay.service

import android.content.Context
import android.provider.Settings

data class SavedBrightness(val brightness: Int, val mode: Int)

interface SavedBrightnessStore {
    val savedBrightness: SavedBrightness?
    fun save(saved: SavedBrightness)
    fun clear()
}

interface SystemBrightness {
    val canWrite: Boolean
    var brightness: Int
    var mode: Int
}

class SettingsBrightness(context: Context) : SystemBrightness {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver

    override val canWrite: Boolean
        get() = Settings.System.canWrite(context)

    override var brightness: Int
        get() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, MAX_BRIGHTNESS)
        set(value) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }

    override var mode: Int
        get() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, MODE_MANUAL)
        set(value) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, value)
        }

    private companion object {
        const val MAX_BRIGHTNESS = 255
    }
}

/** "Extra dim": drops the system brightness to its minimum and puts it back afterwards. */
class BrightnessController(
    private val system: SystemBrightness,
    private val store: SavedBrightnessStore,
) {
    /**
     * Saves the current brightness and mode (unless an earlier call already saved them) and sets
     * the brightness to its minimum. Returns false if Dimsplay may not change system settings.
     */
    fun lower(): Boolean {
        if (!system.canWrite) return false
        return try {
            if (store.savedBrightness == null) store.save(SavedBrightness(system.brightness, system.mode))
            system.mode = MODE_MANUAL
            system.brightness = MIN_BRIGHTNESS
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Writes the saved brightness and mode back and forgets them. Returns false if values were
     * saved but could not be written back, because the permission was revoked.
     */
    fun restore(): Boolean {
        val saved = store.savedBrightness ?: return true
        val restored = system.canWrite && try {
            system.brightness = saved.brightness
            system.mode = saved.mode
            true
        } catch (e: SecurityException) {
            false
        }
        store.clear()
        return restored
    }
}

const val MODE_MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL

/** Lowest non-zero value; the system clamps it to the panel's real minimum. */
const val MIN_BRIGHTNESS = 1
