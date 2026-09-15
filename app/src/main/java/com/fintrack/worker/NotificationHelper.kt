package com.fintrack.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.fintrack.R

object NotificationHelper {
    const val EMI_CHANNEL_ID = "emi_reminders"
    const val EMI_CHANNEL_NAME = "EMI Reminders"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            EMI_CHANNEL_ID,
            EMI_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming EMI payments"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun showEmiNotification(context: Context, notifId: Int, title: String, body: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, EMI_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notification)
    }
}
