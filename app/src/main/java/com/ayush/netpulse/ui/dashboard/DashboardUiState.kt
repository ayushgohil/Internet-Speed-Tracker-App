package com.ayush.netpulse.ui.dashboard

import com.ayush.netpulse.data.local.SpeedRecordEntity
import com.ayush.netpulse.data.model.DataUsage
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType

data class DashboardUiState(
    val isMonitoringActive: Boolean = true,
    val hasUsageAccessPermission: Boolean = false,
    val networkType: NetworkType = NetworkType.DISCONNECTED,
    val liveSpeed: LiveSpeed = LiveSpeed(),
    val dailyUsage: DataUsage = DataUsage(),
    val recentRecords: List<SpeedRecordEntity> = emptyList(),
    val isLoadingUsage: Boolean = false,
    val usageErrorMessage: String? = null
)
