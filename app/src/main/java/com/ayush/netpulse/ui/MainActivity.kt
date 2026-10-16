package com.ayush.netpulse.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayush.netpulse.NetPulseApp
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
            NetPulseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestBatteryOptimization = {
                        startActivity(NetworkUtils.createIgnoreBatteryOptimizationIntent(this))
                    },
                    onRefreshUsage = { viewModel.loadDailyDataUsage(forceReconcile = true) },
                    onClearLogs = { viewModel.clearLogs() },
                    onSetLogRetentionDays = { days -> viewModel.setLogRetentionDays(days) },
                    onToggleSpeedInStatusBar = { viewModel.toggleShowSpeedInStatusBar() },
                    onSetSpeedUnit = { unit -> viewModel.setSpeedUnitPreference(unit) },
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
