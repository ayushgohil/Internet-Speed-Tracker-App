package com.ayush.netpulse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.R
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.ui.MainActivity
import com.ayush.netpulse.util.PreferencesManager
import com.ayush.netpulse.util.SpeedIconGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeedTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var trackerJob: Job? = null
    private var networkObserverJob: Job? = null
    private var currentNetworkType: NetworkType = NetworkType.DISCONNECTED

    private lateinit var notificationManager: NotificationManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var powerManager: PowerManager

    private var lastNotificationSignature: String = ""

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferencesManager = PreferencesManager.getInstance(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        createNotificationChannel()
        Log.d(TAG, "SpeedTrackerService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If intent is null, Android recreated the service via START_STICKY
        if (intent == null) {
            if (!preferencesManager.isBackgroundMonitoringEnabled) {
                Log.d(TAG, "System recreated START_STICKY service but user previously disabled monitoring; stopping self")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            Log.d(TAG, "System recreated START_STICKY service while monitoring was enabled; restoring foreground session")
            preferencesManager.serviceRestartCount = preferencesManager.serviceRestartCount + 1
            preferencesManager.lastKnownServiceState = "Active (Restored by OS)"
            startForegroundMonitoring()
            return START_STICKY
        }

        val action = intent.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                val reason = intent.getStringExtra(EXTRA_STOP_REASON) ?: "Stopped by user"
                stopForegroundMonitoring(reason)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (_isServiceRunningState.value && trackerJob?.isActive == true) {
                    Log.d(TAG, "Service is already running actively; updating notification and skipping re-initialization")
                    updateNotificationSafely(_liveSpeedState.value, currentNetworkType)
                    return START_STICKY
                }
                startForegroundMonitoring()
                return START_STICKY
            }
            else -> {
                startForegroundMonitoring()
                return START_STICKY
            }
        }
    }

    private fun startForegroundMonitoring() {
        preferencesManager.isBackgroundMonitoringEnabled = true
        preferencesManager.lastServiceStartTimestamp = System.currentTimeMillis()
        preferencesManager.lastKnownServiceState = "Active"
        _isServiceRunningState.value = true

        val initialNotification = buildNotification(
            LiveSpeed(0L, 0L),
            currentNetworkType
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            preferencesManager.lastNotificationUpdateTimestamp = System.currentTimeMillis()
            preferencesManager.lastKnownError = null
            Log.d(TAG, "startForeground completed successfully with channel $CHANNEL_ID")
        } catch (e: Exception) {
            val err = "Foreground start exception (${e.javaClass.simpleName}): ${e.message}"
            Log.e(TAG, err, e)
            preferencesManager.lastKnownError = err
            preferencesManager.lastKnownServiceState = "Start Restricted"
            _isServiceRunningState.value = false
            stopSelf()
            return
        }

        startSamplingLoops()
    }

    private fun startSamplingLoops() {
        trackerJob?.cancel()
        networkObserverJob?.cancel()

        val repository = (application as NetPulseApp).networkRepository
        repository.resetSpeedBaseline()

        // 1. Observe network connection changes safely
        networkObserverJob = serviceScope.launch {
            try {
                repository.observeNetworkType().collect { netType ->
                    currentNetworkType = netType
                    _activeNetworkTypeState.value = netType
                    // When network switches (e.g. Wi-Fi <-> Mobile or Disconnected), immediately update notification
                    val speed = if (netType == NetworkType.DISCONNECTED) LiveSpeed(0L, 0L) else _liveSpeedState.value
                    updateNotificationSafely(speed, netType)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.w(TAG, "Network observer stream interrupted: ${e.message}")
                    preferencesManager.lastKnownError = "Network observer: ${e.message}"
                }
            }
        }

        // 2. Periodic live speed sampling and notification update
        trackerJob = serviceScope.launch {
            var tickCount = 0
            var wasLastSavedSpeedZero = false
            var lastPrefsWriteTimestamp = 0L

            while (isActive) {
                try {
                    val isScreenInteractive = powerManager.isInteractive
                    val isOffline = (currentNetworkType == NetworkType.DISCONNECTED)
                    val now = System.currentTimeMillis()

                    val speed = if (isOffline) {
                        LiveSpeed(0L, 0L, now)
                    } else {
                        repository.sampleCurrentSpeed()
                    }
                    _liveSpeedState.value = speed

                    // Throttled SharedPreferences persistence: Avoid writing to flash memory every 1-2.5s all night
                    if (now - lastPrefsWriteTimestamp >= 30000L || (!isOffline && (speed.downloadBytesPerSec > 0 || speed.uploadBytesPerSec > 0))) {
                        preferencesManager.lastSpeedSampleTimestamp = now
                        lastPrefsWriteTimestamp = now
                    }

                    tickCount++

                    // Efficient notification update
                    val compactSpeed = SpeedIconGenerator.formatCompactSpeed(speed.downloadBytesPerSec)
                    val signature = "${speed.formattedDownload}|${speed.formattedUpload}|${currentNetworkType.name}|$compactSpeed|${preferencesManager.showSpeedInStatusBar}"
                    if (signature != lastNotificationSignature || (isScreenInteractive && tickCount >= 4)) {
                        lastNotificationSignature = signature
                        updateNotificationSafely(speed, currentNetworkType)
                    }

                    val isZeroSpeed = speed.downloadBytesPerSec == 0L && speed.uploadBytesPerSec == 0L

                    // Battery & storage efficiency:
                    // Never write to Room database when offline or during zero-traffic idle
                    if (!isOffline) {
                        if (isScreenInteractive) {
                            if (isZeroSpeed) {
                                if (!wasLastSavedSpeedZero) {
                                    repository.saveSpeedRecord(speed, currentNetworkType)
                                    wasLastSavedSpeedZero = true
                                    tickCount = 0
                                }
                            } else {
                                wasLastSavedSpeedZero = false
                                if (speed.downloadBytesPerSec > 10240 || speed.uploadBytesPerSec > 10240 || tickCount >= 10) {
                                    repository.saveSpeedRecord(speed, currentNetworkType)
                                    tickCount = 0
                                }
                            }
                        } else {
                            // Screen locked/off: only log substantial active downloads (> 50 KB/s)
                            if (speed.downloadBytesPerSec > 51200 || speed.uploadBytesPerSec > 51200) {
                                repository.saveSpeedRecord(speed, currentNetworkType)
                                tickCount = 0
                            }
                        }
                    }

                    // Adaptive sleep interval to preserve battery overnight:
                    // Interactive: 1000ms
                    // Screen locked + online: 5000ms
                    // Screen locked + offline (disconnected / airplane mode): 10000ms (10s)
                    val delayTime = when {
                        isScreenInteractive -> 1000L
                        isOffline -> 10000L
                        else -> 5000L
                    }
                    delay(delayTime)

                } catch (e: CancellationException) {
                    throw e // propagate coroutine cancellation cleanly
                } catch (e: Exception) {
                    Log.e(TAG, "Error in speed tracking loop: ${e.message}", e)
                    preferencesManager.lastKnownError = "Loop error: ${e.message}"
                    delay(5000L) // safe backoff before retry
                }
            }
        }
    }

    private fun updateNotificationSafely(speed: LiveSpeed, networkType: NetworkType) {
        try {
            val notification = buildNotification(speed, networkType)
            notificationManager.notify(NOTIFICATION_ID, notification)
            preferencesManager.lastNotificationUpdateTimestamp = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification: ${e.message}")
            preferencesManager.lastKnownError = "Notification update failed: ${e.message}"
        }
    }

    private fun stopForegroundMonitoring(reason: String = "Stopped by user") {
        Log.d(TAG, "stopForegroundMonitoring requested: $reason")
        preferencesManager.isBackgroundMonitoringEnabled = false
        preferencesManager.lastServiceStopReason = reason
        preferencesManager.lastKnownServiceState = "Stopped"
        _isServiceRunningState.value = false
        _liveSpeedState.value = LiveSpeed(0L, 0L)

        trackerJob?.cancel()
        networkObserverJob?.cancel()
        trackerJob = null
        networkObserverJob = null

        val repository = (application as NetPulseApp).networkRepository
        repository.resetSpeedBaseline()

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun buildNotification(speed: LiveSpeed, networkType: NetworkType): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpeedTrackerService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_STOP_REASON, "Stopped via notification action")
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = "↓ ${speed.formattedDownload}   ↑ ${speed.formattedUpload}"
        val connectionText = networkType.displayName

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(speedText)
            .setContentText(connectionText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingOpenApp)
            .addAction(0, "Stop", pendingStop)

        if (preferencesManager.showSpeedInStatusBar) {
            val speedIcon = SpeedIconGenerator.createSpeedIcon(speed.downloadBytesPerSec)
            if (speedIcon != null) {
                builder.setSmallIcon(speedIcon)
            } else {
                builder.setSmallIcon(R.drawable.ic_launcher_foreground)
            }
        } else {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NetPulse Speed Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays continuous internet speed in status bar notification"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping app from recent tasks must not kill monitoring if user enabled it
        Log.d(TAG, "onTaskRemoved called; monitoring enabled: ${preferencesManager.isBackgroundMonitoringEnabled}")
        if (!preferencesManager.isBackgroundMonitoringEnabled) {
            stopForegroundMonitoring("App swiped from recent tasks while disabled")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        val now = System.currentTimeMillis()
        preferencesManager.lastServiceDestroyTimestamp = now

        if (preferencesManager.isBackgroundMonitoringEnabled) {
            val reason = "Service destroyed by OS lifecycle callback"
            preferencesManager.lastServiceStopReason = reason
            preferencesManager.lastKnownServiceState = "Destroyed by OS"
            Log.w(TAG, "SpeedTrackerService onDestroy while monitoring was enabled: $reason")
        } else {
            preferencesManager.lastKnownServiceState = "Stopped"
        }

        _isServiceRunningState.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SpeedTrackerService"
        const val CHANNEL_ID = "netpulse_live_speed_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ayush.netpulse.action.START"
        const val ACTION_STOP = "com.ayush.netpulse.action.STOP"
        const val EXTRA_STOP_REASON = "extra_stop_reason"

        private val _liveSpeedState = MutableStateFlow(LiveSpeed())
        val liveSpeedState: StateFlow<LiveSpeed> = _liveSpeedState.asStateFlow()

        private val _isServiceRunningState = MutableStateFlow(false)
        val isServiceRunningState: StateFlow<Boolean> = _isServiceRunningState.asStateFlow()

        private val _activeNetworkTypeState = MutableStateFlow(NetworkType.DISCONNECTED)
        val activeNetworkTypeState: StateFlow<NetworkType> = _activeNetworkTypeState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SpeedTrackerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context, reason: String = "Stopped by user") {
            val intent = Intent(context, SpeedTrackerService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_STOP_REASON, reason)
            }
            context.startService(intent)
        }
    }
}
