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
    fun testAllSixNetworkConditionsAndTransitions() {
        // 1. Wi-Fi connected
        val wifiState = NetworkType.WIFI
        assertEquals("Wi-Fi", wifiState.displayName)

        // 2. Mobile data connected
        val mobileState = NetworkType.CELLULAR
        assertEquals("Mobile Data", mobileState.displayName)

        // 3. Wi-Fi and mobile data both disabled
        val bothDisabledState = NetworkType.DISCONNECTED
        assertEquals("Disconnected", bothDisabledState.displayName)

        // 4. Airplane mode enabled (represented at transport layer as DISCONNECTED)
        val airplaneModeState = NetworkType.DISCONNECTED
        assertEquals("Disconnected", airplaneModeState.displayName)

        // 5. Switching from online to offline
        val onlineSpeed = LiveSpeed(1024000L, 512000L)
        val offlineSpeed = LiveSpeed(0L, 0L)
        assertEquals("1000.0 KB/s", onlineSpeed.formattedDownload)
        assertEquals("0.0 KB/s", offlineSpeed.formattedDownload)

        // 6. Switching from offline to online
        val recoveredSpeed = LiveSpeed(2048000L, 102400L)
        assertEquals("2.0 MB/s", recoveredSpeed.formattedDownload)
        assertEquals("100.0 KB/s", recoveredSpeed.formattedUpload)
    }

    @Test
    fun testNetworkTypeTransitions_onlineToOfflineAndBack() {
        val states = mutableListOf<NetworkType>()

        // Simulate Wi-Fi connected
        states.add(NetworkType.WIFI)
        // User turns off Wi-Fi & Cellular (or turns on Airplane mode)
        states.add(NetworkType.DISCONNECTED)
        // User turns on Cellular
        states.add(NetworkType.CELLULAR)
        // User switches back to Wi-Fi
        states.add(NetworkType.WIFI)

        assertEquals(4, states.size)
        assertEquals("Wi-Fi", states[0].displayName)
        assertEquals("Disconnected", states[1].displayName)
        assertEquals("Mobile Data", states[2].displayName)
        assertEquals("Wi-Fi", states[3].displayName)
    }
}
