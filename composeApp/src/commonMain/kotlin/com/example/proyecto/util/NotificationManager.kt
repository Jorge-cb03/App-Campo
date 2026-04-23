package com.example.proyecto.util

expect object NotificationManager {
    fun scheduleNotification(title: String, message: String, epochSeconds: Long)
}