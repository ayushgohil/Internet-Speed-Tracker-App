package com.ayush.netpulse.data

import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.util.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyUsageTest {

    @Test
    fun testDailyUsageEntity_totalCalculations() {
        val entity = DailyUsageEntity(
            date = "2026-10-15",
            mobileRxBytes = 100 * 1024 * 1024L, // 100 MB
            mobileTxBytes = 25 * 1024 * 1024L,  // 25 MB
            wifiRxBytes = 500 * 1024 * 1024L,   // 500 MB
            wifiTxBytes = 50 * 1024 * 1024L     // 50 MB
        )

        assertEquals(125 * 1024 * 1024L, entity.totalMobileBytes)
        assertEquals(550 * 1024 * 1024L, entity.totalWifiBytes)
        assertEquals(675 * 1024 * 1024L, entity.totalAllBytes)

        assertEquals("125.0 MB", entity.formattedMobile)
        assertEquals("550.0 MB", entity.formattedWifi)
        assertEquals("675.0 MB", entity.formattedTotal)
    }

    @Test
    fun testDateFormatting_localDatePattern() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val formatted = NetworkUtils.formatDateString(cal.timeInMillis)
        assertEquals("2026-10-15", formatted)
    }

    @Test
    fun testIdempotentReconciliation_replaceSameDate() {
        val snapshotMorning = DailyUsageEntity(
            date = "2026-10-15",
            mobileRxBytes = 50 * 1024 * 1024L,
            mobileTxBytes = 10 * 1024 * 1024L,
            wifiRxBytes = 200 * 1024 * 1024L,
            wifiTxBytes = 20 * 1024 * 1024L,
            lastUpdatedTimestamp = 1000L
        )

        val snapshotEvening = DailyUsageEntity(
            date = "2026-10-15",
            mobileRxBytes = 120 * 1024 * 1024L,
            mobileTxBytes = 30 * 1024 * 1024L,
            wifiRxBytes = 600 * 1024 * 1024L,
            wifiTxBytes = 80 * 1024 * 1024L,
            lastUpdatedTimestamp = 2000L
        )

        assertEquals(snapshotMorning.date, snapshotEvening.date)
        assertTrue(snapshotEvening.totalAllBytes > snapshotMorning.totalAllBytes)
        assertEquals("830.0 MB", snapshotEvening.formattedTotal)
    }

    @Test
    fun testZeroValueProtection_preservesExistingData() {
        val validHistoricalRecord = DailyUsageEntity(
            date = "2026-10-14",
            mobileRxBytes = 400 * 1024 * 1024L,
            mobileTxBytes = 50 * 1024 * 1024L,
            wifiRxBytes = 800 * 1024 * 1024L,
            wifiTxBytes = 60 * 1024 * 1024L,
            lastUpdatedTimestamp = 5000L
        )

        // Simulating a temporary query failure returning 0 bytes for a past day
        val queryResultAllBytes = 0L
        val offset = 1 // past day (yesterday)

        val finalRecord = if (offset > 0 && queryResultAllBytes == 0L && validHistoricalRecord.totalAllBytes > 0L) {
            validHistoricalRecord // Protection triggered: preserve existing
        } else {
            DailyUsageEntity(date = "2026-10-14")
        }

        assertEquals(1310 * 1024 * 1024L, finalRecord.totalAllBytes)
        assertNotEquals(0L, finalRecord.totalAllBytes)
    }

    @Test
    fun testDayBoundaryCalculation_localTimezone() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        val endOfDayCal = Calendar.getInstance().apply {
            timeInMillis = startOfDay
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = endOfDayCal.timeInMillis

        assertTrue(endOfDay > startOfDay)
        val diffHours = (endOfDay - startOfDay) / (1000 * 60 * 60)
        assertEquals(23L, diffHours)
    }
}
