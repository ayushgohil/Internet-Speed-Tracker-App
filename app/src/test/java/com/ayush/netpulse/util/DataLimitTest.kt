package com.ayush.netpulse.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DataLimitTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 12): Long =
        Calendar.getInstance(utc).apply { clear(); set(y, m - 1, d, h, 0, 0) }.timeInMillis

    @Test
    fun cycleStartThisMonthWhenDayAlreadyPassed() {
        val start = DataLimit.cycleStartMillis(millis(2026, 11, 20), 15, utc)
        assertEquals(millis(2026, 11, 15, 0), start)
    }

    @Test
    fun cycleStartPreviousMonthWhenDayNotReachedYet() {
        val start = DataLimit.cycleStartMillis(millis(2026, 11, 3), 15, utc)
        assertEquals(millis(2026, 10, 15, 0), start)
    }

    @Test
    fun cycleStartCrossesYearBoundary() {
        val start = DataLimit.cycleStartMillis(millis(2027, 1, 5), 20, utc)
        assertEquals(millis(2026, 12, 20, 0), start)
    }

    @Test
    fun cycleDayOfOneStartsOnFirstOfMonth() {
        val start = DataLimit.cycleStartMillis(millis(2026, 3, 31), 1, utc)
        assertEquals(millis(2026, 3, 1, 0), start)
    }

    @Test
    fun cycleDayIsClampedTo28() {
        val start = DataLimit.cycleStartMillis(millis(2026, 3, 31), 31, utc)
        assertEquals(millis(2026, 3, 28, 0), start)
    }

    @Test
    fun percentUsed() {
        assertEquals(0, DataLimit.percentUsed(100, 0))
        assertEquals(50, DataLimit.percentUsed(500, 1000))
        assertEquals(125, DataLimit.percentUsed(1250, 1000))
    }

    @Test
    fun alertLevels() {
        assertEquals(0, DataLimit.alertLevel(79))
        assertEquals(80, DataLimit.alertLevel(80))
        assertEquals(80, DataLimit.alertLevel(99))
        assertEquals(100, DataLimit.alertLevel(100))
        assertEquals(100, DataLimit.alertLevel(240))
    }

    @Test
    fun eachAlertFiresOnlyOncePerCycle() {
        assertEquals(80, DataLimit.levelToNotify(85, 0))
        assertNull(DataLimit.levelToNotify(90, 80))
        assertEquals(100, DataLimit.levelToNotify(100, 80))
        assertNull(DataLimit.levelToNotify(130, 100))
        assertNull(DataLimit.levelToNotify(10, 0))
    }

    @Test
    fun formatGb() {
        assertEquals("5", DataLimit.formatGb(DataLimit.gbToBytes(5.0)))
        assertEquals("2.5", DataLimit.formatGb(DataLimit.gbToBytes(2.5)))
    }
}
