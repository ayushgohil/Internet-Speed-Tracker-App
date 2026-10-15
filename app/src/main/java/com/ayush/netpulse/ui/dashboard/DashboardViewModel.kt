package com.ayush.netpulse.ui.dashboard

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.data.model.DiagnosticsInfo
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.repository.NetworkRepository
import com.ayush.netpulse.service.SpeedTrackerService
import com.ayush.netpulse.util.NetworkUtils
import com.ayush.netpulse.util.PreferencesManager
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
    private val repository: NetworkRepository,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var localMonitoringJob: Job? = null
    private var recordSaveCounter = 0

    init {
        // Observe network connection type
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

        // Observe recent Room daily usage records
        viewModelScope.launch {
            repository.getRecentDailyUsage(7).collect { dailyRecords ->
                _uiState.update { it.copy(recentDailyRecords = dailyRecords) }
            }
        }

        // Observe background service running state
        viewModelScope.launch {
            SpeedTrackerService.isServiceRunningState.collect { running ->
                _uiState.update { it.copy(isBackgroundServiceRunning = running) }
                refreshDiagnostics()
                if (running) {
                    stopLocalMonitoring()
                } else if (_uiState.value.isMonitoringActive) {
                    startLocalMonitoring()
                }
            }
        }

        // Observe speed from foreground service when it is running
        viewModelScope.launch {
            SpeedTrackerService.liveSpeedState.collect { speed ->
                if (_uiState.value.isBackgroundServiceRunning) {
                    _uiState.update { it.copy(liveSpeed = speed) }
                }
            }
        }

        // Initial permissions, usage, and diagnostics check
        refreshPermissionsAndUsage()

        // If background monitoring was previously enabled by user, restart the service
        if (preferencesManager.isBackgroundMonitoringEnabled && !_uiState.value.isBackgroundServiceRunning) {
            SpeedTrackerService.start(application)
        } else {
            startLocalMonitoring()
        }
    }

    fun refreshPermissionsAndUsage() {
        val context = getApplication<Application>().applicationContext
        val hasUsagePerm = NetworkUtils.hasUsageStatsPermission(context)
        val isBatteryIgnored = NetworkUtils.isBatteryOptimizationIgnored(context)

        val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _uiState.update {
            it.copy(
                hasUsageAccessPermission = hasUsagePerm,
                hasNotificationPermission = hasNotifPerm,
                isBatteryOptimizationIgnored = isBatteryIgnored
            )
        }

        refreshDiagnostics()

        if (hasUsagePerm) {
            loadDailyDataUsage()
        } else {
            _uiState.update {
                it.copy(
                    usageErrorMessage = "Usage Access is required to read daily Wi-Fi and Mobile data."
                )
            }
        }
    }

    fun refreshDiagnostics() {
        val isServiceRunning = SpeedTrackerService.isServiceRunningState.value
        val lastSampleTime = preferencesManager.lastSpeedSampleTimestamp
        val timeSinceLastSample = System.currentTimeMillis() - lastSampleTime
        val isPotentiallyStale = isServiceRunning && lastSampleTime > 0L && timeSinceLastSample > 45000L

        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = preferencesManager.isBackgroundMonitoringEnabled,
            isForegroundServiceRunning = isServiceRunning,
            isServiceStatePotentiallyStale = isPotentiallyStale,
            lastSpeedSampleTimestamp = lastSampleTime,
            lastReconciliationTimestamp = preferencesManager.lastReconciledTimestamp,
            lastReconciliationStatus = preferencesManager.lastReconciliationStatus ?: "Not executed",
            lastServiceStartTimestamp = preferencesManager.lastServiceStartTimestamp,
            lastServiceStopReason = preferencesManager.lastServiceStopReason ?: "None",
            hasNotificationPermission = _uiState.value.hasNotificationPermission,
            hasUsageAccessPermission = _uiState.value.hasUsageAccessPermission,
            isBatteryOptimizationIgnored = _uiState.value.isBatteryOptimizationIgnored,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            deviceManufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            deviceModel = Build.MODEL
        )

        _uiState.update { it.copy(diagnosticsInfo = diag) }
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
                refreshDiagnostics()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingUsage = false,
                        usageErrorMessage = err.localizedMessage ?: "Failed to read data usage"
                    )
                }
                refreshDiagnostics()
            }
        }
    }

    fun toggleBackgroundService() {
        val context = getApplication<Application>().applicationContext
        if (_uiState.value.isBackgroundServiceRunning) {
            SpeedTrackerService.stop(context, "User toggled off in UI")
        } else {
            SpeedTrackerService.start(context)
        }
        refreshDiagnostics()
    }

    fun toggleMonitoring() {
        val willBeActive = !_uiState.value.isMonitoringActive
        if (willBeActive) {
            _uiState.update { it.copy(isMonitoringActive = true) }
            if (!_uiState.value.isBackgroundServiceRunning) {
                startLocalMonitoring()
            }
        } else {
            _uiState.update { it.copy(isMonitoringActive = false) }
            if (_uiState.value.isBackgroundServiceRunning) {
                toggleBackgroundService()
            }
            stopLocalMonitoring()
        }
    }

    fun onResumeScreen() {
        refreshPermissionsAndUsage()
        if (!_uiState.value.isBackgroundServiceRunning && _uiState.value.isMonitoringActive) {
            startLocalMonitoring()
        }
    }

    fun onPauseScreen() {
        if (!_uiState.value.isBackgroundServiceRunning) {
            stopLocalMonitoring()
        }
    }

    private fun startLocalMonitoring() {
        localMonitoringJob?.cancel()
        repository.resetSpeedBaseline()

        localMonitoringJob = viewModelScope.launch {
            while (isActive) {
                val speed = repository.sampleCurrentSpeed()
                _uiState.update { it.copy(liveSpeed = speed) }
                preferencesManager.lastSpeedSampleTimestamp = System.currentTimeMillis()

                recordSaveCounter++
                if (speed.downloadBytesPerSec > 10240 || speed.uploadBytesPerSec > 10240 || recordSaveCounter >= 3) {
                    recordSaveCounter = 0
                    repository.saveSpeedRecord(speed, _uiState.value.networkType)
                }

                delay(1000)
            }
        }
    }

    private fun stopLocalMonitoring() {
        localMonitoringJob?.cancel()
        localMonitoringJob = null
        repository.resetSpeedBaseline()
        if (!_uiState.value.isBackgroundServiceRunning) {
            _uiState.update { it.copy(liveSpeed = LiveSpeed(0L, 0L)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocalMonitoring()
    }

    companion object {
        fun provideFactory(application: NetPulseApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(
                        application = application,
                        repository = application.networkRepository,
                        preferencesManager = application.preferencesManager
                    ) as T
                }
            }
    }
}
