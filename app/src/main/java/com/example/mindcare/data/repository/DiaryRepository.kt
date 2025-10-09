package com.example.mindcare.data.repository

import com.example.mindcare.data.database.DiaryDao
import com.example.mindcare.data.models.DiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao
) {

    fun getEntriesByUser(userId: String): Flow<List<DiaryEntry>> {
        return diaryDao.getEntriesByUser(userId)
    }

    suspend fun insertEntry(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        diaryDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        diaryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        diaryDao.deleteEntry(entry)
    }

    fun getEntriesBetweenDates(userId: String, startDate: Long, endDate: Long): Flow<List<DiaryEntry>> {
        return diaryDao.getEntriesBetweenDates(userId, startDate, endDate)
    }

    suspend fun getTodayEntry(userId: String): DiaryEntry? = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        diaryDao.getTodayEntry(userId, startOfDay, endOfDay)
    }

    suspend fun saveOrUpdateTodayMood(userId: String, mood: String) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        // First, check if there are any records for today
        val todayEntry = diaryDao.getTodayEntry(userId, startOfDay, endOfDay)

        if (todayEntry != null) {
            // Update existing records
            val updatedEntry = todayEntry.copy(mood = mood)
            diaryDao.updateEntry(updatedEntry)
        } else {
            // Create a new record
            val newEntry = DiaryEntry(
                userId = userId,
                date = System.currentTimeMillis(),
                mood = mood,
                note = ""
            )
            diaryDao.insertEntry(newEntry)
        }
    }
}