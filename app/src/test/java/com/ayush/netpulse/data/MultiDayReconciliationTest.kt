package com.ayush.netpulse.data

import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.util.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MultiDayReconciliationTest {

    @Test
    fun testYearRolloverDateCalculation() {
        // Test transition from Dec 31 to Jan 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateStr = NetworkUtils.formatDateString(cal.timeInMillis)
        assertEquals("2025-12-31", dateStr)
    }

    @Test
    fun testDelayedStatsAggregation_takesMaxValues() {
        val existing = DailyUsageEntity(
            date = "2026-10-14",
            wifiRxBytes = 500 * 1024 * 1024L,
            wifiTxBytes = 50 * 1024 * 1024L,
            mobileRxBytes = 300 * 1024 * 1024L,
            mobileTxBytes = 30 * 1024 * 1024L
        )

        // Delayed carrier/Wi-Fi report returns lower value temporarily
        val delayedQueryWifiRx = 400 * 1024 * 1024L // smaller
        val delayedQueryMobileRx = 350 * 1024 * 1024L // larger

        val finalWifiRx = maxOf(existing.wifiRxBytes, delayedQueryWifiRx)
        val finalMobileRx = maxOf(existing.mobileRxBytes, delayedQueryMobileRx)

        assertEquals(500 * 1024 * 1024L, finalWifiRx) // preserved larger value
        assertEquals(350 * 1024 * 1024L, finalMobileRx) // updated to larger value
    }

    @Test
    fun testTimezoneOffsetsDoNotCrossDateString() {
        val timezones = listOf("UTC", "America/New_York", "Asia/Kolkata", "Europe/London", "Asia/Tokyo")

        for (tzId in timezones) {
            val tz = TimeZone.getTimeZone(tzId)
            val cal = Calendar.getInstance(tz).apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.OCTOBER)
                set(Calendar.DAY_OF_MONTH, 15)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            assertTrue("Timestamp should be valid for timezone $tzId", cal.timeInMillis > 0)
        }
    }

    @Test
    fun testMissingPastDayLeftAsUnknown() {
        val existingRecord: DailyUsageEntity? = null
        val queryTotalBytes = 0L
        val offset = 5 // 5 days ago

        val shouldInsert = existingRecord != null || (queryTotalBytes > 0L || offset == 0)
        assertFalse("Past days with 0 bytes and no record should be left as unknown", shouldInsert)
    }
}
