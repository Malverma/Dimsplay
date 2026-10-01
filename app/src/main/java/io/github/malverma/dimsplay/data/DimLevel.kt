package io.github.malverma.dimsplay.data

const val MIN_LEVEL = 0
const val MAX_LEVEL = 100
const val DEFAULT_LEVEL = 40
const val LEVEL_STEP = 10

/**
 * Highest overlay window alpha. Android 12+ stops touches from reaching the apps under an
 * untrusted overlay whose alpha is above 0.8 (InputManager#getMaximumObscuringOpacityForTouch),
 * so a 100% dim level maps to exactly this value and never higher.
 */
const val MAX_OVERLAY_ALPHA = 0.8f

fun overlayAlpha(level: Int): Float =
    level.coerceIn(MIN_LEVEL, MAX_LEVEL) / MAX_LEVEL.toFloat() * MAX_OVERLAY_ALPHA
