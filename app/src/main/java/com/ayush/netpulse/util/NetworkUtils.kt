package com.ayush.netpulse.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Locale

object NetworkUtils {

    /**
     * Formats bytes into a human-readable data transfer speed (e.g. "1.2 MB/s", "450 KB/s")
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0.0 KB/s"
        val kb = bytesPerSec / 1024.0
        return when {
            kb >= 1024 * 1024 -> String.format(Locale.US, "%.2f GB/s", kb / (1024 * 1024))
            kb >= 1024 -> String.format(Locale.US, "%.1f MB/s", kb / 1024.0)
            else -> String.format(Locale.US, "%.1f KB/s", kb)
        }
    }

    /**
     * Formats total bytes into human-readable data size (e.g. "450 MB", "2.4 GB")
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0.0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Checks if the PACKAGE_USAGE_STATS special permission is granted via AppOpsManager
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Creates an intent to navigate the user to the Usage Access settings screen
     */
    fun createUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
