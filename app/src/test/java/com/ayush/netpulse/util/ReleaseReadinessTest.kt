package com.ayush.netpulse.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReleaseReadinessTest {
    @Test fun orderNormalizesUnknownAndDuplicateKeys() {
        assertEquals(listOf("quick_actions", "usage_insights", "network_details"),
            DashboardOrder.parse("quick_actions,unknown,quick_actions"))
        assertEquals(DashboardOrder.defaults, DashboardOrder.parse(null))
    }

    @Test fun movingCardsPreservesEveryCard() {
        val original = DashboardOrder.defaults
        val moved = DashboardOrder.move(original, 0, 2)
        assertEquals(listOf("network_details", "quick_actions", "usage_insights"), moved)
        assertEquals(original, DashboardOrder.move(original, 0, -1))
        assertEquals(original, DashboardOrder.move(original, 2, 1))
    }

    @Test fun dataLimitThresholdsDoNotRepeat() {
        assertNull(DataLimit.levelToNotify(79, 0))
        assertEquals(80, DataLimit.levelToNotify(80, 0))
        assertNull(DataLimit.levelToNotify(99, 80))
        assertEquals(100, DataLimit.levelToNotify(100, 80))
        assertNull(DataLimit.levelToNotify(120, 100))
    }

    @Test fun billingCycleCrossesYearBoundary() {
        val zone = TimeZone.getTimeZone("UTC")
        val now = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.JANUARY, 2, 12, 0) }
        val expected = Calendar.getInstance(zone).apply { clear(); set(2025, Calendar.DECEMBER, 15) }
        assertEquals(expected.timeInMillis, DataLimit.cycleStartMillis(now.timeInMillis, 15, zone))
    }
}
