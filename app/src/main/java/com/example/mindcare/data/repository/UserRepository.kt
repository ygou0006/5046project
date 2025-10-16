package com.example.mindcare.data.repository

import com.example.mindcare.data.database.UserDao
import com.example.mindcare.data.models.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    suspend fun createUser(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (userDao.checkEmailExists(email) > 0) {
                return@withContext Result.failure(Exception("Email already exists"))
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                email = email,
                password = password,
                name = name
            )

            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserFromFirebase(firebaseUser: FirebaseUser, name: String): Result<User> = withContext(Dispatchers.IO) {
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            password = firebaseUser.email ?: "",
            name = name,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        try {
            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun updateLastLogin(userId: String) = withContext(Dispatchers.IO) {
        userDao.updateLastLogin(userId, System.currentTimeMillis())
    }

    suspend fun checkEmailExists(email: String): Boolean = withContext(Dispatchers.IO) {
        userDao.checkEmailExists(email) > 0
    }
}