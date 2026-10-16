package com.ayush.netpulse.ui.dashboard

import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.data.local.SpeedRecordEntity
import com.ayush.netpulse.data.model.DataUsage
import com.ayush.netpulse.data.model.DiagnosticsInfo
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType

data class DashboardUiState(
    val isMonitoringActive: Boolean = true,
    val isBackgroundServiceRunning: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = false,
    val networkType: NetworkType = NetworkType.DISCONNECTED,
    val liveSpeed: LiveSpeed = LiveSpeed(),
    val dailyUsage: DataUsage = DataUsage(),
    val recentDailyRecords: List<DailyUsageEntity> = emptyList(),
    val recentRecords: List<SpeedRecordEntity> = emptyList(),
    val diagnosticsInfo: DiagnosticsInfo = DiagnosticsInfo(),
    val isLoadingUsage: Boolean = false,
    val usageErrorMessage: String? = null,
    val showSpeedInStatusBar: Boolean = false,
    val speedUnitPreference: String = "Auto",
    val notificationStyle: String = "Minimal",
    val isReconcilingUsage: Boolean = false
)
