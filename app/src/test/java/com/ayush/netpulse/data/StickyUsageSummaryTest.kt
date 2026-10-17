package com.ayush.netpulse.data

import com.ayush.netpulse.data.local.DailyUsageEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StickyUsageSummaryTest {

    private val oneGb = 1024L * 1024L * 1024L
    private val halfGb = 512L * 1024L * 1024L

    @Test
    fun testUsageSummaryAggregation_CalculatesAccurately() {
        val records = listOf(
            DailyUsageEntity(
                date = "2026-10-15",
                mobileRxBytes = 1 * oneGb,
                mobileTxBytes = halfGb,
                wifiRxBytes = 2 * oneGb,
                wifiTxBytes = 1 * oneGb
            ),
            DailyUsageEntity(
                date = "2026-10-16",
                mobileRxBytes = 2 * oneGb,
                mobileTxBytes = halfGb,
                wifiRxBytes = 3 * oneGb,
                wifiTxBytes = halfGb
            )
        )

        var totalMobile = 0L
        var totalWifi = 0L
        var activeDays = 0

        records.forEach {
            totalMobile += it.totalMobileBytes
            totalWifi += it.totalWifiBytes
            if (it.totalAllBytes > 0) activeDays++
        }

        // Day 1: Mobile = 1.5 GB, Wifi = 3.0 GB
        // Day 2: Mobile = 2.5 GB, Wifi = 3.5 GB
        // Total Mobile = 4.0 GB, Total Wifi = 6.5 GB, Combined = 10.5 GB
        val expectedMobile = 4L * oneGb
        val expectedWifi = 6 * oneGb + halfGb

        assertEquals(expectedMobile, totalMobile)
        assertEquals(expectedWifi, totalWifi)
        assertEquals(expectedMobile + expectedWifi, totalMobile + totalWifi)
        assertEquals(2, activeDays)
    }
}
