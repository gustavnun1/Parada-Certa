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
    private const val RC_NOW   = 2004

    private const val PREFS_BOOT = "reservation_alarm_prefs"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, sessao: SessaoAtiva): Boolean {
        val horario = sessao.horarioReserva ?: return false
        val reservationTime = parseReservationTime(horario) ?: return false
        if (!canScheduleExact(context)) return false
        val nome = sessao.estacionamentoNome

        scheduleAlarm(context, RC_60MIN, reservationTime - 60 * 60_000L,
            "Reserva em 1 hora", "Sua reserva em $nome começa em 1 hora", 20)
        scheduleAlarm(context, RC_30MIN, reservationTime - 30 * 60_000L,
            "Reserva em 30 minutos", "Sua reserva em $nome começa em 30 minutos", 21)
        scheduleAlarm(context, RC_10MIN, reservationTime - 10 * 60_000L,
            "Reserva em 10 minutos", "Sua reserva em $nome começa em 10 minutos", 22)
        scheduleAlarm(context, RC_NOW,   reservationTime,
            "Hora da sua reserva!", "É hora da sua reserva em $nome", 23)

        context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE).edit()
            .putString("nome", nome)
            .putLong("reservation_time", reservationTime)
            .apply()
        return true
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(RC_60MIN, RC_30MIN, RC_10MIN, RC_NOW).forEach { rc ->
            val intent = Intent(context, ReservationAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, rc, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
        context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun rescheduleAfterBoot(context: Context, nome: String, reservationTime: Long) {
        scheduleAlarm(context, RC_60MIN, reservationTime - 60 * 60_000L,
            "Reserva em 1 hora", "Sua reserva em $nome começa em 1 hora", 20)
        scheduleAlarm(context, RC_30MIN, reservationTime - 30 * 60_000L,
            "Reserva em 30 minutos", "Sua reserva em $nome começa em 30 minutos", 21)
        scheduleAlarm(context, RC_10MIN, reservationTime - 10 * 60_000L,
            "Reserva em 10 minutos", "Sua reserva em $nome começa em 10 minutos", 22)
        scheduleAlarm(context, RC_NOW, reservationTime,
            "Hora da sua reserva!", "É hora da sua reserva em $nome", 23)
    }

    private fun scheduleAlarm(
        context: Context, requestCode: Int, triggerAt: Long,
        title: String, message: String, notifId: Int
    ) {
        if (triggerAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReservationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
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
            // Se o horário já passou hoje, agenda para amanhã
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
