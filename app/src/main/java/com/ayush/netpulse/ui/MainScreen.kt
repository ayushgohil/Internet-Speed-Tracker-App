package com.ayush.netpulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.ui.dashboard.DashboardTab
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.navigation.NavScreen
import com.ayush.netpulse.ui.settings.SettingsScreen
import com.ayush.netpulse.ui.usage.StickyUsageSummaryBar
import com.ayush.netpulse.ui.usage.UsageTab

@Composable
fun MainScreen(
    uiState: DashboardUiState,
    onToggleMonitoring: () -> Unit,
    onToggleBackgroundService: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRefreshUsage: () -> Unit,
    onClearLogs: () -> Unit,
    onSetLogRetentionDays: (Int) -> Unit = {},
    onToggleSpeedInStatusBar: () -> Unit = {},
    onSetSpeedUnit: (String) -> Unit = {},
    onSetNotificationStyle: (String) -> Unit = {},
    onSetNotificationRefreshInterval: (Long) -> Unit = {},
    onSetAppTheme: (String) -> Unit = {},
    onReconcileUsage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable { mutableStateOf(NavScreen.DASHBOARD) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var openSettingsDirectlyToLogs by rememberSaveable { mutableStateOf(false) }

    // Preserve Usage tab scroll position across tab switching
    val usageLazyListState = rememberLazyListState()

    // Aggregate 90-day totals for sticky usage summary (recomputed only when DB records change)
    val usageSummary = remember(uiState.recentDailyRecords) {
        var mobile = 0L
        var wifi = 0L
        var recorded = 0
        uiState.recentDailyRecords.forEach {
            mobile += it.totalMobileBytes
            wifi += it.totalWifiBytes
            if (it.totalAllBytes > 0) recorded++
        }
        Triple(mobile, wifi, recorded)
    }

    if (isSettingsOpen) {
        BackHandler {
            isSettingsOpen = false
            openSettingsDirectlyToLogs = false
        }
        SettingsScreen(
            uiState = uiState,
            onBack = {
                isSettingsOpen = false
                openSettingsDirectlyToLogs = false
            },
            onToggleBackgroundMonitoring = onToggleBackgroundService,
            onToggleSpeedInStatusBar = onToggleSpeedInStatusBar,
            onSetSpeedUnit = onSetSpeedUnit,
            onSetNotificationStyle = onSetNotificationStyle,
            onSetNotificationRefreshInterval = onSetNotificationRefreshInterval,
            onSetAppTheme = onSetAppTheme,
            onRequestUsageAccess = onRequestUsageAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onSetRetentionDays = onSetLogRetentionDays,
            onClearLogs = onClearLogs,
            onReconcileUsage = onReconcileUsage,
            initialOpenSpeedLogs = openSettingsDirectlyToLogs,
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    // Sticky Usage Summary: Permanently fixed directly above bottom navigation on Usage screen
                    if (currentScreen == NavScreen.USAGE && uiState.hasUsageAccessPermission) {
                        StickyUsageSummaryBar(
                            totalMobileBytes = usageSummary.first,
                            totalWifiBytes = usageSummary.second,
                            totalCombinedBytes = usageSummary.first + usageSummary.second,
                            daysCount = 90,
                            recordedDaysCount = usageSummary.third
                        )
                    }

                    // Bottom Navigation: Clean 2-tab navigation (Dashboard | Usage)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavScreen.values().forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            when (currentScreen) {
                NavScreen.DASHBOARD -> {
                    DashboardTab(
                        uiState = uiState,
                        onToggleMonitoring = onToggleMonitoring,
                        onToggleBackgroundService = onToggleBackgroundService,
                        onNavigateToUsage = { currentScreen = NavScreen.USAGE },
                        onNavigateToLogs = {
                            openSettingsDirectlyToLogs = true
                            isSettingsOpen = true
                        },
                        onOpenSettings = {
                            openSettingsDirectlyToLogs = false
                            isSettingsOpen = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                NavScreen.USAGE -> {
                    UsageTab(
                        recentDailyRecords = uiState.recentDailyRecords,
                        hasUsageAccessPermission = uiState.hasUsageAccessPermission,
                        isLoadingUsage = uiState.isLoadingUsage,
                        isReconcilingUsage = uiState.isReconcilingUsage,
                        onRequestUsageAccess = onRequestUsageAccess,
                        onRefreshUsage = onRefreshUsage,
                        lazyListState = usageLazyListState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
