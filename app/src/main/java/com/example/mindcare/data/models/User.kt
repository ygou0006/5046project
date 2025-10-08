package com.example.mindcare.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val email: String,
    val password: String, // Hash Password
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null
)