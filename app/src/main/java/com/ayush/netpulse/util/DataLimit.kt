package com.ayush.netpulse.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure helpers for the monthly mobile-data limit (no Android classes, so they are unit-testable).
 */
object DataLimit {

    const val WARN_PERCENT = 80
    const val LIMIT_PERCENT = 100
    const val MIN_CYCLE_DAY = 1
    const val MAX_CYCLE_DAY = 28 // 28 so every month has that day

    const val BYTES_PER_GB = 1024L * 1024L * 1024L

    /** Start (local midnight) of the billing cycle that contains [nowMillis]. */
    fun cycleStartMillis(
        nowMillis: Long,
        cycleDay: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val day = cycleDay.coerceIn(MIN_CYCLE_DAY, MAX_CYCLE_DAY)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        if (cal.get(Calendar.DAY_OF_MONTH) < day) {
            cal.set(Calendar.DAY_OF_MONTH, 1) // avoid overflow when stepping back a month
            cal.add(Calendar.MONTH, -1)
        }
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Whole-number percent of the limit used (can exceed 100). 0 when no limit is set. */
    fun percentUsed(usedBytes: Long, limitBytes: Long): Int {
        if (limitBytes <= 0L || usedBytes <= 0L) return 0
        return ((usedBytes * 100.0) / limitBytes).toInt()
    }

    /** The alert level reached for this percent: 0 (none), 80 or 100. */
    fun alertLevel(percent: Int): Int = when {
        percent >= LIMIT_PERCENT -> LIMIT_PERCENT
        percent >= WARN_PERCENT -> WARN_PERCENT
        else -> 0
    }

    /** The level to notify now, or null if this level was already notified this cycle. */
    fun levelToNotify(percent: Int, alreadyNotified: Int): Int? {
        val level = alertLevel(percent)
        return if (level > alreadyNotified) level else null
    }

    fun gbToBytes(gb: Double): Long = (gb * BYTES_PER_GB).toLong()

    /** "5" for whole numbers, "2.5" otherwise. */
    fun formatGb(bytes: Long): String {
        val gb = bytes.toDouble() / BYTES_PER_GB
        return if (gb == Math.floor(gb)) gb.toLong().toString() else String.format(java.util.Locale.US, "%.1f", gb)
    }
}
