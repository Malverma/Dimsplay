package io.github.malverma.dimsplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import io.github.malverma.dimsplay.ads.Ads
import io.github.malverma.dimsplay.data.DimPrefs
import io.github.malverma.dimsplay.data.DimState
import io.github.malverma.dimsplay.service.BrightnessController
import io.github.malverma.dimsplay.service.DimController
import io.github.malverma.dimsplay.service.NotificationHelper
import io.github.malverma.dimsplay.service.SettingsBrightness
import io.github.malverma.dimsplay.ui.AdBanner
import io.github.malverma.dimsplay.ui.DimColors
import io.github.malverma.dimsplay.ui.DimScreen
import io.github.malverma.dimsplay.ui.DimsplayTheme

class MainActivity : ComponentActivity() {
    private var canDrawOverlays by mutableStateOf(false)
    private var startWhenPermitted = false
    private var enableExtraDimWhenPermitted = false
    private var askedForNotifications = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // The foreground notification posted before the grant was hidden; post it again.
            if (granted && DimState.running.value) NotificationHelper(this).update(DimState.level.value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val bars = SystemBarStyle.dark(DimColors.Bg.toArgb())
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        canDrawOverlays = Settings.canDrawOverlays(this)
        setContent {
            val level by DimState.level.collectAsState()
            val running by DimState.running.collectAsState()
            val extraDim by DimState.extraDim.collectAsState()
            val adsReady by Ads.ready.collectAsState()
            DimsplayTheme {
                // The banner sits below the screen, so it takes the bottom inset instead of DimScreen.
                Column(Modifier.fillMaxSize().background(DimColors.Bg)) {
                    DimScreen(
                        level = level,
                        dimming = running,
                        extraDim = extraDim,
                        showPermissionBanner = !canDrawOverlays,
                        onLevelChange = { value ->
                            DimController.setLevel(value)
                            // Moving the slider while dimming is off turns it on.
                            if (!DimState.running.value && canDrawOverlays) startDimming()
                        },
                        onLevelChangeFinished = { DimController.saveLevel(this@MainActivity) },
                        onDimmingChange = { on -> if (on) startDimming() else DimController.stop(this@MainActivity) },
                        onExtraDimChange = ::setExtraDim,
                        onGrantPermission = ::requestOverlayPermission,
                        modifier = Modifier
                            .weight(1f)
                            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    )
                    AdBanner(
                        ready = adsReady,
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        ),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Saved brightness while dimming is off was left behind by a service that died early.
        if (!DimState.running.value) BrightnessController(SettingsBrightness(this), DimPrefs(this)).restore()
        canDrawOverlays = Settings.canDrawOverlays(this)
        if (!canDrawOverlays) {
            // Covers the permission being revoked while dimming was on.
            DimController.stop(this)
        } else if (startWhenPermitted) {
            startDimming()
        }
        startWhenPermitted = false

        val canWriteSettings = Settings.System.canWrite(this)
        if (enableExtraDimWhenPermitted && canWriteSettings) {
            enableExtraDim()
        } else if (DimState.extraDim.value && !canWriteSettings) {
            DimController.setExtraDim(this, false)
            Toast.makeText(this, R.string.extra_dim_lost, Toast.LENGTH_LONG).show()
        }
        enableExtraDimWhenPermitted = false
    }

    private fun startDimming() {
        if (!Settings.canDrawOverlays(this)) {
            startWhenPermitted = true
            requestOverlayPermission()
            return
        }
        DimController.start(this)
        // Dimming works without notifications, so this is asked once and never blocks it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedForNotifications &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setExtraDim(enabled: Boolean) {
        if (enabled && !Settings.System.canWrite(this)) {
            // The switch stays off unless the permission is granted (checked in onResume).
            enableExtraDimWhenPermitted = true
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:$packageName".toUri()))
        } else if (enabled) {
            enableExtraDim()
        } else {
            DimController.setExtraDim(this, false)
        }
    }

    private fun enableExtraDim() {
        DimController.setExtraDim(this, true)
        // Extra dim only lowers brightness while dimming runs, so, like the slider, turning it on
        // while dimming is off turns dimming on.
        if (!DimState.running.value && canDrawOverlays) startDimming()
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
    }
}
