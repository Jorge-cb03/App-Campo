package com.example.proyecto.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.proyecto.data.database.appContext

actual object NotificationManager {
    actual fun scheduleNotification(title: String, message: String, epochSeconds: Long) {
        val intent = Intent(appContext, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }

        // MEJORA: Generamos un ID único combinando el hash del título y el tiempo
        // para que dos alarmas a la misma hora pero con distinto título no se pisen.
        val notificationId = (title.hashCode() + epochSeconds.toInt())

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId, // ID único para que Android registre alarmas separadas
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = appContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else true

        if (canScheduleExact) {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                epochSeconds,
                pendingIntent
            )
        } else {
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                epochSeconds,
                pendingIntent
            )
        }
    }
}