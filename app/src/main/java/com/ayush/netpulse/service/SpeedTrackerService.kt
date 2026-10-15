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

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferencesManager = PreferencesManager.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopForegroundMonitoring()
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

                tickCount++
                // Update persistent status bar notification every tick (1s)
                val notification = buildNotification(speed, currentNetworkType)
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Periodically persist notable speeds or every 5 seconds to Room
                if (speed.downloadBytesPerSec > 10240 || speed.uploadBytesPerSec > 10240 || tickCount >= 5) {
                    tickCount = 0
                    repository.saveSpeedRecord(speed, currentNetworkType)
                }

                delay(1000)
            }
        }
    }

    private fun stopForegroundMonitoring() {
        preferencesManager.isBackgroundMonitoringEnabled = false
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
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = "↓ ${speed.formattedDownload}   ↑ ${speed.formattedUpload}"
        val subText = networkType.displayName

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NetPulse • Live Speed ($subText)")
            .setContentText(speedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$speedText\nActive Connection: $subText"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingOpenApp)
            .addAction(0, "Stop Monitoring", pendingStop)
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
        if (preferencesManager.isBackgroundMonitoringEnabled) {
            // Keep running as sticky foreground service
        } else {
            stopForegroundMonitoring()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunningState.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "netpulse_live_speed_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ayush.netpulse.action.START"
        const val ACTION_STOP = "com.ayush.netpulse.action.STOP"

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

        fun stop(context: Context) {
            val intent = Intent(context, SpeedTrackerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
