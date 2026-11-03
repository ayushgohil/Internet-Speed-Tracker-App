package com.ayush.netpulse.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.R
import com.ayush.netpulse.ui.MainActivity
import com.ayush.netpulse.util.DataLimit
import com.ayush.netpulse.util.NetworkUtils
import com.ayush.netpulse.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Home-screen widget showing today's mobile / Wi-Fi usage and, if set, the monthly data limit. */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Draw what we already know straight away, then refresh the numbers in the background.
        render(context, manager, appWidgetIds)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // On success the repository calls refresh() again with the new numbers.
                NetPulseApp.instance.networkRepository.getTodayDataUsage(forceReconcile = false)
            } catch (_: Throwable) {
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** Re-draws every placed widget from the cached usage numbers. */
        fun refresh(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
                if (ids.isNotEmpty()) render(context, manager, ids)
            } catch (_: Throwable) {
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val prefs = PreferencesManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.widget_usage)

            val hasPermission = NetworkUtils.hasUsageStatsPermission(context)
            val isToday = prefs.cachedTodayDate == NetworkUtils.formatDateString()
            val mobile = if (isToday) prefs.cachedTodayMobileBytes else 0L
            val wifi = if (isToday) prefs.cachedTodayWifiBytes else 0L

            if (!hasPermission) {
                views.setTextViewText(R.id.widget_total, "--")
                views.setTextViewText(R.id.widget_detail, "Tap to allow Usage Access")
            } else {
                views.setTextViewText(R.id.widget_total, NetworkUtils.formatBytes(mobile + wifi))
                views.setTextViewText(
                    R.id.widget_detail,
                    "Mobile ${NetworkUtils.formatBytes(mobile)} · Wi-Fi ${NetworkUtils.formatBytes(wifi)}"
                )
            }

            val limit = prefs.dataLimitBytes
            if (hasPermission && limit > 0L) {
                val used = prefs.cachedCycleMobileBytes
                val percent = DataLimit.percentUsed(used, limit)
                views.setTextViewText(
                    R.id.widget_limit_text,
                    "Plan: ${NetworkUtils.formatBytes(used)} of ${DataLimit.formatGb(limit)} GB ($percent%)"
                )
                views.setProgressBar(R.id.widget_limit_progress, 100, percent.coerceAtMost(100), false)
                views.setViewVisibility(R.id.widget_limit_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_limit_progress, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_limit_text, View.GONE)
                views.setViewVisibility(R.id.widget_limit_progress, View.GONE)
            }

            val open = PendingIntent.getActivity(
                context,
                3001,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            manager.updateAppWidget(ids, views)
        }
    }
}
