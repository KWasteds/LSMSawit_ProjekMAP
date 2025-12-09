package com.example.lsmsawit_projekmap.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "LSM_PREF"
        private const val KEY_IS_LOGIN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_ROLE = "userRole"
        private const val KEY_LAST_LOGIN = "lastLogin"
        private const val SESSION_DURATION = 60 * 60 * 1000  // 1 jam
        private const val TAG = "SessionCheck" // Tag untuk Logcat
    }

    private val pref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val editor: SharedPreferences.Editor = pref.edit()

    fun createLoginSession(uid: String, role: String) {
        editor.putBoolean(KEY_IS_LOGIN, true)
        editor.putString(KEY_USER_ID, uid)
        editor.putString(KEY_USER_ROLE, role)
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
        editor.apply() // Gunakan commit() untuk debugging agar yakin tersimpan sinkron
        // editor.commit()

        Log.d(TAG, "🔥 SESSION DIBUAT: UID=$uid, Role=$role, Waktu=${System.currentTimeMillis()}")
    }

    fun refreshSession() {
        if (isSessionValid()) {
            val oldTime = pref.getLong(KEY_LAST_LOGIN, 0L)
            val newTime = System.currentTimeMillis()
            editor.putLong(KEY_LAST_LOGIN, newTime)
            editor.apply()

            Log.d(TAG, "♻️ SESSION DIPERPANJANG: Lama=$oldTime, Baru=$newTime")
        } else {
            Log.e(TAG, "❌ Gagal Perpanjang: Session sudah tidak valid/kadaluarsa")
        }
    }

    fun isSessionValid(): Boolean {
        val isLogin = pref.getBoolean(KEY_IS_LOGIN, false)
        val last = pref.getLong(KEY_LAST_LOGIN, 0L)
        val now = System.currentTimeMillis()
        val diff = now - last
        val isValidDuration = diff <= SESSION_DURATION

        Log.d(TAG, "🔍 CEK SESSION: IsLogin=$isLogin, Diff=$diff ms, ValidDuration=$isValidDuration")

        if (!isLogin) return false
        return isValidDuration
    }

    fun logout() {
        editor.clear()
        editor.apply()
        Log.d(TAG, "👋 SESSION DIHAPUS (Logout)")
    }

    // Getter helpers
    fun getUserId(): String? = pref.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = pref.getString(KEY_USER_ROLE, null)
}