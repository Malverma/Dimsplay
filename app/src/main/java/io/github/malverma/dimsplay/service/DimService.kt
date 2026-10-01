package io.github.malverma.dimsplay.service

import android.app.AppOpsManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.malverma.dimsplay.R
import io.github.malverma.dimsplay.data.DimPrefs
import io.github.malverma.dimsplay.data.DimState
import io.github.malverma.dimsplay.data.overlayAlpha
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground service that owns the dim overlay and, with Extra dim, the lowered brightness. */
class DimService : Service() {
    private val scope = MainScope()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: DimPrefs
    private lateinit var overlay: OverlayController
    private lateinit var brightness: BrightnessController
    private lateinit var notifications: NotificationHelper
    private var collectors: List<Job> = emptyList()
    private var dimming = false

    // The system drops notification updates posted faster than a few per second, so slider
    // drags only refresh the notification once the level settles.
    private val refreshNotification = Runnable { notifications.update(DimState.level.value) }

    // Notices the user revoking a permission in system settings while dimming runs in the background.
    private val permissionListener = AppOpsManager.OnOpChangedListener { _, _ ->
        handler.post(::checkPermissions)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = DimPrefs(this)
        overlay = OverlayController(this)
        brightness = BrightnessController(SettingsBrightness(this), prefs)
        notifications = NotificationHelper(this).apply { createChannel() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_STEP -> step(intent.getIntExtra(EXTRA_DELTA, 0))
            else -> if (!dimming) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        teardown()
        scope.cancel()
        super.onDestroy()
    }

    private fun start() {
        // startForeground must be called after every startForegroundService, even if we bail out.
        val notification = notifications.build(DimState.level.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
        if (dimming) return
        if (!overlay.show(overlayAlpha(DimState.level.value))) {
            stop()
            return
        }
        dimming = true
        DimState.setRunning(true)
        getSystemService(AppOpsManager::class.java).apply {
            startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, permissionListener)
            startWatchingMode(AppOpsManager.OPSTR_WRITE_SETTINGS, packageName, permissionListener)
        }
        collectors = listOf(
            scope.launch { DimState.level.collect { onLevel(it) } },
            scope.launch { DimState.extraDim.collect { onExtraDim(it) } },
        )
    }

    private fun step(delta: Int) {
        if (!dimming) {
            stopSelf()
            return
        }
        DimState.setLevel(DimState.level.value + delta)
        prefs.level = DimState.level.value
    }

    private fun onLevel(level: Int) {
        if (!overlay.setAlpha(overlayAlpha(level))) {
            // The overlay window is gone, most likely because the permission was revoked.
            stop()
            return
        }
        handler.removeCallbacks(refreshNotification)
        handler.postDelayed(refreshNotification, NOTIFICATION_REFRESH_DELAY_MS)
    }

    private fun onExtraDim(enabled: Boolean) {
        if (enabled) {
            if (!brightness.lower()) disableExtraDim()
        } else {
            restoreBrightness()
        }
    }

    private fun checkPermissions() {
        if (!dimming) return
        if (!Settings.canDrawOverlays(this)) {
            stop()
        } else if (DimState.extraDim.value && !Settings.System.canWrite(this)) {
            // Turning Extra dim off tries to restore brightness, which now fails and shows a notice.
            disableExtraDim(notify = false)
        }
    }

    private fun disableExtraDim(notify: Boolean = true) {
        prefs.extraDim = false
        DimState.setExtraDim(false)
        if (notify) showNotice(R.string.extra_dim_lost)
    }

    private fun stop() {
        teardown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun teardown() {
        handler.removeCallbacks(refreshNotification)
        collectors.forEach { it.cancel() }
        collectors = emptyList()
        if (dimming) {
            getSystemService(AppOpsManager::class.java).stopWatchingMode(permissionListener)
            overlay.hide()
            restoreBrightness()
            dimming = false
        }
        DimState.setRunning(false)
    }

    private fun restoreBrightness() {
        if (!brightness.restore()) showNotice(R.string.brightness_not_restored)
    }

    private fun showNotice(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION_START = "io.github.malverma.dimsplay.action.START"
        const val ACTION_STOP = "io.github.malverma.dimsplay.action.STOP"
        const val ACTION_STEP = "io.github.malverma.dimsplay.action.STEP"
        private const val EXTRA_DELTA = "delta"
        private const val NOTIFICATION_REFRESH_DELAY_MS = 300L

        fun intent(context: Context, action: String, delta: Int = 0): Intent =
            Intent(context, DimService::class.java)
                .setAction(action)
                .putExtra(EXTRA_DELTA, delta)
    }
}
