package io.github.malverma.dimsplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.malverma.dimsplay.MainActivity
import io.github.malverma.dimsplay.R
import io.github.malverma.dimsplay.data.LEVEL_STEP

class NotificationHelper(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(level: Int): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_text, level))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.action_stop), serviceIntent(REQUEST_STOP, DimService.ACTION_STOP))
            .addAction(0, context.getString(R.string.action_decrease), serviceIntent(REQUEST_DECREASE, DimService.ACTION_STEP, -LEVEL_STEP))
            .addAction(0, context.getString(R.string.action_increase), serviceIntent(REQUEST_INCREASE, DimService.ACTION_STEP, LEVEL_STEP))
            .build()
    }

    fun update(level: Int) {
        manager.notify(NOTIFICATION_ID, build(level))
    }

    private fun serviceIntent(requestCode: Int, action: String, delta: Int = 0): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            DimService.intent(context, action, delta),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "dimming"
        private const val REQUEST_STOP = 1
        private const val REQUEST_DECREASE = 2
        private const val REQUEST_INCREASE = 3
    }
}
