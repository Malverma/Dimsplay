package io.github.malverma.dimsplay.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MODE_AUTOMATIC = 1

class BrightnessControllerTest {
    private class FakeSystem(
        override var canWrite: Boolean = true,
        override var brightness: Int = 180,
        override var mode: Int = MODE_AUTOMATIC,
    ) : SystemBrightness

    private class FakeStore : SavedBrightnessStore {
        override var savedBrightness: SavedBrightness? = null
        override fun save(saved: SavedBrightness) {
            savedBrightness = saved
        }
        override fun clear() {
            savedBrightness = null
        }
    }

    private val system = FakeSystem()
    private val store = FakeStore()
    private val controller = BrightnessController(system, store)

    @Test
    fun lowerSavesOriginalAndDropsToMinimum() {
        assertTrue(controller.lower())

        assertEquals(SavedBrightness(180, MODE_AUTOMATIC), store.savedBrightness)
        assertEquals(MIN_BRIGHTNESS, system.brightness)
        assertEquals(MODE_MANUAL, system.mode)
    }

    @Test
    fun restorePutsBackBrightnessAndModeExactly() {
        controller.lower()

        assertTrue(controller.restore())

        assertEquals(180, system.brightness)
        assertEquals(MODE_AUTOMATIC, system.mode)
        assertNull(store.savedBrightness)
    }

    @Test
    fun loweringTwiceKeepsTheFirstSavedValues() {
        controller.lower()
        system.brightness = 90 // the user nudges brightness while dimming

        controller.lower()
        controller.restore()

        assertEquals(180, system.brightness)
    }

    @Test
    fun lowerWithoutPermissionChangesNothing() {
        system.canWrite = false

        assertFalse(controller.lower())

        assertEquals(180, system.brightness)
        assertNull(store.savedBrightness)
    }

    @Test
    fun restoreWithNothingSavedIsANoOp() {
        assertTrue(controller.restore())
        assertEquals(180, system.brightness)
    }

    @Test
    fun crashRecoveryRestoresValuesLeftInStore() {
        // A previous process lowered brightness and died before restoring.
        BrightnessController(system, store).lower()

        val afterRestart = BrightnessController(system, store)
        assertTrue(afterRestart.restore())

        assertEquals(180, system.brightness)
        assertEquals(MODE_AUTOMATIC, system.mode)
        assertNull(store.savedBrightness)
    }

    @Test
    fun restoreAfterPermissionRevokedReportsFailureAndClears() {
        controller.lower()
        system.canWrite = false

        assertFalse(controller.restore())

        assertEquals(MIN_BRIGHTNESS, system.brightness)
        assertNull(store.savedBrightness)
    }
}
