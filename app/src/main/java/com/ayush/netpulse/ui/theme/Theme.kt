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
    val elevatedBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val secondaryAccent: Color, // Cream Soda
    val warning: Color,
    val error: Color,
    val heatmapMissing: Color,
    val heatmapZero: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
    val heatmapLevel5: Color
)

val LocalNetPulseColors = staticCompositionLocalOf {
    NetPulseExtendedColors(
        download = DownloadMidnight,
        upload = UploadMidnight,
        cardBackground = DarkMidnightCardSurface,
        elevatedBackground = DarkMidnightElevatedSurface,
        cardBorder = BorderDark,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        secondaryAccent = SecondaryCreamSodaAccent,
        warning = WarningAmber,
        error = ErrorRed,
        heatmapMissing = HeatmapMissingDark,
        heatmapZero = HeatmapZeroDark,
        heatmapLevel1 = HeatmapLevel1Dark,
        heatmapLevel2 = HeatmapLevel2Dark,
        heatmapLevel3 = HeatmapLevel3Dark,
        heatmapLevel4 = HeatmapLevel4Dark,
        heatmapLevel5 = HeatmapLevel5Dark
    )
}

// Midnight Blueberry (Dark) ColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMidnightAccent,              // #8EA5FF
    onPrimary = DarkMidnightBackground,           // #0C1224
    primaryContainer = DarkMidnightElevatedSurface,// #202D4C
    onPrimaryContainer = PrimaryMidnightAccent,   // #8EA5FF
    secondary = SecondaryCreamSodaAccent,         // #FFF0C8
    onSecondary = DarkMidnightBackground,
    secondaryContainer = DarkMidnightElevatedSurface,
    onSecondaryContainer = SecondaryCreamSodaAccent,
    tertiary = PrimaryMidnightAccent,
    onTertiary = DarkMidnightBackground,
    background = DarkMidnightBackground,          // #0C1224
    onBackground = TextPrimaryDark,               // #F6F7FC
    surface = DarkMidnightCardSurface,            // #17213B
    onSurface = TextPrimaryDark,                  // #F6F7FC
    surfaceVariant = DarkMidnightElevatedSurface, // #202D4C
    onSurfaceVariant = TextSecondaryDark,         // #A8B3C7
    outline = BorderDark,                         // #303F5C
    outlineVariant = Color(0xFF202D4C),
    error = ErrorRed,                             // #F87171
    onError = DarkMidnightBackground
)

// Blueberry + Cream Soda (Light) ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueberry,                   // #293D91
    onPrimary = Color.White,
    primaryContainer = CreamSodaAccent,           // #FFF0C8
    onPrimaryContainer = PrimaryBlueberry,        // #293D91
    secondary = UploadBrownLight,                 // #9A5B20
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF2FD),
    onSecondaryContainer = PrimaryBlueberry,
    tertiary = UploadBrownLight,
    onTertiary = Color.White,
    background = LightBackground,                 // #F8F9FD
    onBackground = TextPrimaryLight,              // #17213B
    surface = LightSurface,                       // #FFFFFF
    onSurface = TextPrimaryLight,                 // #17213B
    surfaceVariant = Color(0xFFF1F4FA),
    onSurfaceVariant = TextSecondaryLight,        // #64748B
    outline = BorderLight,                        // #E2E8F0
    outlineVariant = Color(0xFFCBD5E1),
    error = ErrorRed,
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
            download = DownloadMidnight,
            upload = UploadMidnight,
            cardBackground = DarkMidnightCardSurface,
            elevatedBackground = DarkMidnightElevatedSurface,
            cardBorder = BorderDark,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            secondaryAccent = SecondaryCreamSodaAccent,
            warning = WarningAmber,
            error = ErrorRed,
            heatmapMissing = HeatmapMissingDark,
            heatmapZero = HeatmapZeroDark,
            heatmapLevel1 = HeatmapLevel1Dark,
            heatmapLevel2 = HeatmapLevel2Dark,
            heatmapLevel3 = HeatmapLevel3Dark,
            heatmapLevel4 = HeatmapLevel4Dark,
            heatmapLevel5 = HeatmapLevel5Dark
        )
    } else {
        NetPulseExtendedColors(
            download = DownloadBlueberryLight,
            upload = UploadBrownLight,
            cardBackground = LightSurface,
            elevatedBackground = Color(0xFFF1F4FA),
            cardBorder = BorderLight,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            secondaryAccent = CreamSodaAccent,
            warning = WarningAmber,
            error = ErrorRed,
            heatmapMissing = HeatmapMissingLight,
            heatmapZero = HeatmapZeroLight,
            heatmapLevel1 = HeatmapLevel1Light,
            heatmapLevel2 = HeatmapLevel2Light,
            heatmapLevel3 = HeatmapLevel3Light,
            heatmapLevel4 = HeatmapLevel4Light,
            heatmapLevel5 = HeatmapLevel5Light
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
