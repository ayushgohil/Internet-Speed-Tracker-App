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
    const val MAX_CYCLE_DAY = 31
    const val UNLIMITED = -1L
    const val MAX_GB = 10000.0

    const val BYTES_PER_GB = 1024L * 1024L * 1024L

    /** Start (local midnight) of the billing cycle that contains [nowMillis]. */
    fun cycleStartMillis(
        nowMillis: Long,
        cycleDay: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val day = cycleDay.coerceIn(MIN_CYCLE_DAY, MAX_CYCLE_DAY)
        val date = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone.toZoneId()).toLocalDate()
        val month = java.time.YearMonth.from(date)
        fun boundary(m: java.time.YearMonth) = m.atDay(day.coerceAtMost(m.lengthOfMonth()))
        val start = if (date < boundary(month)) boundary(month.minusMonths(1)) else boundary(month)
        return start.atStartOfDay(zone.toZoneId()).toInstant().toEpochMilli()
    }

    fun nextCycleMillis(nowMillis: Long, cycleDay: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val start = java.time.Instant.ofEpochMilli(cycleStartMillis(nowMillis, cycleDay, zone)).atZone(zone.toZoneId()).toLocalDate()
        val month = java.time.YearMonth.from(start).plusMonths(1)
        return month.atDay(cycleDay.coerceIn(1, month.lengthOfMonth())).atStartOfDay(zone.toZoneId()).toInstant().toEpochMilli()
    }

    fun parseAllowance(input: String, unit: String): Long? {
        val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0) return null
        val multiplier = if (unit == "MB") 1024.0 * 1024 else BYTES_PER_GB.toDouble()
        val bytes = value * multiplier
        return if (bytes >= 1 && bytes <= MAX_GB * BYTES_PER_GB) bytes.toLong() else null
    }

    fun remaining(used: Long, limit: Long): Long = if (limit > 0) (limit - used.coerceAtLeast(0)).coerceAtLeast(0) else 0
    fun overage(used: Long, limit: Long): Long = if (limit > 0) (used.coerceAtLeast(0) - limit).coerceAtLeast(0) else 0
    fun label(bytes: Long): String = when {
        bytes == UNLIMITED -> "Unlimited"
        bytes <= 0 -> "Off"
        else -> formatGb(bytes) + " GB"
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

    fun gbToBytes(gb: Double): Long = when {
        gb == -1.0 -> UNLIMITED
        !gb.isFinite() || gb <= 0 -> 0L
        else -> (gb.coerceAtMost(MAX_GB) * BYTES_PER_GB).toLong()
    }

    /** "5" for whole numbers, "2.5" otherwise. */
    fun formatGb(bytes: Long): String {
        val gb = bytes.toDouble() / BYTES_PER_GB
        return java.math.BigDecimal.valueOf(gb).stripTrailingZeros().toPlainString()
    }
}
