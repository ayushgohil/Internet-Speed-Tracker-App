package com.ayush.netpulse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class NetPulseExtendedColors(
    val download: Color,
    val upload: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val warning: Color,
    val error: Color,
    val heatmapMissing: Color,
    val heatmapZero: Color,
    val heatmapVeryLow: Color,
    val heatmapLow: Color,
    val heatmapMedium: Color,
    val heatmapHigh: Color
)

val LocalNetPulseColors = staticCompositionLocalOf {
    NetPulseExtendedColors(
        download = DownloadTeal,
        upload = UploadBlue,
        cardBackground = NavySurface,
        cardBorder = BorderDark,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        warning = WarningAmber,
        error = ErrorRed,
        heatmapMissing = HeatmapMissingDark,
        heatmapZero = HeatmapZeroDark,
        heatmapVeryLow = HeatmapVeryLowDark,
        heatmapLow = HeatmapLowDark,
        heatmapMedium = HeatmapMediumDark,
        heatmapHigh = HeatmapHighDark
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AquaFoam,
    onPrimary = DarkNavyBackground,
    primaryContainer = DeepTeal,
    onPrimaryContainer = AquaFoam,
    secondary = UploadBlue,
    onSecondary = DarkNavyBackground,
    secondaryContainer = NavySurfaceVariant,
    onSecondaryContainer = UploadBlue,
    tertiary = DeepTeal,
    onTertiary = TextPrimaryDark,
    background = DarkNavyBackground,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = Color(0xFF1E2D44),
    error = ErrorRed,
    onError = DarkNavyBackground
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTealPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = DeepTealPrimaryLight,
    secondary = UploadBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = UploadBlueLight,
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = ErrorRedLight,
    onError = Color.White
)

@Composable
fun NetPulseTheme(
    appTheme: String = "Dark",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) {
        NetPulseExtendedColors(
            download = DownloadTeal,
            upload = UploadBlue,
            cardBackground = NavySurface,
            cardBorder = BorderDark,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            warning = WarningAmber,
            error = ErrorRed,
            heatmapMissing = HeatmapMissingDark,
            heatmapZero = HeatmapZeroDark,
            heatmapVeryLow = HeatmapVeryLowDark,
            heatmapLow = HeatmapLowDark,
            heatmapMedium = HeatmapMediumDark,
            heatmapHigh = HeatmapHighDark
        )
    } else {
        NetPulseExtendedColors(
            download = DownloadTealLight,
            upload = UploadBlueLight,
            cardBackground = LightSurface,
            cardBorder = BorderLight,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            warning = WarningAmberLight,
            error = ErrorRedLight,
            heatmapMissing = HeatmapMissingLight,
            heatmapZero = HeatmapZeroLight,
            heatmapVeryLow = HeatmapVeryLowLight,
            heatmapLow = HeatmapLowLight,
            heatmapMedium = HeatmapMediumLight,
            heatmapHigh = HeatmapHighLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalNetPulseColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
