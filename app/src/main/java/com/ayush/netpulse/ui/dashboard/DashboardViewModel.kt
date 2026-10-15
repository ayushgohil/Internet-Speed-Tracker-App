package com.ayush.netpulse.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.repository.NetworkRepository
import com.ayush.netpulse.util.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application,
    private val repository: NetworkRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private var recordSaveCounter = 0

    init {
        // Observe network connection type (Wi-Fi, Cellular, etc.)
        viewModelScope.launch {
            repository.observeNetworkType().collect { netType ->
                _uiState.update { it.copy(networkType = netType) }
            }
        }

        // Observe recent Room speed records
        viewModelScope.launch {
            repository.getRecentSpeedRecords(15).collect { records ->
                _uiState.update { it.copy(recentRecords = records) }
            }
        }

        // Initial check for permissions and data usage
        refreshPermissionsAndUsage()

        // Start live speed sampling
        startMonitoring()
    }

    fun refreshPermissionsAndUsage() {
        val context = getApplication<Application>().applicationContext
        val hasPermission = NetworkUtils.hasUsageStatsPermission(context)
        _uiState.update { it.copy(hasUsageAccessPermission = hasPermission) }

        if (hasPermission) {
            loadDailyDataUsage()
        } else {
            _uiState.update {
                it.copy(
                    usageErrorMessage = "Usage Access is required to read daily Wi-Fi and Mobile data."
                )
            }
        }
    }

    fun loadDailyDataUsage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUsage = true, usageErrorMessage = null) }
            val result = repository.getTodayDataUsage()
            result.onSuccess { usage ->
                _uiState.update {
                    it.copy(
                        dailyUsage = usage,
                        isLoadingUsage = false,
                        usageErrorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingUsage = false,
                        usageErrorMessage = err.localizedMessage ?: "Failed to read data usage"
                    )
                }
            }
        }
    }

    fun toggleMonitoring() {
        val willBeActive = !_uiState.value.isMonitoringActive
        if (willBeActive) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    fun onResumeScreen() {
        refreshPermissionsAndUsage()
        if (_uiState.value.isMonitoringActive && (monitoringJob == null || !monitoringJob!!.isActive)) {
            startMonitoring()
        }
    }

    fun onPauseScreen() {
        // When screen is paused in this milestone, pause sampling to conserve battery
        stopMonitoring(userPaused = false)
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        repository.resetSpeedBaseline()
        _uiState.update { it.copy(isMonitoringActive = true) }

        monitoringJob = viewModelScope.launch {
            while (isActive) {
                val speed = repository.sampleCurrentSpeed()
                _uiState.update { it.copy(liveSpeed = speed) }

                // Save to Room every 3 seconds or when active transfer is detected (>10 KB/s)
                recordSaveCounter++
                if (speed.downloadBytesPerSec > 10240 || speed.uploadBytesPerSec > 10240 || recordSaveCounter >= 3) {
                    recordSaveCounter = 0
                    repository.saveSpeedRecord(speed, _uiState.value.networkType)
                }

                delay(1000)
            }
        }
    }

    private fun stopMonitoring(userPaused: Boolean = true) {
        monitoringJob?.cancel()
        monitoringJob = null
        repository.resetSpeedBaseline()
        if (userPaused) {
            _uiState.update {
                it.copy(
                    isMonitoringActive = false,
                    liveSpeed = LiveSpeed(0L, 0L)
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }

    companion object {
        fun provideFactory(application: NetPulseApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(
                        application = application,
                        repository = application.networkRepository
                    ) as T
                }
            }
    }
}
