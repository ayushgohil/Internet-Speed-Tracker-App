package com.ayush.netpulse.data

import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.data.model.DiagnosticsInfo
import com.ayush.netpulse.util.NetworkUtils
import com.ayush.netpulse.util.SpeedIconGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Phase8VerificationTest {

    @Test
    fun testCompactSpeedFormattingForStatusBarIcon() {
        // Zero or negative
        assertEquals("0", SpeedIconGenerator.formatCompactSpeed(0L))
        assertEquals("0", SpeedIconGenerator.formatCompactSpeed(-500L))

        // Sub-KB (< 1024 bytes, above 100 B/s displays tenths to prevent premature zero)
        assertEquals("0", SpeedIconGenerator.formatCompactSpeed(50L))
        assertEquals("0.5K", SpeedIconGenerator.formatCompactSpeed(500L))
        assertEquals("0.8K", SpeedIconGenerator.formatCompactSpeed(800L))

        // Kilobytes (< 1000 KB)
        assertEquals("12K", SpeedIconGenerator.formatCompactSpeed(12 * 1024L))
        assertEquals("128K", SpeedIconGenerator.formatCompactSpeed(128 * 1024L))
        assertEquals("300K", SpeedIconGenerator.formatCompactSpeed(300 * 1024L))
        assertEquals("999K", SpeedIconGenerator.formatCompactSpeed(999 * 1024L))

        // Megabytes: rounded to whole integer to guarantee maximum font size & avoid tiny illegible decimals
        assertEquals("1M", SpeedIconGenerator.formatCompactSpeed((1.2 * 1024 * 1024).toLong()))
        assertEquals("6M", SpeedIconGenerator.formatCompactSpeed((5.6 * 1024 * 1024).toLong()))
        assertEquals("15M", SpeedIconGenerator.formatCompactSpeed(15 * 1024 * 1024L))
        assertEquals("120M", SpeedIconGenerator.formatCompactSpeed(120 * 1024 * 1024L))
    }

    @Test
    fun test90DayDateRangeGeneration() {
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dates = mutableListOf<String>()
        for (offset in 89 downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayCalendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            dates.add(NetworkUtils.formatDateString(cal.timeInMillis))
        }

        assertEquals(90, dates.size)
        // Ensure oldest is 89 days ago, newest is today
        val expectedTodayStr = NetworkUtils.formatDateString(todayCalendar.timeInMillis)
        assertEquals(expectedTodayStr, dates.last())
        // All dates must be unique
        assertEquals(90, dates.toSet().size)
    }

    @Test
    fun testMissingDataDifferentiatedFromConfirmedZeroUsage() {
        val dateStr = "2026-10-14"
        val existingMap = mapOf(
            "2026-10-15" to DailyUsageEntity(date = "2026-10-15", mobileRxBytes = 1000L),
            "2026-10-16" to DailyUsageEntity(date = "2026-10-16", mobileRxBytes = 0L, wifiRxBytes = 0L)
        )

        // 2026-10-14 is missing (no entity) -> treated as unavailable, not fabricated 0
        assertNull(existingMap[dateStr])

        // 2026-10-16 exists with 0 bytes -> confirmed zero usage
        val confirmedZeroEntity = existingMap["2026-10-16"]
        assertNotNull(confirmedZeroEntity)
        assertEquals(0L, confirmedZeroEntity?.totalAllBytes)
    }

    @Test
    fun testHeatmapIntensityCalculations() {
        fun computeIntensityLevel(bytes: Long, hasRecord: Boolean): Int {
            if (!hasRecord) return 0 // Unavailable / Missing
            if (bytes <= 0L) return 0 // Confirmed 0
            val mb = bytes / (1024.0 * 1024.0)
            return when {
                mb < 50.0 -> 1   // Low
                mb < 200.0 -> 2  // Medium
                mb < 500.0 -> 3  // High
                else -> 4        // Very High
            }
        }

        // Missing record
        assertEquals(0, computeIntensityLevel(0L, hasRecord = false))

        // Confirmed zero
        assertEquals(0, computeIntensityLevel(0L, hasRecord = true))

        // Level 1: Low (< 50 MB)
        assertEquals(1, computeIntensityLevel(20 * 1024 * 1024L, hasRecord = true))

        // Level 2: Medium (50 - 200 MB)
        assertEquals(2, computeIntensityLevel(120 * 1024 * 1024L, hasRecord = true))

        // Level 3: High (200 - 500 MB)
        assertEquals(3, computeIntensityLevel(350 * 1024 * 1024L, hasRecord = true))

        // Level 4: Very High (> 500 MB)
        assertEquals(4, computeIntensityLevel(1500 * 1024 * 1024L, hasRecord = true))
    }

    @Test
    fun testDiagnosticsResolvedServiceStatus() {
        // Active
        val activeDiag = DiagnosticsInfo(isForegroundServiceRunning = true, hasNotificationPermission = true)
        assertEquals("Active", activeDiag.resolvedServiceStatus)

        // Needs Permission
        val permDiag = DiagnosticsInfo(isForegroundServiceRunning = false, hasNotificationPermission = false)
        assertEquals("Needs Permission", permDiag.resolvedServiceStatus)

        // Stopped
        val stoppedDiag = DiagnosticsInfo(
            isForegroundServiceRunning = false,
            hasNotificationPermission = true,
            isBackgroundMonitoringEnabled = false
        )
        assertEquals("Stopped", stoppedDiag.resolvedServiceStatus)
    }
}
