package com.ayush.netpulse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ayush.netpulse.R
import com.ayush.netpulse.util.PreferencesManager

/**
 * Helpers that let NetPulse come back on its own after Android stops the tracking service
 * (for example overnight while offline).
 *
 * - A PendingIntent-based network callback is held by the system, so it still fires when our
 *   process is dead. The manifest CONNECTIVITY_CHANGE broadcast is never delivered to manifest
 *   receivers on modern Android, so this replaces it.
 * - If Android refuses to start the foreground service from the background, we retry with
 *   backoff and show a "tap to resume" notification (a tap is always allowed to start it).
 */
object ServiceRevival {

    private const val TAG = "ServiceRevival"

    const val ACTION_NETWORK_CHANGED = "com.ayush.netpulse.action.NETWORK_CHANGED"
    const val RESUME_PROMPT_NOTIFICATION_ID = 1002
    const val RESUME_PROMPT_CHANNEL_ID = "netpulse_resume_prompt"

    private const val NETWORK_WAKEUP_REQUEST_CODE = 2002
    private const val RESUME_PROMPT_REQUEST_CODE = 2003
    private const val BASE_BACKOFF_MS = 15_000L
    private const val MAX_BACKOFF_MS = 15 * 60 * 1000L

    /** Delay before the next retry: 15s, 30s, 60s ... capped at 15 minutes. */
    fun nextBackoffMs(retryCount: Int): Long {
        val shift = retryCount.coerceIn(0, 10)
        return (BASE_BACKOFF_MS shl shift).coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun networkWakeupIntent(context: Context): PendingIntent {
        val intent = Intent(context, ServiceRestartReceiver::class.java).apply {
            action = ACTION_NETWORK_CHANGED
        }
        // The system fills in the network extras, so this PendingIntent must be mutable on Android 12+.
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            NETWORK_WAKEUP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
        )
    }

    /**
     * Asks the system to wake NetPulse whenever a network appears or changes, even if the app
     * process is dead. Safe to call repeatedly: an equal PendingIntent replaces the old request.
     * Do not call this from the receiver that handles ACTION_NETWORK_CHANGED, since registering
     * triggers an immediate callback.
     */
    fun registerNetworkWakeup(context: Context) {
        try {
            val appContext = context.applicationContext
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkWakeupIntent(appContext))
            Log.d(TAG, "Registered persistent network wake-up callback")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not register network wake-up callback: ${e.message}", e)
        }
    }

    /** Stops the system from waking NetPulse on network changes (used when the user turns tracking off). */
    fun unregisterNetworkWakeup(context: Context) {
        try {
            val appContext = context.applicationContext
            val connectivityManager =
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            connectivityManager.unregisterNetworkCallback(networkWakeupIntent(appContext))
            Log.d(TAG, "Unregistered persistent network wake-up callback")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not unregister network wake-up callback: ${e.message}")
        }
    }

    /**
     * Called when Android refuses to start the foreground service. Records why, shows a
     * tap-to-resume notification, and schedules the next retry with backoff.
     */
    fun handleStartFailure(context: Context, source: String, error: Throwable) {
        val appContext = context.applicationContext
        val prefs = PreferencesManager.getInstance(appContext)
        val retryCount = prefs.revivalRetryCount
        val delayMs = nextBackoffMs(retryCount)
        prefs.revivalRetryCount = retryCount + 1

        val reason = "${error.javaClass.simpleName}: ${error.message}"
        prefs.lastKnownError = "Start blocked ($source): $reason"
        prefs.appendRevivalLog(source, "blocked ($reason); retry in ${delayMs / 1000}s")
        Log.w(TAG, "Foreground start blocked from $source: $reason; retrying in ${delayMs}ms")

        postResumePrompt(appContext)
        SpeedTrackerService.scheduleRestartAlarm(appContext, delayMs)
    }

    /** Called after the service successfully starts, so the next failure starts from a short delay again. */
    fun onStartSucceeded(context: Context, source: String) {
        val appContext = context.applicationContext
        val prefs = PreferencesManager.getInstance(appContext)
        prefs.revivalRetryCount = 0
        prefs.appendRevivalLog(source, "service started")
        cancelResumePrompt(appContext)
    }

    private fun postResumePrompt(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    RESUME_PROMPT_CHANNEL_ID,
                    "NetPulse Resume Prompt",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shown when Android blocks NetPulse from restarting itself"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val startIntent = Intent(context, SpeedTrackerService::class.java).apply {
                action = SpeedTrackerService.ACTION_START
                putExtra(SpeedTrackerService.EXTRA_START_SOURCE, "resume-prompt-tap")
            }
            val pendingStart = PendingIntent.getForegroundService(
                context,
                RESUME_PROMPT_REQUEST_CODE,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, RESUME_PROMPT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("NetPulse is paused")
                .setContentText("Tap to resume live speed tracking")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingStart)
                .build()

            notificationManager.notify(RESUME_PROMPT_NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.w(TAG, "Could not post resume prompt: ${e.message}", e)
        }
    }

    fun cancelResumePrompt(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(RESUME_PROMPT_NOTIFICATION_ID)
        } catch (e: Throwable) {
            Log.w(TAG, "Could not cancel resume prompt: ${e.message}")
        }
    }
}
