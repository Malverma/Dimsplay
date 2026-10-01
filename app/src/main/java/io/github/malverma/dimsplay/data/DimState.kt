package io.github.malverma.dimsplay.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process state shared by the UI and [io.github.malverma.dimsplay.service.DimService], so the
 * slider follows level changes made from the notification and the overlay follows the slider.
 */
object DimState {
    private val _level = MutableStateFlow(DEFAULT_LEVEL)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _extraDim = MutableStateFlow(false)
    val extraDim: StateFlow<Boolean> = _extraDim.asStateFlow()

    fun setLevel(level: Int) {
        _level.value = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
    }

    fun setRunning(running: Boolean) {
        _running.value = running
    }

    fun setExtraDim(enabled: Boolean) {
        _extraDim.value = enabled
    }
}
