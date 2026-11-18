package com.ayush.netpulse.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ayush.netpulse.util.PreferencesManager

/**
 * Single owner of boot and app-update recovery.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isBackgroundMonitoringEnabled) {
            Log.d(TAG, "Monitoring was disabled by the user; skipping boot recovery.")
            return
        }

        Log.d(TAG, "Boot completed or package replaced: restoring SpeedTrackerService")

        // System-held network callbacks don't survive a reboot or app update; register again.
        ServiceRevival.registerNetworkWakeup(context)
        try {
            SpeedTrackerService.start(context, "boot")
        } catch (e: Throwable) {
            val err = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                "Foreground service start restricted by system policy: ${e.message}"
            } else {
                "Failed to start service on boot: ${e.message}"
            }
            Log.e(TAG, err, e)
            prefs.lastKnownError = err
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
