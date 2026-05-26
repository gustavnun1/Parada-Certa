package com.example.paradacerta.notifications

import android.content.Context

object NotificationPreferencesManager {

    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_RESERVAS = "notif_reservas"

    fun isReservasEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RESERVAS, true)

    fun setReservasEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_RESERVAS, enabled).apply()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
