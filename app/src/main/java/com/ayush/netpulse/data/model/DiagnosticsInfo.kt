package com.ayush.netpulse.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticsInfo(
    val isBackgroundMonitoringEnabled: Boolean = false,
    val isForegroundServiceRunning: Boolean = false,
    val isServiceStatePotentiallyStale: Boolean = false,
    val lastSpeedSampleTimestamp: Long = 0L,
    val lastReconciliationTimestamp: Long = 0L,
    val lastReconciliationStatus: String = "Not yet executed",
    val lastServiceStartTimestamp: Long = 0L,
    val lastServiceStopReason: String = "None",
    val confirmedProcessExitReason: String = "None recorded",
    val lastKnownError: String? = null,
    val logRetentionDays: Int = 30,
    val lastCleanupTimestamp: Long = 0L,
    val lastNotificationUpdateTimestamp: Long = 0L,
    val lastServiceDestroyTimestamp: Long = 0L,
    val serviceRestartCount: Int = 0,
    val hasNotificationPermission: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val deviceManufacturer: String = "",
    val deviceModel: String = ""
) {
    /**
     * Resolves the verified service state using requested standard categories:
     * "Active", "Stopped", "Unknown", or "Needs Permission".
     * Does not assume the service is running merely because monitoring is enabled in prefs.
     */
    val resolvedServiceStatus: String
        get() = when {
            isForegroundServiceRunning -> "Active"
            !hasNotificationPermission -> "Needs Permission"
            !isBackgroundMonitoringEnabled -> "Stopped"
            else -> "Unknown"
        }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
