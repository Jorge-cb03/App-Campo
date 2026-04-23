package com.example.proyecto.util

import platform.UserNotifications.*
import platform.Foundation.*

actual object NotificationManager {
    actual fun scheduleNotification(title: String, message: String, epochSeconds: Long) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
        }

        val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                    NSCalendarUnitHour or NSCalendarUnitMinute,
            date
        )

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = epochSeconds.toString(),
            content = content,
            trigger = trigger
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { _ -> }
    }
}