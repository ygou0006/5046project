package com.example.mindcare.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mindcare.MainActivity
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val context: Context
) {
    companion object {
        const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder"
        const val MOOD_TREND_CHANNEL_ID = "mood_trend"
        const val DAILY_REMINDER_REQUEST_CODE = 1001
        const val MOOD_TREND_REQUEST_CODE = 1002
        const val DAILY_REMINDER_ACTION = "DAILY_REMINDER_ACTION"
        const val MOOD_TREND_ACTION = "MOOD_TREND_ACTION"

        const val OPEN_APP_REQUEST_CODE = 1003
    }

    private var notificationsEnabled = true

    private fun getOpenAppPendingIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
        } ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
        }

        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun showDailyReminder() {
        if (!notificationsEnabled || !NotificationPermissionHelper.hasNotificationPermission(context)) {
            return
        }

        val notification = NotificationCompat.Builder(context, DAILY_REMINDER_CHANNEL_ID)
            .setContentTitle("MindCare Daily Check-in")
            .setContentText("How are you feeling today? Take a moment to record your mood.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getOpenAppPendingIntent())
            .build()

        NotificationManagerCompat.from(context).notify(DAILY_REMINDER_REQUEST_CODE, notification)
    }

    @SuppressLint("MissingPermission")
    fun showMoodTrendNotification(negativeDays: Int) {
        if (!notificationsEnabled || !NotificationPermissionHelper.hasNotificationPermission(context)) {
            return
        }

        val messages = listOf(
            "We noticed you've been feeling down recently. Remember, it's okay to not be okay. 💙",
            "You've had some tough days. Be kind to yourself today. 🌟",
            "Your mood tracker shows some challenges. Consider trying a breathing exercise. 🧘‍♀️",
            "It's been a rough patch. Remember, this too shall pass. 🌈"
        )

        val randomMessage = messages.random()

        val notification = NotificationCompat.Builder(context, MOOD_TREND_CHANNEL_ID)
            .setContentTitle("MindCare Check-in")
            .setContentText(randomMessage)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getOpenAppPendingIntent())
            .build()

        NotificationManagerCompat.from(context).notify(MOOD_TREND_REQUEST_CODE, notification)
    }

    fun scheduleDailyReminder() {
        if (!notificationsEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = DAILY_REMINDER_ACTION
            putExtra("type", "daily_reminder")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set daily reminder at 7 pm
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 19) // 7 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // If it's already past 7 o'clock, set it as tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // Repeat every day
            pendingIntent
        )
    }

    fun cancelDailyReminder() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = DAILY_REMINDER_ACTION
                putExtra("type", "daily_reminder")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelMoodTrendCheck() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = MOOD_TREND_ACTION
                putExtra("type", "mood_trend_check")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MOOD_TREND_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAllNotifications() {
        try {
            notificationsEnabled = false

            cancelDailyReminder()
            cancelMoodTrendCheck()

            // Clear all app notifications
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun enableAllNotifications() {
        notificationsEnabled = true
        scheduleDailyReminder()
        scheduleMoodTrendCheck()
    }

    fun scheduleMoodTrendCheck() {
        if (!notificationsEnabled) return

        // Check emotional trends every 12 hours
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = MOOD_TREND_ACTION
            putExtra("type", "mood_trend_check")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MOOD_TREND_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (30 * 1000) // The test lasts for 30 seconds, but in reality, it should be AlarmManager.INTERVAL_HALF_DAY

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}

object NotificationPermissionHelper {
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }
}