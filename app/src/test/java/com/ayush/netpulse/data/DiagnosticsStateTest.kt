package com.ayush.netpulse.data

import com.ayush.netpulse.data.model.DiagnosticsInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsStateTest {

    @Test
    fun testDiagnosticsState_activeWhenForegroundServiceRunning() {
        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = true,
            isForegroundServiceRunning = true,
            hasNotificationPermission = true
        )
        assertEquals("Active", diag.resolvedServiceStatus)
    }

    @Test
    fun testDiagnosticsState_stoppedWhenMonitoringDisabled() {
        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = false,
            isForegroundServiceRunning = false,
            hasNotificationPermission = true
        )
        assertEquals("Stopped", diag.resolvedServiceStatus)
    }

    @Test
    fun testDiagnosticsState_needsPermissionWhenNotificationMissing() {
        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = true,
            isForegroundServiceRunning = false,
            hasNotificationPermission = false
        )
        assertEquals("Needs Permission", diag.resolvedServiceStatus)
    }

    @Test
    fun testDiagnosticsState_unknownWhenEnabledInPrefsButNotRunning() {
        // Do not claim service is running merely because monitoring is enabled in prefs
        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = true,
            isForegroundServiceRunning = false,
            hasNotificationPermission = true
        )
        assertEquals("Unknown", diag.resolvedServiceStatus)
    }

    @Test
    fun testDiagnosticsState_staleFlagIsPreserved() {
        val diag = DiagnosticsInfo(
            isBackgroundMonitoringEnabled = true,
            isForegroundServiceRunning = true,
            isServiceStatePotentiallyStale = true,
            hasNotificationPermission = true
        )
        assertEquals("Active", diag.resolvedServiceStatus)
        assertEquals(true, diag.isServiceStatePotentiallyStale)
    }
}
