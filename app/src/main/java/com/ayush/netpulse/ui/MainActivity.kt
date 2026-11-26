package com.ayush.netpulse.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.service.SpeedTrackerService
import com.ayush.netpulse.ui.dashboard.DashboardViewModel
import com.ayush.netpulse.ui.theme.NetPulseTheme
import com.ayush.netpulse.util.NetworkUtils

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.provideFactory(application as NetPulseApp)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissionsAndUsage()
        if (isGranted) {
            viewModel.toggleBackgroundService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            NetPulseTheme(appTheme = uiState.appTheme) {
                MainScreen(
                    uiState = uiState,
                    onToggleMonitoring = { viewModel.toggleMonitoring() },
                    onToggleBackgroundService = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !uiState.hasNotificationPermission
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleBackgroundService()
                        }
                    },
                    onRequestUsageAccess = {
                        startActivity(NetworkUtils.createUsageAccessSettingsIntent())
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                        putExtra(Settings.EXTRA_CHANNEL_ID, SpeedTrackerService.activeChannelId(this@MainActivity))
                                    }
                                    startActivity(intent)
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", packageName, null)
                                    }
                                    startActivity(intent)
                                }
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }
                        }
                    },
                    onRequestBatteryOptimization = {
                        startActivity(NetworkUtils.createIgnoreBatteryOptimizationIntent(this))
                    },
                    onRefreshUsage = { viewModel.loadDailyDataUsage(forceReconcile = true) },
                    onClearLogs = { viewModel.clearLogs() },
                    onSetLogRetentionDays = { days -> viewModel.setLogRetentionDays(days) },
                    onToggleSpeedInStatusBar = { viewModel.toggleShowSpeedInStatusBar() },
                    onToggleShowOnLockScreen = { viewModel.toggleShowOnLockScreen() },
                    onSetDataLimitGb = { gb -> viewModel.setDataLimitGb(gb) },
                    onSetDailyDataLimitGb = { gb -> viewModel.setDailyDataLimitGb(gb) },
                    onSetDataLimitCycleDay = { day -> viewModel.setDataLimitCycleDay(day) },
                    onSetSpeedUnit = { unit -> viewModel.setSpeedUnitPreference(unit) },
                    onSetNotificationStyle = { style -> viewModel.setNotificationStylePreference(style) },
                    onSetNotificationRefreshInterval = { interval -> viewModel.setNotificationRefreshInterval(interval) },
                    onSetAppTheme = { theme -> viewModel.setAppThemePreference(theme) },
                    onReconcileUsage = { viewModel.triggerManualReconciliation() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeScreen()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPauseScreen()
    }
}
