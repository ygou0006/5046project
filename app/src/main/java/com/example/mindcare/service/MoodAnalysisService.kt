package com.example.mindcare.service

import com.example.mindcare.data.repository.DiaryRepository
import com.example.mindcare.utils.SessionManager
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodAnalysisService @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager
) {

    suspend fun checkNegativeMoodTrend(): Int {
        val currentUser = sessionManager.getUser() ?: return 0
        val entries = diaryRepository.getEntriesByUser(currentUser.id).first()

        if (entries.isEmpty()) return 0

        val negativeMoods = listOf("Sad", "Anxious", "Angry")
        var consecutiveNegativeDays = 0

        // Check your emotions in the past 3 days
        for (i in 0..2) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val startOfDay = getStartOfDay(calendar.timeInMillis)
            val endOfDay = getEndOfDay(calendar.timeInMillis)

            val dayEntry = entries.find { entry ->
                entry.date in startOfDay..endOfDay
            }

            if (dayEntry != null && dayEntry.mood in negativeMoods) {
                consecutiveNegativeDays++
            } else {
                break // Stop counting once encountering non negative emotions
            }
        }

        return consecutiveNegativeDays
    }

    private fun getStartOfDay(timeInMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timeInMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}