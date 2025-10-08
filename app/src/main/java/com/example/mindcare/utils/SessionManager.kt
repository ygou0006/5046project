package com.example.mindcare.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.mindcare.data.models.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER = "user"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(user: User) {
        try {
            val gson = Gson()
            val jsonString = gson.toJson(user)

            val encodedString = Base64.encodeToString(
                jsonString.toByteArray(charset("UTF-8")),
                Base64.NO_WRAP
            )

            prefs.edit().putString(KEY_USER, encodedString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUser(): User? {
        return try {
            val encodedString = prefs.getString(KEY_USER, null) ?: return null

            val decodedBytes = Base64.decode(encodedString, Base64.NO_WRAP)
            val jsonString = String(decodedBytes, charset("UTF-8"))

            Gson().fromJson(jsonString, User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveConfig(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getConfig(key: String): String? = prefs.getString(key, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getUser() != null
}