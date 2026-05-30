package com.example.paradacerta.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.paradacerta.MainActivity
import com.example.paradacerta.R

object NotificationHelper {

    const val CHANNEL_RESERVAS = "channel_reservas"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_RESERVAS,
            "Lembretes de Reserva",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas sobre o horário da sua reserva"
            manager.createNotificationChannel(this)
        }
    }

    fun showReservationNotification(context: Context, notifId: Int, title: String, message: String) {
        val notifManager = NotificationManagerCompat.from(context)
        if (!notifManager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, notifId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_RESERVAS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            notifManager.notify(notifId, notification)
        } catch (_: SecurityException) { }
    }

}
