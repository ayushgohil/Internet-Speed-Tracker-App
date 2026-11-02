package com.ayush.netpulse.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.ayush.netpulse.util.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.isBackgroundMonitoringEnabled) {
                // Android 13+ (API 33+) requires POST_NOTIFICATIONS to post foreground notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotif = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasNotif) {
                        val msg = "Boot restore skipped: Notification permission not granted."
                        Log.w("BootReceiver", msg)
                        prefs.lastKnownError = msg
                        return
                    }
                }

                Log.d("BootReceiver", "Boot completed or package replaced: restoring SpeedTrackerService")
                // System-held network callbacks don't survive a reboot or app update; register again.
                ServiceRevival.registerNetworkWakeup(context)
                try {
                    SpeedTrackerService.start(context, "boot")
                    prefs.lastKnownError = null
                } catch (e: Throwable) {
                    val err = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                        "Foreground service start restricted by system policy: ${e.message}"
                    } else {
                        "Failed to start service on boot: ${e.message}"
                    }
                    Log.e("BootReceiver", err, e)
                    prefs.lastKnownError = err
                }
            }
        }
    }
}
