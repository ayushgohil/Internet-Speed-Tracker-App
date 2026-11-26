package com.ayush.netpulse.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ayush.netpulse.R
import com.ayush.netpulse.ui.MainActivity

/** Posts the "80% used" and "limit reached" alerts, once each per billing cycle. */
object DataLimitNotifier {

    private const val CHANNEL_ID = "netpulse_data_limit"
    private const val NOTIFICATION_ID = 2001

    @Synchronized
    fun check(context: Context, prefs: PreferencesManager) {
        val limit = prefs.dataLimitBytes
        if (limit <= 0L) return

        val now = System.currentTimeMillis()
        val cycleStart = DataLimit.cycleStartMillis(now, prefs.dataLimitCycleDay)
        val used = prefs.cachedCycleMobileBytes
        val percent = DataLimit.percentUsed(used, limit)

        val (savedCycle, savedLevel) = parseState(prefs.dataLimitAlertState)
        val alreadyNotified = if (savedCycle == cycleStart) savedLevel else 0
        val level = DataLimit.levelToNotify(percent, alreadyNotified) ?: return

        // Remember it first so a failed post doesn't cause a repeat every 30 seconds.
        prefs.dataLimitAlertState = "$cycleStart:$level"
        post(context, level, used, limit, percent)
    }

    @Synchronized
    fun checkDaily(context: Context, prefs: PreferencesManager, used: Long, now: Long) {
        val limit = prefs.dailyDataLimitBytes
        if (limit <= 0L) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val day = NetworkUtils.formatDateString(now)
        val state = prefs.dailyDataLimitAlertState.split(":")
        val previous = if (state.firstOrNull() == day) state.getOrNull(1)?.toIntOrNull() ?: 0 else 0
        val percent = DataLimit.percentUsed(used, limit)
        val level = DataLimit.levelToNotify(percent, previous) ?: return
        post(context, level, used, limit, percent, daily = true)
        prefs.dailyDataLimitAlertState = "$day:$level"
    }

    private fun post(context: Context, level: Int, used: Long, limit: Long, percent: Int, daily: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Data limit alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Warns when your daily or monthly mobile data limit is nearly used or reached"
                }
            )
        }

        val period = if (daily) "daily" else "monthly"
        val title = if (level >= DataLimit.LIMIT_PERCENT) "$period data limit reached" else "$percent% of $period data used"
        val text = "${NetworkUtils.formatBytes(used)} of ${NetworkUtils.formatBytes(limit)} mobile data ${if (daily) "today" else "this cycle"}"

        val open = PendingIntent.getActivity(
            context, 2001, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            manager.notify(if (daily) 2002 else NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun parseState(state: String): Pair<Long, Int> {
        val parts = state.split(":")
        val cycle = parts.getOrNull(0)?.toLongOrNull() ?: return 0L to 0
        val level = parts.getOrNull(1)?.toIntOrNull() ?: return 0L to 0
        return cycle to level
    }
}
