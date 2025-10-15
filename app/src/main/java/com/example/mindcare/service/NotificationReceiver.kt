package com.example.mindcare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var moodAnalysisService: MoodAnalysisService

    override fun onReceive(context: Context, intent: Intent) {
        // Check the action of Intent
        when (intent.action) {
            NotificationService.DAILY_REMINDER_ACTION -> {
                notificationService.showDailyReminder()
                // Reschedule tomorrow's reminder
                notificationService.scheduleDailyReminder()
            }
            NotificationService.MOOD_TREND_ACTION -> {
                // Using runBlocking to call a pending function in a broadcast receiver
                runBlocking {
                    val negativeDays = moodAnalysisService.checkNegativeMoodTrend()
                    if (negativeDays >= 2) { // Negative emotions for 2 consecutive days
                        notificationService.showMoodTrendNotification(negativeDays)
                    }
                }
                // Arrange the next inspection again
                notificationService.scheduleMoodTrendCheck()
            }
        }
    }
}