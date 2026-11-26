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
import com.ayush.netpulse.util.DataLimit
import com.ayush.netpulse.util.PreferencesManager
import com.ayush.netpulse.widget.UsageWidgetProvider
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
    private val speedSmoother = com.ayush.netpulse.util.SpeedSmoother()

    init {
        // Observe network connection type
        viewModelScope.launch {
            repository.observeNetworkType().collect { netType ->
                _uiState.update { it.copy(networkType = netType, liveSpeedHistory = if (it.networkType != netType) emptyList() else it.liveSpeedHistory) }
                // Notify smoother of network switch to prevent stale values across transports
                val currentRaw = _uiState.value.liveSpeed
                val smoothed = speedSmoother.smooth(currentRaw, netType)
                _uiState.update { it.copy(liveSpeed = smoothed) }
            }
        }

        // Observe recent Room speed records
        viewModelScope.launch {
            repository.getRecentSpeedRecords(15).collect { records ->
                _uiState.update { it.copy(recentRecords = records) }
            }
        }

        // Observe recent Room daily usage records (up to 90 days)
        viewModelScope.launch {
            repository.getRecentDailyUsage(90).collect { dailyRecords ->
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
                } else if (_uiState.value.isMonitoringActive && preferencesManager.isBackgroundMonitoringEnabled) {
                    startLocalMonitoring()
                }
            }
        }

        // Observe speed from authoritative liveSpeedState (piped through SpeedSmoother)
        viewModelScope.launch {
            SpeedTrackerService.liveSpeedState.collect { rawSpeed ->
                if (!SpeedTrackerService.isServiceRunningState.value) return@collect
                val netType = _uiState.value.networkType
                val smoothed = speedSmoother.smooth(rawSpeed, netType)
                _uiState.update { state ->
                    val history = (state.liveSpeedHistory + smoothed)
                        .filter { it.timestamp >= smoothed.timestamp - 60_000L }
                        .takeLast(120)
                    state.copy(liveSpeed = smoothed, liveSpeedHistory = history)
                }
                preferencesManager.lastDisplayedSpeedTimestamp = System.currentTimeMillis()
            }
        }

        // Initial permissions, usage, and diagnostics check
        refreshPermissionsAndUsage()

        // Initial preferences state
        _uiState.update {
            it.copy(
                showSpeedInStatusBar = preferencesManager.showSpeedInStatusBar,
                showOnLockScreen = preferencesManager.showOnLockScreen,
                dataLimitBytes = preferencesManager.dataLimitBytes,
                dailyDataLimitBytes = preferencesManager.dailyDataLimitBytes,
                dataLimitCycleDay = preferencesManager.dataLimitCycleDay,
                cycleMobileBytes = preferencesManager.cachedCycleMobileBytes,
                speedUnitPreference = preferencesManager.speedUnitPreference,
                notificationStyle = preferencesManager.notificationStyle,
                appTheme = preferencesManager.appTheme,
                notificationRefreshIntervalMs = preferencesManager.notificationRefreshIntervalMs
            )
        }

        // Single authoritative pipeline:
        // If background monitoring is enabled by user, start the service.
        // Never start concurrent local monitoring when background monitoring is requested.
        if (preferencesManager.isBackgroundMonitoringEnabled) {
            if (!SpeedTrackerService.isServiceRunningState.value) {
                SpeedTrackerService.start(application)
            }
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
        val context = getApplication<Application>().applicationContext
        val isServiceRunning = SpeedTrackerService.isServiceRunningState.value
        val lastSampleTime = preferencesManager.lastSpeedSampleTimestamp
        val timeSinceLastSample = System.currentTimeMillis() - lastSampleTime
        val isPotentiallyStale = isServiceRunning && lastSampleTime > 0L && timeSinceLastSample > 45000L
        val confirmedExit = NetworkUtils.getConfirmedProcessExitReason(context)

        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = preferencesManager.isBackgroundMonitoringEnabled,
            isForegroundServiceRunning = isServiceRunning,
            isServiceStatePotentiallyStale = isPotentiallyStale,
            lastSpeedSampleTimestamp = lastSampleTime,
            lastReconciliationTimestamp = preferencesManager.lastReconciledTimestamp,
            lastReconciliationStatus = preferencesManager.lastReconciliationStatus ?: "Not executed",
            lastServiceStartTimestamp = preferencesManager.lastServiceStartTimestamp,
            lastServiceStopReason = preferencesManager.lastServiceStopReason ?: "None",
            confirmedProcessExitReason = confirmedExit,
            lastKnownError = preferencesManager.lastKnownError,
            logRetentionDays = preferencesManager.logRetentionDays,
            lastCleanupTimestamp = preferencesManager.lastCleanupTimestamp,
            lastNotificationUpdateTimestamp = preferencesManager.lastNotificationUpdateTimestamp,
            lastAttemptedNotificationTimestamp = preferencesManager.lastAttemptedNotificationTimestamp,
            lastDisplayedSpeedTimestamp = preferencesManager.lastDisplayedSpeedTimestamp,
            lastNotificationException = preferencesManager.lastNotificationException,
            lastServiceDestroyTimestamp = preferencesManager.lastServiceDestroyTimestamp,
            serviceRestartCount = preferencesManager.serviceRestartCount,
            revivalLog = preferencesManager.revivalLog,
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

    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    private var usageLoadJob: Job? = null

    fun loadDailyDataUsage(forceReconcile: Boolean = false, isSilent: Boolean = false) {
        if (usageLoadJob?.isActive == true && !forceReconcile) return
        usageLoadJob?.cancel()
        usageLoadJob = viewModelScope.launch {
            if (!isSilent) {
                _uiState.update { it.copy(isLoadingUsage = true, usageErrorMessage = null) }
            }
            val result = repository.getTodayDataUsage(forceReconcile = forceReconcile)
            result.onSuccess { usage ->
                _uiState.update {
                    it.copy(
                        dailyUsage = usage,
                        cycleMobileBytes = preferencesManager.cachedCycleMobileBytes,
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
            preferencesManager.isBackgroundMonitoringEnabled = false
            _uiState.update { it.copy(isBackgroundServiceRunning = false, isMonitoringActive = false) }
            SpeedTrackerService.stop(context, "User toggled off in UI")
            stopLocalMonitoring()
        } else {
            preferencesManager.isBackgroundMonitoringEnabled = true
            _uiState.update { it.copy(isBackgroundServiceRunning = true, isMonitoringActive = true) }
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
        if (preferencesManager.isBackgroundMonitoringEnabled && !SpeedTrackerService.isServiceRunningState.value) {
            val context = getApplication<Application>().applicationContext
            SpeedTrackerService.start(context)
        }
        if (!_uiState.value.isBackgroundServiceRunning && _uiState.value.isMonitoringActive && !preferencesManager.isBackgroundMonitoringEnabled) {
            startLocalMonitoring()
        }
    }

    fun onPauseScreen() {
        if (!_uiState.value.isBackgroundServiceRunning && !preferencesManager.isBackgroundMonitoringEnabled) {
            stopLocalMonitoring()
        }
    }

    private fun startLocalMonitoring() {
        if (preferencesManager.isBackgroundMonitoringEnabled || SpeedTrackerService.isServiceRunningState.value) {
            // Never run duplicate sampling loop if background service is active or requested
            return
        }
        localMonitoringJob?.cancel()
        repository.resetSpeedBaseline()

        localMonitoringJob = viewModelScope.launch {
            while (isActive) {
                val rawSpeed = repository.sampleCurrentSpeed()
                val netType = _uiState.value.networkType
                val smoothed = speedSmoother.smooth(rawSpeed, netType)
                _uiState.update { state ->
                    val history = (state.liveSpeedHistory + smoothed)
                        .filter { it.timestamp >= smoothed.timestamp - 60_000L }
                        .takeLast(120)
                    state.copy(liveSpeed = smoothed, liveSpeedHistory = history)
                }
                preferencesManager.lastSpeedSampleTimestamp = System.currentTimeMillis()
                preferencesManager.lastDisplayedSpeedTimestamp = System.currentTimeMillis()

                recordSaveCounter++
                if (rawSpeed.downloadBytesPerSec > 10240 || rawSpeed.uploadBytesPerSec > 10240 || recordSaveCounter >= 3) {
                    recordSaveCounter = 0
                    repository.saveSpeedRecord(rawSpeed, _uiState.value.networkType)
                }

                delay(1000)
            }
        }
    }

    private fun stopLocalMonitoring() {
        localMonitoringJob?.cancel()
        localMonitoringJob = null
        speedSmoother.reset()
        if (!SpeedTrackerService.isServiceRunningState.value && !preferencesManager.isBackgroundMonitoringEnabled) {
            repository.resetSpeedBaseline()
            _uiState.update { it.copy(liveSpeed = LiveSpeed(0L, 0L), liveSpeedHistory = emptyList()) }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearSpeedRecords()
        }
    }

    fun setLogRetentionDays(days: Int) {
        preferencesManager.logRetentionDays = days
        com.ayush.netpulse.worker.LogRetentionWorker.triggerImmediate(getApplication())
        refreshDiagnostics()
    }

    fun toggleShowSpeedInStatusBar() {
        val next = !preferencesManager.showSpeedInStatusBar
        preferencesManager.showSpeedInStatusBar = next
        _uiState.update { it.copy(showSpeedInStatusBar = next) }
        val context = getApplication<Application>().applicationContext
        if (_uiState.value.isBackgroundServiceRunning) {
            SpeedTrackerService.start(context)
        }
    }

    /** Sets the monthly mobile data limit in GB; 0 or less turns the limit and its alerts off. */
    fun setDailyDataLimitGb(gb: Double) {
        val bytes = DataLimit.gbToBytes(gb)
        preferencesManager.dailyDataLimitBytes = bytes
        preferencesManager.dailyDataLimitAlertState = ""
        _uiState.update { it.copy(dailyDataLimitBytes = bytes) }
        if (bytes != 0L && _uiState.value.hasUsageAccessPermission) loadDailyDataUsage(forceReconcile = true)
    }

    fun setDataLimitGb(gb: Double) {
        val bytes = DataLimit.gbToBytes(gb)
        preferencesManager.dataLimitBytes = bytes
        preferencesManager.cachedCycleMobileBytes = 0L
        preferencesManager.dataLimitAlertState = "" // new limit: alerts may fire again
        _uiState.update { it.copy(dataLimitBytes = bytes, cycleMobileBytes = preferencesManager.cachedCycleMobileBytes) }
        if (bytes != 0L && _uiState.value.hasUsageAccessPermission) loadDailyDataUsage(forceReconcile = true)
        UsageWidgetProvider.refresh(getApplication())
    }

    fun setDataLimitCycleDay(day: Int) {
        val safeDay = day.coerceIn(DataLimit.MIN_CYCLE_DAY, DataLimit.MAX_CYCLE_DAY)
        preferencesManager.dataLimitCycleDay = safeDay
        preferencesManager.cachedCycleMobileBytes = 0L
        preferencesManager.dataLimitAlertState = ""
        _uiState.update { it.copy(dataLimitCycleDay = safeDay, cycleMobileBytes = 0L) }
        if (preferencesManager.dataLimitBytes != 0L && _uiState.value.hasUsageAccessPermission) {
            loadDailyDataUsage(forceReconcile = true)
        }
    }

    fun toggleShowOnLockScreen() {
        val next = !preferencesManager.showOnLockScreen
        preferencesManager.showOnLockScreen = next
        _uiState.update { it.copy(showOnLockScreen = next) }
        val context = getApplication<Application>().applicationContext
        // Re-post the notification so the new lock-screen setting takes effect straight away.
        if (_uiState.value.isBackgroundServiceRunning) {
            SpeedTrackerService.start(context)
        }
    }

    fun setSpeedUnitPreference(unit: String) {
        preferencesManager.speedUnitPreference = unit
        _uiState.update { it.copy(speedUnitPreference = unit) }
    }

    fun setNotificationStylePreference(style: String) {
        preferencesManager.notificationStyle = style
        _uiState.update { it.copy(notificationStyle = style) }
        val context = getApplication<Application>().applicationContext
        // Trigger notification update with the new style without resetting service or baseline
        if (_uiState.value.isBackgroundServiceRunning) {
            SpeedTrackerService.start(context)
        }
    }

    fun setNotificationRefreshInterval(intervalMs: Long) {
        preferencesManager.notificationRefreshIntervalMs = intervalMs
        _uiState.update { it.copy(notificationRefreshIntervalMs = intervalMs) }
    }

    fun setAppThemePreference(theme: String) {
        preferencesManager.appTheme = theme
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun triggerManualReconciliation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReconcilingUsage = true) }
            repository.reconcileMultiDayHistoricalUsage(90)
            refreshDiagnostics()
            _uiState.update { it.copy(isReconcilingUsage = false) }
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
