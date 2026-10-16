package com.ayush.netpulse.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isBackgroundMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_MONITORING, value).apply()

    var lastReconciledDate: String?
        get() = prefs.getString(KEY_LAST_RECONCILED_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_RECONCILED_DATE, value).apply()

    var lastReconciledTimestamp: Long
        get() = prefs.getLong(KEY_LAST_RECONCILED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_RECONCILED_TIMESTAMP, value).apply()

    var lastReconciliationStatus: String?
        get() = prefs.getString(KEY_LAST_RECONCILIATION_STATUS, "Not yet executed")
        set(value) = prefs.edit().putString(KEY_LAST_RECONCILIATION_STATUS, value).apply()

    var lastServiceStartTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SERVICE_START_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SERVICE_START_TIMESTAMP, value).apply()

    var lastServiceStopReason: String?
        get() = prefs.getString(KEY_LAST_SERVICE_STOP_REASON, "Never stopped")
        set(value) = prefs.edit().putString(KEY_LAST_SERVICE_STOP_REASON, value).apply()

    var lastSpeedSampleTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SPEED_SAMPLE_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SPEED_SAMPLE_TIMESTAMP, value).apply()

    var logRetentionDays: Int
        get() = prefs.getInt(KEY_LOG_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_LOG_RETENTION_DAYS, value).apply()

    var lastKnownError: String?
        get() = prefs.getString(KEY_LAST_KNOWN_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_KNOWN_ERROR, value).apply()

    var lastCleanupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CLEANUP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CLEANUP_TIMESTAMP, value).apply()

    var lastNotificationUpdateTimestamp: Long
        get() = prefs.getLong(KEY_LAST_NOTIFICATION_UPDATE_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFICATION_UPDATE_TIMESTAMP, value).apply()

    var lastServiceDestroyTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SERVICE_DESTROY_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SERVICE_DESTROY_TIMESTAMP, value).apply()

    var serviceRestartCount: Int
        get() = prefs.getInt(KEY_SERVICE_RESTART_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_SERVICE_RESTART_COUNT, value).apply()

    var lastKnownServiceState: String
        get() = prefs.getString(KEY_LAST_KNOWN_SERVICE_STATE, "Stopped") ?: "Stopped"
        set(value) = prefs.edit().putString(KEY_LAST_KNOWN_SERVICE_STATE, value).apply()

    companion object {
        private const val PREFS_NAME = "netpulse_preferences"
        private const val KEY_BACKGROUND_MONITORING = "key_background_monitoring"
        private const val KEY_LAST_RECONCILED_DATE = "key_last_reconciled_date"
        private const val KEY_LAST_RECONCILED_TIMESTAMP = "key_last_reconciled_timestamp"
        private const val KEY_LAST_RECONCILIATION_STATUS = "key_last_reconciliation_status"
        private const val KEY_LAST_SERVICE_START_TIMESTAMP = "key_last_service_start_timestamp"
        private const val KEY_LAST_SERVICE_STOP_REASON = "key_last_service_stop_reason"
        private const val KEY_LAST_SPEED_SAMPLE_TIMESTAMP = "key_last_speed_sample_timestamp"
        private const val KEY_LOG_RETENTION_DAYS = "key_log_retention_days"
        private const val KEY_LAST_KNOWN_ERROR = "key_last_known_error"
        private const val KEY_LAST_CLEANUP_TIMESTAMP = "key_last_cleanup_timestamp"
        private const val KEY_LAST_NOTIFICATION_UPDATE_TIMESTAMP = "key_last_notification_update_timestamp"
        private const val KEY_LAST_SERVICE_DESTROY_TIMESTAMP = "key_last_service_destroy_timestamp"
        private const val KEY_SERVICE_RESTART_COUNT = "key_service_restart_count"
        private const val KEY_LAST_KNOWN_SERVICE_STATE = "key_last_known_service_state"

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
