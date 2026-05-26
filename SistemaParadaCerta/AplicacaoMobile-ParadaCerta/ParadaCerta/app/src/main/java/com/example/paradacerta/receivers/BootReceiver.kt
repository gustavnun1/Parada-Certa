package com.example.paradacerta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.paradacerta.notifications.NotificationPreferencesManager
import com.example.paradacerta.notifications.ReservationNotificationScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!NotificationPreferencesManager.isReservasEnabled(context)) return

        val prefs = context.getSharedPreferences("reservation_alarm_prefs", Context.MODE_PRIVATE)
        val nome = prefs.getString("nome", null) ?: return
        val reservationTime = prefs.getLong("reservation_time", 0L)
        if (reservationTime <= System.currentTimeMillis()) return

        ReservationNotificationScheduler.rescheduleAfterBoot(context, nome, reservationTime)
    }
}
