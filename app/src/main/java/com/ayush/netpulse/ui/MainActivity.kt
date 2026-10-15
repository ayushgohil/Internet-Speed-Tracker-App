package com.ayush.netpulse.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.ui.dashboard.DashboardScreen
import com.ayush.netpulse.ui.dashboard.DashboardViewModel
import com.ayush.netpulse.ui.theme.NetPulseTheme
import com.ayush.netpulse.util.NetworkUtils

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.provideFactory(application as NetPulseApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NetPulseTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                DashboardScreen(
                    uiState = uiState,
                    onToggleMonitoring = { viewModel.toggleMonitoring() },
                    onRequestUsageAccess = {
                        startActivity(NetworkUtils.createUsageAccessSettingsIntent())
                    },
                    onRefreshUsage = { viewModel.loadDailyDataUsage() }
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
