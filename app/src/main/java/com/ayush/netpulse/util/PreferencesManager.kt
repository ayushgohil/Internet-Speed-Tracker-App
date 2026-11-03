package com.ayush.netpulse.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var lastAttemptedNotificationTimestamp: Long
        get() = prefs.getLong(KEY_LAST_ATTEMPTED_NOTIFICATION_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ATTEMPTED_NOTIFICATION_TIMESTAMP, value).apply()

    var lastDisplayedSpeedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_DISPLAYED_SPEED_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_DISPLAYED_SPEED_TIMESTAMP, value).apply()

    var lastNotificationException: String?
        get() = prefs.getString(KEY_LAST_NOTIFICATION_EXCEPTION, null)
        set(value) = prefs.edit().putString(KEY_LAST_NOTIFICATION_EXCEPTION, value).apply()

    var lastServiceDestroyTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SERVICE_DESTROY_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SERVICE_DESTROY_TIMESTAMP, value).apply()

    var serviceRestartCount: Int
        get() = prefs.getInt(KEY_SERVICE_RESTART_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_SERVICE_RESTART_COUNT, value).apply()

    var lastKnownServiceState: String
        get() = prefs.getString(KEY_LAST_KNOWN_SERVICE_STATE, "Stopped") ?: "Stopped"
        set(value) = prefs.edit().putString(KEY_LAST_KNOWN_SERVICE_STATE, value).apply()

    /** Consecutive blocked restart attempts, used to back off retries. Reset when the service starts. */
    var revivalRetryCount: Int
        get() = prefs.getInt(KEY_REVIVAL_RETRY_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_REVIVAL_RETRY_COUNT, value).apply()

    /** Last few restart attempts (source and outcome), kept across app launches for diagnostics. */
    var revivalLog: String
        get() = prefs.getString(KEY_REVIVAL_LOG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REVIVAL_LOG, value).apply()

    @Synchronized
    fun appendRevivalLog(source: String, outcome: String) {
        val stamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val lines = revivalLog.lines().filter { it.isNotBlank() }.toMutableList()
        lines.add("$stamp  $source: $outcome")
        revivalLog = lines.takeLast(MAX_REVIVAL_LOG_LINES).joinToString("\n")
    }

    var showSpeedInStatusBar: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SPEED_IN_STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SPEED_IN_STATUS_BAR, value).apply()

    /** Whether the live speed notification may show its details on the lock screen. Off by default. */
    var showOnLockScreen: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ON_LOCK_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ON_LOCK_SCREEN, value).apply()

    /** Monthly mobile data limit in bytes; 0 means no limit / alerts off. */
    var dataLimitBytes: Long
        get() = prefs.getLong(KEY_DATA_LIMIT_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_DATA_LIMIT_BYTES, value).apply()

    /** Day of the month (1-28) on which the data plan renews. */
    var dataLimitCycleDay: Int
        get() = prefs.getInt(KEY_DATA_LIMIT_CYCLE_DAY, 1)
        set(value) = prefs.edit().putInt(KEY_DATA_LIMIT_CYCLE_DAY, value).apply()

    /** Mobile bytes used since the current billing cycle started (refreshed with today's usage). */
    var cachedCycleMobileBytes: Long
        get() = prefs.getLong(KEY_CACHED_CYCLE_MOBILE_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_CACHED_CYCLE_MOBILE_BYTES, value).apply()

    /** "cycleStartMillis:level" of the last alert sent, so each alert fires once per cycle. */
    var dataLimitAlertState: String
        get() = prefs.getString(KEY_DATA_LIMIT_ALERT_STATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DATA_LIMIT_ALERT_STATE, value).apply()

    var speedUnitPreference: String
        get() = prefs.getString(KEY_SPEED_UNIT_PREFERENCE, "Auto") ?: "Auto"
        set(value) = prefs.edit().putString(KEY_SPEED_UNIT_PREFERENCE, value).apply()

    var notificationStyle: String
        get() = prefs.getString(KEY_NOTIFICATION_STYLE, "Minimal") ?: "Minimal"
        set(value) = prefs.edit().putString(KEY_NOTIFICATION_STYLE, value).apply()

    var cachedTodayMobileBytes: Long
        get() = prefs.getLong(KEY_CACHED_TODAY_MOBILE_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_CACHED_TODAY_MOBILE_BYTES, value).apply()

    var cachedTodayWifiBytes: Long
        get() = prefs.getLong(KEY_CACHED_TODAY_WIFI_BYTES, 0L)
        set(value) = prefs.edit().putLong(KEY_CACHED_TODAY_WIFI_BYTES, value).apply()

    var cachedTodayDate: String?
        get() = prefs.getString(KEY_CACHED_TODAY_DATE, null)
        set(value) = prefs.edit().putString(KEY_CACHED_TODAY_DATE, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "Dark") ?: "Dark"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var notificationRefreshIntervalMs: Long
        get() = prefs.getLong(KEY_NOTIFICATION_REFRESH_INTERVAL_MS, 1000L)
        set(value) = prefs.edit().putLong(KEY_NOTIFICATION_REFRESH_INTERVAL_MS, value).apply()

    companion object {
        private const val PREFS_NAME = "netpulse_preferences"
        private const val KEY_APP_THEME = "key_app_theme"
        private const val KEY_NOTIFICATION_REFRESH_INTERVAL_MS = "key_notification_refresh_interval_ms"
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
        private const val KEY_LAST_ATTEMPTED_NOTIFICATION_TIMESTAMP = "key_last_attempted_notification_timestamp"
        private const val KEY_LAST_DISPLAYED_SPEED_TIMESTAMP = "key_last_displayed_speed_timestamp"
        private const val KEY_LAST_NOTIFICATION_EXCEPTION = "key_last_notification_exception"
        private const val KEY_LAST_SERVICE_DESTROY_TIMESTAMP = "key_last_service_destroy_timestamp"
        private const val KEY_SERVICE_RESTART_COUNT = "key_service_restart_count"
        private const val KEY_LAST_KNOWN_SERVICE_STATE = "key_last_known_service_state"
        private const val KEY_SHOW_SPEED_IN_STATUS_BAR = "key_show_speed_in_status_bar"
        private const val KEY_SHOW_ON_LOCK_SCREEN = "key_show_on_lock_screen"
        private const val KEY_DATA_LIMIT_BYTES = "key_data_limit_bytes"
        private const val KEY_DATA_LIMIT_CYCLE_DAY = "key_data_limit_cycle_day"
        private const val KEY_CACHED_CYCLE_MOBILE_BYTES = "key_cached_cycle_mobile_bytes"
        private const val KEY_DATA_LIMIT_ALERT_STATE = "key_data_limit_alert_state"
        private const val KEY_SPEED_UNIT_PREFERENCE = "key_speed_unit_preference"
        private const val KEY_NOTIFICATION_STYLE = "key_notification_style"
        private const val KEY_CACHED_TODAY_MOBILE_BYTES = "key_cached_today_mobile_bytes"
        private const val KEY_CACHED_TODAY_WIFI_BYTES = "key_cached_today_wifi_bytes"
        private const val KEY_CACHED_TODAY_DATE = "key_cached_today_date"
        private const val KEY_REVIVAL_RETRY_COUNT = "key_revival_retry_count"
        private const val KEY_REVIVAL_LOG = "key_revival_log"
        private const val MAX_REVIVAL_LOG_LINES = 20

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
