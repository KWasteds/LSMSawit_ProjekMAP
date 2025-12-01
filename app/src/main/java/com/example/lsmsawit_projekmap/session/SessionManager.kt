package com.example.lsmsawit_projekmap.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIME = "login_time"

        private const val SESSION_DURATION = 30 * 60 * 1000L // 30 menit
    }

    fun createLoginSession(userId: String, role: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun isSessionValid(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!loggedIn) return false

        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        return (currentTime - loginTime) <= SESSION_DURATION
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getUserRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
}
