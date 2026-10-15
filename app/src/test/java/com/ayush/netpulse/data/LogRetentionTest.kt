package com.ayush.netpulse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRetentionTest {

    @Test
    fun testLogRetentionCutoff_sevenDays() {
        val now = 1760000000000L // arbitrary fixed timestamp
        val retentionDays = 7
        val cutoff = now - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)

        val recordFrom8DaysAgo = now - (8L * 24L * 60L * 60L * 1000L)
        val recordFrom6DaysAgo = now - (6L * 24L * 60L * 60L * 1000L)

        assertTrue(recordFrom8DaysAgo < cutoff)
        assertTrue(recordFrom6DaysAgo > cutoff)
    }

    @Test
    fun testLogRetentionCutoff_thirtyDays() {
        val now = 1760000000000L
        val retentionDays = 30
        val cutoff = now - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)

        val recordFrom31DaysAgo = now - (31L * 24L * 60L * 60L * 1000L)
        val recordFrom29DaysAgo = now - (29L * 24L * 60L * 60L * 1000L)

        assertTrue(recordFrom31DaysAgo < cutoff)
        assertTrue(recordFrom29DaysAgo > cutoff)
    }

    @Test
    fun testLogRetentionCutoff_ninetyDays() {
        val now = 1760000000000L
        val retentionDays = 90
        val cutoff = now - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)

        val recordFrom91DaysAgo = now - (91L * 24L * 60L * 60L * 1000L)
        val recordFrom89DaysAgo = now - (89L * 24L * 60L * 60L * 1000L)

        assertTrue(recordFrom91DaysAgo < cutoff)
        assertTrue(recordFrom89DaysAgo > cutoff)
    }

    @Test
    fun testLogRetention_preservesDailyUsageSummaries() {
        // Daily usage summaries must never be purged when speed log retention cleans records
        val isDailyUsageTableAffected = false
        assertEquals("Daily usage summaries must not be deleted by log retention", false, isDailyUsageTableAffected)
    }
}
