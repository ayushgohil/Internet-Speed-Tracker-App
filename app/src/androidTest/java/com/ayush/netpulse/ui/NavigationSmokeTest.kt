package com.ayush.netpulse.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.theme.NetPulseTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class NavigationSmokeTest {
    @get:Rule val compose = createComposeRule()

    private fun show(theme: String, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                NetPulseTheme(appTheme = theme) {
                    MainScreen(
                        uiState = DashboardUiState(isBatteryOptimizationIgnored = true, isMonitoringActive = false),
                        onToggleMonitoring = {}, onToggleBackgroundService = {},
                        onRequestUsageAccess = {}, onRequestNotificationPermission = {},
                        onRequestBatteryOptimization = {}, onRefreshUsage = {}, onClearLogs = {}
                    )
                }
            }
        }
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "ui-screenshots").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun darkNavigationAndScreenshots() {
        show("Dark")
        compose.onNodeWithContentDescription("Background monitoring").assertExists()
        screenshot("dark-overview")
        compose.onNodeWithText("Tools", substring = false).performClick()
        compose.onNodeWithText("Network tools").assertExists()
        screenshot("dark-tools")
        compose.onNodeWithText("Internet speed test").performClick()
        compose.onNodeWithContentDescription("Back to tools").assertHasClickAction().performClick()
        compose.onNodeWithText("Network tools").assertExists()
        compose.onNodeWithText("Settings", substring = false).performClick()
        compose.onNodeWithText("Appearance", substring = false).assertHasClickAction()
        screenshot("dark-settings")
    }

    @Test fun lightLargeTextNavigation() {
        show("Light", 1.3f)
        compose.onNodeWithText("Apps", substring = false).assertHasClickAction().performClick()
        compose.onNodeWithText("Grant access").assertHasClickAction()
        screenshot("light-large-apps")
    }
}
