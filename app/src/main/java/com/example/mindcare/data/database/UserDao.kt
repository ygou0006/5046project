package com.example.mindcare.data.database

import androidx.room.*
import com.example.mindcare.data.models.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertUser(user: User)

    @Update
    fun updateUser(user: User)

    @Query("UPDATE users SET lastLoginAt = :lastLoginAt WHERE id = :userId")
    fun updateLastLogin(userId: String, lastLoginAt: Long)

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    fun checkEmailExists(email: String): Int
}