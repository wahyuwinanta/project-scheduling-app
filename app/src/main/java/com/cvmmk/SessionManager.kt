package com.cvmmk

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveSession(user: User) {
        try {
            with(sharedPreferences.edit()) {
                putString("username", user.username)
                putString("role", user.role)
                putInt("user_id", user.id)
                user.workerId?.let { putInt("worker_id", it) }
                putBoolean("is_logged_in", true)
                apply()
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error saving session: ${e.message}")
        }
    }

    fun isLoggedIn(): Boolean {
        return try {
            sharedPreferences.getBoolean("is_logged_in", false) &&
                    !sharedPreferences.getString("username", "").isNullOrEmpty()
        } catch (e: Exception) {
            Log.e("SessionManager", "Error checking login status: ${e.message}")
            false
        }
    }

    fun getLoggedInUser(): User? {
        return try {
            if (!isLoggedIn()) {
                Log.d("SessionManager", "Not logged in")
                return null
            }

            val username = sharedPreferences.getString("username", "") ?: return null
            val role = sharedPreferences.getString("role", "") ?: return null
            val userId = sharedPreferences.getInt("user_id", -1)
            val workerId = if (sharedPreferences.contains("worker_id")) {
                sharedPreferences.getInt("worker_id", -1)
            } else null

            if (username.isEmpty() || role.isEmpty() || userId == -1) {
                Log.d("SessionManager", "Invalid session data: username=$username, role=$role, userId=$userId")
                return null
            }

            Log.d("SessionManager", "Returning user: $username, role=$role, id=$userId")
            User(userId, username, role, workerId)
        } catch (e: Exception) {
            Log.e("SessionManager", "Error getting user: ${e.message}")
            null
        }
    }

    fun clearSession() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("SessionManager", "Error clearing session: ${e.message}")
        }
    }
}