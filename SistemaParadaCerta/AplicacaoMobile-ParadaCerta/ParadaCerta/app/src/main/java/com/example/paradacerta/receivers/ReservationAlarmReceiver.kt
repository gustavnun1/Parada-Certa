package com.example.paradacerta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.paradacerta.notifications.NotificationHelper
import com.example.paradacerta.notifications.NotificationPreferencesManager
import com.example.paradacerta.notifications.ReservationNotificationScheduler

class ReservationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!NotificationPreferencesManager.isReservasEnabled(context)) return

        val title   = intent.getStringExtra(ReservationNotificationScheduler.EXTRA_TITLE)   ?: return
        val message = intent.getStringExtra(ReservationNotificationScheduler.EXTRA_MESSAGE) ?: return
        val notifId = intent.getIntExtra(ReservationNotificationScheduler.EXTRA_NOTIF_ID, 20)

        NotificationHelper.showReservationNotification(context, notifId, title, message)
    }
}
