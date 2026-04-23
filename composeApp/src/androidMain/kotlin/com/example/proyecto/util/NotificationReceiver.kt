package com.example.proyecto.util

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Alerta de Huerta"
        val message = intent.getStringExtra("message") ?: ""
        val channelId = "huerto_alerts_local"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas locales",
                AndroidNotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // MEJORA: Generamos un ID único combinando el hash del título y el mensaje
        // para que si hay dos avisos distintos a la misma hora, ambos se muestren.
        val notificationId = (title.hashCode() + message.hashCode() + System.currentTimeMillis().toInt())

        manager.notify(notificationId, builder.build())
    }
}