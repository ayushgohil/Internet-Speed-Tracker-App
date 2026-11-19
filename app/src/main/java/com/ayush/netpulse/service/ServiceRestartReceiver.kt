package com.ayush.netpulse.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ayush.netpulse.util.PreferencesManager

/**
 * Revives SpeedTrackerService after restart alarms, screen unlock, or network transitions.
 * Boot and app-update recovery are owned exclusively by BootReceiver.
 */
class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "ServiceRestartReceiver triggered by action: $action")

        val prefs = PreferencesManager.getInstance(context)
        prefs.appendRevivalLog("receiver:${action.substringAfterLast('.')}", "delivered")
        if (!prefs.isBackgroundMonitoringEnabled) {
            Log.d(TAG, "Background monitoring is disabled by user; skipping revive.")
            prefs.appendRevivalLog("receiver", "skipped (monitoring disabled)")
            return
        }

        // POST_NOTIFICATIONS controls notification-drawer visibility on Android 13+;
        // it is not a prerequisite for starting a foreground service.
        if (!SpeedTrackerService.isServiceRunningState.value) {
            Log.i(TAG, "Reviving SpeedTrackerService from action: $action")
            val source = when (action) {
                ServiceRevival.ACTION_NETWORK_CHANGED -> "network-back"
                ACTION_RESTART_SERVICE -> "retry-alarm"
                Intent.ACTION_USER_PRESENT -> "screen-unlock"
                else -> "receiver:${action.substringAfterLast('.')}"
            }
            // start() handles blocked starts itself (backoff retry + tap-to-resume notification).
            prefs.appendRevivalLog(source, "start requested by receiver")
            SpeedTrackerService.start(context, source)
        } else {
            Log.d(TAG, "SpeedTrackerService is already actively running")
            prefs.appendRevivalLog("receiver", "skipped (service state already running)")
        }
    }

    companion object {
        const val TAG = "ServiceRestartReceiver"
        const val ACTION_RESTART_SERVICE = "com.ayush.netpulse.action.RESTART_SERVICE"
    }
}
