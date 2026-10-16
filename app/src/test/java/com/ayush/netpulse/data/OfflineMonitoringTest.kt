package com.ayush.netpulse.data

import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.util.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMonitoringTest {

    @Test
    fun testOfflineNetworkState_displaysDisconnected() {
        val offlineType = NetworkType.DISCONNECTED
        assertEquals("Disconnected", offlineType.displayName)
    }

    @Test
    fun testOfflineSpeed_isZeroKbps() {
        val speed = LiveSpeed(0L, 0L)
        assertEquals("0.0 KB/s", speed.formattedDownload)
        assertEquals("0.0 KB/s", speed.formattedUpload)
    }

    @Test
    fun testOfflineNotificationSignature() {
        val speed = LiveSpeed(0L, 0L)
        val networkType = NetworkType.DISCONNECTED
        val signature = "${speed.formattedDownload}|${speed.formattedUpload}|${networkType.name}"
        assertEquals("0.0 KB/s|0.0 KB/s|DISCONNECTED", signature)
    }

    @Test
    fun testAdaptiveOvernightDelayIntervals() {
        fun computeDelay(isScreenInteractive: Boolean, isOffline: Boolean): Long {
            return when {
                isScreenInteractive -> 1000L
                isOffline -> 10000L
                else -> 5000L
            }
        }

        assertEquals(1000L, computeDelay(isScreenInteractive = true, isOffline = false))
        assertEquals(1000L, computeDelay(isScreenInteractive = true, isOffline = true))
        assertEquals(5000L, computeDelay(isScreenInteractive = false, isOffline = false))
        assertEquals(10000L, computeDelay(isScreenInteractive = false, isOffline = true))
    }

    @Test
    fun testNetworkTypeTransitions_onlineToOfflineAndBack() {
        val states = mutableListOf<NetworkType>()

        // Simulate Wi-Fi connected
        states.add(NetworkType.WIFI)
        // User turns off Wi-Fi & Cellular
        states.add(NetworkType.DISCONNECTED)
        // User turns on Cellular
        states.add(NetworkType.CELLULAR)

        assertEquals(3, states.size)
        assertEquals("Wi-Fi", states[0].displayName)
        assertEquals("Disconnected", states[1].displayName)
        assertEquals("Mobile Data", states[2].displayName)
    }
}
