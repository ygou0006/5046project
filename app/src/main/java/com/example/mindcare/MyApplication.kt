package com.example.mindcare

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.mindcare.service.NotificationService
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {
    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        createNotificationChannels()

        if (sessionManager.getConfig("notificationsEnabled") == "TRUE") {
            // Start notification service
            notificationService.enableAllNotifications()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Daily reminder channel
            val dailyChannel = NotificationChannel(
                NotificationService.DAILY_REMINDER_CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to record your daily mood"
            }

            // Emotional Trend Channel
            val trendChannel = NotificationChannel(
                NotificationService.MOOD_TREND_CHANNEL_ID,
                "Mood Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Insights about your mood trends"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(dailyChannel, trendChannel))
        }
    }
}