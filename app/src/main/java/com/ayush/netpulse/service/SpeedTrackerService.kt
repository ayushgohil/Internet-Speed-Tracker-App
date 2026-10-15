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
import androidx.core.app.NotificationCompat
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.R
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.ui.MainActivity
import com.ayush.netpulse.util.PreferencesManager
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                val reason = intent?.getStringExtra(EXTRA_STOP_REASON) ?: "Stopped by user"
                stopForegroundMonitoring(reason)
                return START_NOT_STICKY
            }
            ACTION_START -> {
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
        _isServiceRunningState.value = true

        val initialNotification = buildNotification(
            LiveSpeed(0L, 0L),
            currentNetworkType
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startSamplingLoops()
    }

    private fun startSamplingLoops() {
        trackerJob?.cancel()
        networkObserverJob?.cancel()

        val repository = (application as NetPulseApp).networkRepository
        repository.resetSpeedBaseline()

        // Observe network connection changes
        networkObserverJob = serviceScope.launch {
            repository.observeNetworkType().collect { netType ->
                currentNetworkType = netType
                _activeNetworkTypeState.value = netType
            }
        }

        // Periodic live speed sampling and notification update
        trackerJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                val speed = repository.sampleCurrentSpeed()
                _liveSpeedState.value = speed
                preferencesManager.lastSpeedSampleTimestamp = System.currentTimeMillis()

                tickCount++

                // Efficient notification update: only update system notification when text changed or every 4 ticks
                val signature = "${speed.formattedDownload}|${speed.formattedUpload}|${currentNetworkType.name}"
                if (signature != lastNotificationSignature || tickCount >= 4) {
                    lastNotificationSignature = signature
                    val notification = buildNotification(speed, currentNetworkType)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }

                // Battery efficiency: persist speed to Room only when notable activity or every 10 ticks
                if (speed.downloadBytesPerSec > 10240 || speed.uploadBytesPerSec > 10240 || tickCount >= 10) {
                    if (speed.downloadBytesPerSec > 0 || speed.uploadBytesPerSec > 0 || tickCount >= 20) {
                        repository.saveSpeedRecord(speed, currentNetworkType)
                        tickCount = 0
                    }
                }

                // When screen is off / locked, adapt update rate to 2500ms to preserve battery
                val isScreenInteractive = powerManager.isInteractive
                val delayTime = if (isScreenInteractive) 1000L else 2500L
                delay(delayTime)
            }
        }
    }

    private fun stopForegroundMonitoring(reason: String = "Stopped by user") {
        preferencesManager.isBackgroundMonitoringEnabled = false
        preferencesManager.lastServiceStopReason = reason
        _isServiceRunningState.value = false
        _liveSpeedState.value = LiveSpeed(0L, 0L)

        trackerJob?.cancel()
        networkObserverJob?.cancel()
        trackerJob = null
        networkObserverJob = null

        val repository = (application as NetPulseApp).networkRepository
        repository.resetSpeedBaseline()

        stopForeground(STOP_FOREGROUND_REMOVE)
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(speedText)
            .setContentText(connectionText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingOpenApp)
            .addAction(0, "Stop", pendingStop)
            .build()
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
        if (!preferencesManager.isBackgroundMonitoringEnabled) {
            stopForegroundMonitoring("App swiped from recent tasks while disabled")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_isServiceRunningState.value) {
            preferencesManager.lastServiceStopReason = "Destroyed by system"
        }
        _isServiceRunningState.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
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
