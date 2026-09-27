package com.example.data.session

import android.content.Context
import android.content.SharedPreferences

class SecureCredentialStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ruijie_secure_prefs", Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String = ""): String {
        return prefs.getString(key, defValue) ?: defValue
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
