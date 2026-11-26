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
    fun cycleDay31UsesLastDayOfShortMonth() {
        val start = DataLimit.cycleStartMillis(millis(2026, 3, 31), 31, utc)
        assertEquals(millis(2026, 3, 31, 0), start)
        assertEquals(millis(2026, 2, 28, 0), DataLimit.cycleStartMillis(millis(2026, 3, 1), 31, utc))
        assertEquals(millis(2028, 2, 29, 0), DataLimit.cycleStartMillis(millis(2028, 2, 29), 31, utc))
        assertEquals(millis(2028, 3, 31, 0), DataLimit.nextCycleMillis(millis(2028, 2, 29), 31, utc))
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
    @Test fun allowanceModesAndUnits() {
        assertEquals(-1L, DataLimit.gbToBytes(-1.0))
        assertEquals(0L, DataLimit.gbToBytes(Double.NaN))
        assertEquals(0, DataLimit.percentUsed(Long.MAX_VALUE, DataLimit.UNLIMITED))
        assertEquals(2L * DataLimit.BYTES_PER_GB, DataLimit.parseAllowance("2", "GB")!!)
        assertEquals(512L * 1024 * 1024, DataLimit.parseAllowance("512", "MB")!!)
        assertEquals(DataLimit.gbToBytes(1.5), DataLimit.parseAllowance("1,5", "GB")!!)
        assertNull(DataLimit.parseAllowance("Infinity", "GB"))
        assertNull(DataLimit.parseAllowance("-2", "GB"))
        assertNull(DataLimit.parseAllowance("0", "GB"))
        assertNull(DataLimit.parseAllowance("10001", "GB"))
        assertEquals(0L, DataLimit.remaining(120, 100))
        assertEquals(20L, DataLimit.overage(120, 100))
    }
}
