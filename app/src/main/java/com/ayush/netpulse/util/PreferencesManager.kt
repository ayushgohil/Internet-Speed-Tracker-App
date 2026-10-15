package com.ayush.netpulse.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isBackgroundMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_MONITORING, value).apply()

    companion object {
        private const val PREFS_NAME = "netpulse_preferences"
        private const val KEY_BACKGROUND_MONITORING = "key_background_monitoring"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
