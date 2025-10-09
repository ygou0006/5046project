package com.example.mindcare.data.database

import androidx.room.*
import com.example.mindcare.data.models.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntriesByUser(userId: String): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: DiaryEntry)

    @Update
    fun updateEntry(entry: DiaryEntry)

    @Delete
    fun deleteEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getEntriesBetweenDates(userId: String, startDate: Long, endDate: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    fun getTodayEntry(userId: String, startOfDay: Long, endOfDay: Long): DiaryEntry?

    @Query("UPDATE diary_entries SET mood = :mood WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay")
    fun updateTodayMood(userId: String, mood: String, startOfDay: Long, endOfDay: Long)
}