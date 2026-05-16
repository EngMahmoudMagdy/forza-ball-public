package com.forzaball.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.forzaball.R

object FeedNotificationChannels {

    fun ensureFeedChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            FeedPushConstants.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.feed_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.feed_notification_channel_description)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }
}
