package com.ayush.netpulse.data.model

import com.ayush.netpulse.util.NetworkUtils
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
    val hasNotificationPermission: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val deviceManufacturer: String = "",
    val deviceModel: String = ""
) {
    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
