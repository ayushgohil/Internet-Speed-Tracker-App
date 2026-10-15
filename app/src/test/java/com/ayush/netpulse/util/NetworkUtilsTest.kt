package com.ayush.netpulse.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun testFormatSpeed_zeroOrNegative() {
        assertEquals("0.0 KB/s", NetworkUtils.formatSpeed(0L))
        assertEquals("0.0 KB/s", NetworkUtils.formatSpeed(-100L))
    }

    @Test
    fun testFormatSpeed_kilobytes() {
        assertEquals("500.0 KB/s", NetworkUtils.formatSpeed(500 * 1024L))
    }

    @Test
    fun testFormatSpeed_megabytes() {
        assertEquals("2.5 MB/s", NetworkUtils.formatSpeed((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatBytes_megabytesAndGigabytes() {
        assertEquals("500.0 MB", NetworkUtils.formatBytes(500 * 1024 * 1024L))
        assertEquals("1.50 GB", NetworkUtils.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatBytes_bytesAndKilobytes() {
        assertEquals("0.0 MB", NetworkUtils.formatBytes(0L))
        assertEquals("500 B", NetworkUtils.formatBytes(500L))
        assertEquals("10.0 KB", NetworkUtils.formatBytes(10 * 1024L))
    }

    @Test
    fun testFormatSpeed_gigabytes() {
        assertEquals("1.20 GB/s", NetworkUtils.formatSpeed((1.2 * 1024 * 1024 * 1024).toLong()))
    }
}
