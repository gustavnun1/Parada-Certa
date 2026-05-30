package com.example.paradacerta.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.receivers.ReservationAlarmReceiver
import java.util.Calendar

object ReservationNotificationScheduler {

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    private const val RC_60MIN = 2001
    private const val RC_30MIN = 2002
    private const val RC_10MIN = 2003
    private const val RC_NOW = 2004

    private const val PREFS_BOOT = "reservation_alarm_prefs"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun shouldRequestExactAlarmPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact(context)
    }

    fun schedule(context: Context, sessao: SessaoAtiva): Boolean {
        val reservationTime = sessao.inicioReservaPrevisto
            ?: sessao.horarioReserva?.let { parseReservationTime(it) }
            ?: return false
        val nome = sessao.estacionamentoNome
        val exactAllowed = canScheduleExact(context)

        val scheduledAny = listOf(
            scheduleAlarm(
                context,
                RC_60MIN,
                reservationTime - 60 * 60_000L,
                "Reserva em 1 hora",
                "Sua reserva em $nome comeca em 1 hora",
                20,
                exactAllowed
            ),
            scheduleAlarm(
                context,
                RC_30MIN,
                reservationTime - 30 * 60_000L,
                "Reserva em 30 minutos",
                "Sua reserva em $nome comeca em 30 minutos",
                21,
                exactAllowed
            ),
            scheduleAlarm(
                context,
                RC_10MIN,
                reservationTime - 10 * 60_000L,
                "Reserva em 10 minutos",
                "Sua reserva em $nome comeca em 10 minutos",
                22,
                exactAllowed
            ),
            scheduleAlarm(
                context,
                RC_NOW,
                reservationTime,
                "Hora da sua reserva!",
                "E hora da sua reserva em $nome",
                23,
                exactAllowed
            )
        ).any { it }

        if (scheduledAny) {
            context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE).edit()
                .putString("nome", nome)
                .putLong("reservation_time", reservationTime)
                .apply()
        }
        return scheduledAny
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(RC_60MIN, RC_30MIN, RC_10MIN, RC_NOW).forEach { rc ->
            val intent = Intent(context, ReservationAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
        context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun rescheduleAfterBoot(context: Context, nome: String, reservationTime: Long) {
        val exactAllowed = canScheduleExact(context)
        scheduleAlarm(
            context,
            RC_60MIN,
            reservationTime - 60 * 60_000L,
            "Reserva em 1 hora",
            "Sua reserva em $nome comeca em 1 hora",
            20,
            exactAllowed
        )
        scheduleAlarm(
            context,
            RC_30MIN,
            reservationTime - 30 * 60_000L,
            "Reserva em 30 minutos",
            "Sua reserva em $nome comeca em 30 minutos",
            21,
            exactAllowed
        )
        scheduleAlarm(
            context,
            RC_10MIN,
            reservationTime - 10 * 60_000L,
            "Reserva em 10 minutos",
            "Sua reserva em $nome comeca em 10 minutos",
            22,
            exactAllowed
        )
        scheduleAlarm(
            context,
            RC_NOW,
            reservationTime,
            "Hora da sua reserva!",
            "E hora da sua reserva em $nome",
            23,
            exactAllowed
        )
    }

    private fun scheduleAlarm(
        context: Context,
        requestCode: Int,
        triggerAt: Long,
        title: String,
        message: String,
        notifId: Int,
        exactAllowed: Boolean = canScheduleExact(context)
    ): Boolean {
        if (triggerAt <= System.currentTimeMillis()) return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReservationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            when {
                exactAllowed -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        return true
    }

    private fun parseReservationTime(horario: String): Long? {
        val parts = horario.split(":").takeIf { it.size >= 2 } ?: return null
        val hora = parts[0].toIntOrNull() ?: return null
        val minuto = parts[1].toIntOrNull() ?: return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
