package com.example.mindcare.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val mood: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)