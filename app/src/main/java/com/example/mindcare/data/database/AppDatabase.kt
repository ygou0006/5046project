package com.example.mindcare.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mindcare.data.models.DiaryEntry
import com.example.mindcare.data.models.User

@Database(
    entities = [User::class, DiaryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun diaryDao(): DiaryDao
}