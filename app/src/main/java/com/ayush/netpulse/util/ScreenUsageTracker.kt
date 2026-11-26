package com.ayush.netpulse.util

import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks observed device traffic only, without attributing ambiguous intervals to either screen state. */
object ScreenUsageTracker {
    data class Totals(val date: String = "", val on: Long = 0, val off: Long = 0, val unknown: Long = 0)
    private val _totals = MutableStateFlow(Totals())
    val totals = _totals.asStateFlow()
    private var previousBytes = -1L
    private var previousTime = -1L
    private var previousInteractive: Boolean? = null
    private var previousDate = ""
    private var lastSave = 0L

    @Synchronized
    fun load(context: Context) {
        val day = NetworkUtils.formatDateString(System.currentTimeMillis())
        if (_totals.value.date == day) return
        val prefs = context.getSharedPreferences("netpulse_screen_usage", Context.MODE_PRIVATE)
        _totals.value = if (prefs.getString("day", "") == day)
            Totals(day, prefs.getLong("on", 0), prefs.getLong("off", 0), prefs.getLong("unknown", 0))
        else Totals(day)
    }

    @Synchronized
    fun sample(context: Context, interactive: Boolean) {
        val day = NetworkUtils.formatDateString(System.currentTimeMillis())
        val now = SystemClock.elapsedRealtime()
        load(context)
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val bytes = if (rx < 0 || tx < 0) -1L else rx + tx
        if (bytes >= 0 && previousBytes >= 0 && bytes >= previousBytes && previousDate == day) {
            val delta = bytes - previousBytes
            val totals = _totals.value
            _totals.value = when {
                previousTime < 0 || now - previousTime > 15_000L || previousInteractive != interactive ->
                    totals.copy(unknown = totals.unknown + delta)
                interactive -> totals.copy(on = totals.on + delta)
                else -> totals.copy(off = totals.off + delta)
            }
        }
        previousBytes = bytes
        previousTime = now
        previousInteractive = interactive
        previousDate = day
        if (now - lastSave >= 30_000L) {
            save(context)
            lastSave = now
        }
    }

    @Synchronized
    fun stop(context: Context) {
        save(context)
        previousBytes = -1
        previousTime = -1
        previousInteractive = null
        previousDate = ""
    }

    private fun save(context: Context) {
        val totals = _totals.value
        if (totals.date.isEmpty()) return
        context.getSharedPreferences("netpulse_screen_usage", Context.MODE_PRIVATE).edit()
            .putString("day", totals.date).putLong("on", totals.on).putLong("off", totals.off)
            .putLong("unknown", totals.unknown).apply()
    }
}
