package com.ayush.netpulse.util

import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeedSmootherTest {

    private lateinit var smoother: SpeedSmoother

    @Before
    fun setUp() {
        smoother = SpeedSmoother(windowSize = 3)
    }

    @Test
    fun testSustainedTrafficProducesConsistentAverage() {
        val sustainedDown = 100 * 1024L // 100 KB/s
        val sustainedUp = 20 * 1024L // 20 KB/s

        // Sample 1
        val s1 = smoother.smooth(LiveSpeed(sustainedDown, sustainedUp), NetworkType.WIFI)
        assertEquals(sustainedDown, s1.downloadBytesPerSec)
        assertEquals(sustainedUp, s1.uploadBytesPerSec)

        // Sample 2
        val s2 = smoother.smooth(LiveSpeed(sustainedDown, sustainedUp), NetworkType.WIFI)
        assertEquals(sustainedDown, s2.downloadBytesPerSec)
        assertEquals(sustainedUp, s2.uploadBytesPerSec)

        // Sample 3
        val s3 = smoother.smooth(LiveSpeed(sustainedDown, sustainedUp), NetworkType.WIFI)
        assertEquals(sustainedDown, s3.downloadBytesPerSec)
        assertEquals(sustainedUp, s3.uploadBytesPerSec)
    }

    @Test
    fun testBurstyTrafficIsSmoothedOverWindow() {
        // Sample 1: Idle
        smoother.smooth(LiveSpeed(0L, 0L), NetworkType.WIFI)

        // Sample 2: Sudden burst of 300 KB/s
        val burst = smoother.smooth(LiveSpeed(300 * 1024L, 0L), NetworkType.WIFI)
        // Average of (0 + 300) / 2 = 150 KB/s
        assertEquals(150 * 1024L, burst.downloadBytesPerSec)

        // Sample 3: Back down to 0 KB/s
        val afterBurst = smoother.smooth(LiveSpeed(0L, 0L), NetworkType.WIFI)
        // Average of (0 + 300 + 0) / 3 = 100 KB/s
        assertEquals(100 * 1024L, afterBurst.downloadBytesPerSec)

        // Sample 4: Still 0 KB/s -> Second consecutive zero immediately resets to avoid stale reading
        val secondZero = smoother.smooth(LiveSpeed(0L, 0L), NetworkType.WIFI)
        assertEquals(0L, secondZero.downloadBytesPerSec)
    }

    @Test
    fun testImmediateZeroWhenDisconnectedWithoutStaleSmoothing() {
        // Fill window with active 500 KB/s
        smoother.smooth(LiveSpeed(500 * 1024L, 50 * 1024L), NetworkType.WIFI)
        smoother.smooth(LiveSpeed(500 * 1024L, 50 * 1024L), NetworkType.WIFI)

        // Network disconnects
        val offline = smoother.smooth(LiveSpeed(0L, 0L), NetworkType.DISCONNECTED)
        assertEquals(0L, offline.downloadBytesPerSec)
        assertEquals(0L, offline.uploadBytesPerSec)
    }

    @Test
    fun testHistoryResetsOnTransportChange() {
        // High Wi-Fi traffic
        smoother.smooth(LiveSpeed(800 * 1024L, 100 * 1024L), NetworkType.WIFI)
        smoother.smooth(LiveSpeed(800 * 1024L, 100 * 1024L), NetworkType.WIFI)

        // Transport switches to Cellular with low traffic (10 KB/s)
        val switched = smoother.smooth(LiveSpeed(10 * 1024L, 2 * 1024L), NetworkType.CELLULAR)
        // Must NOT average with the previous 800 KB/s Wi-Fi samples
        assertEquals(10 * 1024L, switched.downloadBytesPerSec)
        assertEquals(2 * 1024L, switched.uploadBytesPerSec)
    }

    @Test
    fun testSmallNonZeroTrafficNeverPrematurelyRoundedToZero() {
        // Small 200 B/s traffic with empty or low history
        val smallSpeed = smoother.smooth(LiveSpeed(200L, 150L), NetworkType.CELLULAR)
        assertTrue(smallSpeed.downloadBytesPerSec > 0L)
        assertTrue(smallSpeed.uploadBytesPerSec > 0L)
    }

    @Test
    fun testUploadTrafficIndependentSmoothing() {
        // Only upload traffic, download 0
        val s1 = smoother.smooth(LiveSpeed(0L, 60 * 1024L), NetworkType.WIFI)
        assertEquals(0L, s1.downloadBytesPerSec)
        assertEquals(60 * 1024L, s1.uploadBytesPerSec)

        val s2 = smoother.smooth(LiveSpeed(0L, 120 * 1024L), NetworkType.WIFI)
        assertEquals(0L, s2.downloadBytesPerSec)
        assertEquals(90 * 1024L, s2.uploadBytesPerSec) // (60 + 120) / 2 = 90
    }

    @Test
    fun testAsymmetricDownloadUploadRemainsCompletelyIndependent() {
        // Case A: High Download (800 KB/s), Low Upload (20 KB/s)
        val down800 = 800 * 1024L
        val up20 = 20 * 1024L
        val resA = smoother.smooth(LiveSpeed(down800, up20), NetworkType.WIFI)
        assertEquals(down800, resA.downloadBytesPerSec)
        assertEquals(up20, resA.uploadBytesPerSec)
        assertEquals("800.0 KB/s", resA.formattedDownload)
        assertEquals("20.0 KB/s", resA.formattedUpload)

        // Case B: Low Download (5 KB/s), High Upload (600 KB/s)
        val down5 = 5 * 1024L
        val up600 = 600 * 1024L
        smoother.reset()
        val resB = smoother.smooth(LiveSpeed(down5, up600), NetworkType.CELLULAR)
        assertEquals(down5, resB.downloadBytesPerSec)
        assertEquals(up600, resB.uploadBytesPerSec)
        assertEquals("5.0 KB/s", resB.formattedDownload)
        assertEquals("600.0 KB/s", resB.formattedUpload)
    }
}
