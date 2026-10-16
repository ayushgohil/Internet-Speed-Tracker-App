package com.ayush.netpulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.ui.dashboard.DashboardTab
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.logs.LogsTab
import com.ayush.netpulse.ui.navigation.NavScreen
import com.ayush.netpulse.ui.settings.SettingsScreen
import com.ayush.netpulse.ui.theme.Cyan400
import com.ayush.netpulse.ui.theme.DarkBackground
import com.ayush.netpulse.ui.theme.DarkSurface
import com.ayush.netpulse.ui.theme.Slate400
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
    onReconcileUsage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable { mutableStateOf(NavScreen.DASHBOARD) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    if (isSettingsOpen) {
        BackHandler { isSettingsOpen = false }
        SettingsScreen(
            uiState = uiState,
            onBack = { isSettingsOpen = false },
            onToggleBackgroundMonitoring = onToggleBackgroundService,
            onToggleSpeedInStatusBar = onToggleSpeedInStatusBar,
            onSetSpeedUnit = onSetSpeedUnit,
            onSetNotificationStyle = onSetNotificationStyle,
            onRequestUsageAccess = onRequestUsageAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onSetRetentionDays = onSetLogRetentionDays,
            onClearLogs = onClearLogs,
            onReconcileUsage = onReconcileUsage,
            modifier = modifier
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
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
                                selectedIconColor = DarkBackground,
                                selectedTextColor = Cyan400,
                                indicatorColor = Cyan400,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
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
                        onNavigateToLogs = { currentScreen = NavScreen.LOGS },
                        onOpenSettings = { isSettingsOpen = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                NavScreen.USAGE -> {
                    UsageTab(
                        uiState = uiState,
                        onRequestUsageAccess = onRequestUsageAccess,
                        onRefreshUsage = onRefreshUsage,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                NavScreen.LOGS -> {
                    LogsTab(
                        records = uiState.recentRecords,
                        onClearLogs = onClearLogs,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
