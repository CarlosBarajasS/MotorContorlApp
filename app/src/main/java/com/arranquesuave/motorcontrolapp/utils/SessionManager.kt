package com.arranquesuave.motorcontrolapp.utils

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "key_token"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
