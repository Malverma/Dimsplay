package io.github.malverma.dimsplay.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimLevelTest {
    @Test
    fun zeroLevelIsFullyTransparent() {
        assertEquals(0f, overlayAlpha(0), 0f)
    }

    @Test
    fun fullLevelMapsToTouchSafeCap() {
        assertEquals(MAX_OVERLAY_ALPHA, overlayAlpha(MAX_LEVEL), 0f)
        assertTrue(overlayAlpha(MAX_LEVEL) <= 0.8f)
    }

    @Test
    fun levelScalesLinearly() {
        assertEquals(0.4f, overlayAlpha(50), 1e-6f)
        assertEquals(0.32f, overlayAlpha(40), 1e-6f)
    }

    @Test
    fun outOfRangeLevelsAreClamped() {
        assertEquals(0f, overlayAlpha(-20), 0f)
        assertEquals(MAX_OVERLAY_ALPHA, overlayAlpha(250), 0f)
    }
}
