package com.ayush.netpulse.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ayush.netpulse.util.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.isBackgroundMonitoringEnabled) {
                Log.d("BootReceiver", "Boot completed or package replaced: restoring SpeedTrackerService")
                try {
                    SpeedTrackerService.start(context)
                } catch (e: Throwable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                        Log.w("BootReceiver", "Foreground service start restricted by system policy: ${e.message}")
                    } else {
                        Log.e("BootReceiver", "Failed to start service on boot: ${e.message}", e)
                    }
                }
            }
        }
    }
}
