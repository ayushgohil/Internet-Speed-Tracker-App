package com.ayush.netpulse.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// NetPulse Design System: Blueberry + Cream Soda & Midnight Blueberry
// ==============================================================================

// --- Light Theme Tokens: Blueberry + Cream Soda ---
val LightBackground = Color(0xFFF8F9FD)        // Clean Canvas (#F8F9FD)
val LightSurface = Color(0xFFFFFFFF)           // Surface (#FFFFFF)
val PrimaryBlueberry = Color(0xFF293D91)       // Primary Blueberry (#293D91)
val CreamSodaAccent = Color(0xFFFFF0C8)        // Cream Soda Accent (#FFF0C8)
val TextPrimaryLight = Color(0xFF17213B)       // Primary Text (#17213B)
val TextSecondaryLight = Color(0xFF64748B)     // Secondary Text (#64748B)
val BorderLight = Color(0xFFE2E8F0)            // Borders (#E2E8F0)
val DownloadBlueberryLight = Color(0xFF293D91) // Download Indicator (#293D91)
val UploadBrownLight = Color(0xFF9A5B20)       // Upload Indicator (#9A5B20)

// --- Dark Theme Tokens: Midnight Blueberry ---
val DarkMidnightBackground = Color(0xFF0C1224)      // Background (#0C1224)
val DarkMidnightCardSurface = Color(0xFF17213B)     // Card Surface (#17213B)
val DarkMidnightElevatedSurface = Color(0xFF202D4C) // Elevated Surface (#202D4C)
val PrimaryMidnightAccent = Color(0xFF8EA5FF)       // Primary Accent (#8EA5FF)
val SecondaryCreamSodaAccent = Color(0xFFFFF0C8)    // Secondary Accent (#FFF0C8)
val TextPrimaryDark = Color(0xFFF6F7FC)             // Primary Text (#F6F7FC)
val TextSecondaryDark = Color(0xFFA8B3C7)           // Secondary Text (#A8B3C7)
val BorderDark = Color(0xFF303F5C)                  // Borders (#303F5C)
val DownloadMidnight = Color(0xFF8EA5FF)            // Download Indicator (#8EA5FF)
val UploadMidnight = Color(0xFFFFF0C8)              // Upload Indicator (#FFF0C8)
val WarningAmber = Color(0xFFF5B544)                // Warning (#F5B544)
val ErrorRed = Color(0xFFF87171)                    // Error (#F87171)

// --- Heatmap Scale: Midnight Blueberry (Dark Theme) ---
val HeatmapMissingDark = Color(0xFF303F5C)  // Missing data (#303F5C)
val HeatmapZeroDark = Color(0xFF17213B)     // Genuine zero usage recorded
val HeatmapLevel1Dark = Color(0xFF293A78)   // Level 1 (#293A78)
val HeatmapLevel2Dark = Color(0xFF4258A7)   // Level 2 (#4258A7)
val HeatmapLevel3Dark = Color(0xFF627BD0)   // Level 3 (#627BD0)
val HeatmapLevel4Dark = Color(0xFF8EA5FF)   // Level 4 (#8EA5FF)
val HeatmapLevel5Dark = Color(0xFFC4D0FF)   // Level 5 (#C4D0FF)

// --- Heatmap Scale: Blueberry (Light Theme) ---
val HeatmapMissingLight = Color(0xFFE2E8F0) // Missing data (#E2E8F0)
val HeatmapZeroLight = Color(0xFFFFFFFF)    // Genuine zero usage recorded
val HeatmapLevel1Light = Color(0xFFDCE4FF)  // Level 1
val HeatmapLevel2Light = Color(0xFFB4C5FE)  // Level 2
val HeatmapLevel3Light = Color(0xFF8EA5FF)  // Level 3
val HeatmapLevel4Light = Color(0xFF5673E0)  // Level 4
val HeatmapLevel5Light = Color(0xFF293D91)  // Level 5

// --- Backward-Compatibility Aliases ---
val DarkBackground = DarkMidnightBackground
val DarkSurface = DarkMidnightCardSurface
val DarkCardBorder = BorderDark
val Cyan400 = PrimaryMidnightAccent
val Cyan500 = PrimaryMidnightAccent
val Blue400 = PrimaryMidnightAccent
val Blue500 = PrimaryBlueberry
val Emerald400 = PrimaryMidnightAccent
val Emerald500 = PrimaryBlueberry
val Slate900 = DarkMidnightBackground
val Slate800 = DarkMidnightElevatedSurface
val Slate700 = BorderDark
val Slate600 = TextSecondaryLight
val Slate400 = TextSecondaryDark
val Slate200 = BorderLight
val Amber500 = WarningAmber
val Rose500 = ErrorRed
val AquaFoam = PrimaryMidnightAccent
val DeepTeal = PrimaryBlueberry
val DownloadTeal = DownloadMidnight
val UploadBlue = UploadMidnight
val DownloadTealLight = DownloadBlueberryLight
val UploadBlueLight = UploadBrownLight
val DarkNavyBackground = DarkMidnightBackground
val NavySurface = DarkMidnightCardSurface
val NavySurfaceVariant = DarkMidnightElevatedSurface
val DeepTealPrimaryLight = PrimaryBlueberry
val WarningAmberLight = WarningAmber
val ErrorRedLight = ErrorRed
