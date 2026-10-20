package com.ayush.netpulse.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.ayush.netpulse.util.PreferencesManager

/**
 * High-resilience broadcast receiver that automatically revives SpeedTrackerService
 * when the app task is removed, after device reboot/update, on user screen unlock,
 * or when network connectivity transitions occur.
 */
class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "ServiceRestartReceiver triggered by action: $action")

        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isBackgroundMonitoringEnabled) {
            Log.d(TAG, "Background monitoring is disabled by user; skipping revive.")
            return
        }

        // On Android 13+ (API 33+), ensure notification permission is granted before starting FGS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                Log.w(TAG, "Cannot start SpeedTrackerService: Notification permission not granted")
                return
            }
        }

        if (!SpeedTrackerService.isServiceRunningState.value) {
            Log.i(TAG, "Reviving SpeedTrackerService from action: $action")
            try {
                SpeedTrackerService.start(context)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start service directly from receiver: ${e.message}", e)
                SpeedTrackerService.scheduleRestartAlarm(context, 2000L)
            }
        } else {
            Log.d(TAG, "SpeedTrackerService is already actively running")
        }
    }

    companion object {
        const val TAG = "ServiceRestartReceiver"
        const val ACTION_RESTART_SERVICE = "com.ayush.netpulse.action.RESTART_SERVICE"
    }
}
