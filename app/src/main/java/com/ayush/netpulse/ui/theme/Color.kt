package com.ayush.netpulse.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// NetPulse Design System: Deep Navy / Cyber Teal / Aqua Foam
// ==============================================================================

// --- Dark Theme Tokens ---
val DarkNavyBackground = Color(0xFF0B1220) // Deep Navy App Background
val NavySurface = Color(0xFF162235)        // Card / Surface Background
val NavySurfaceVariant = Color(0xFF1E2D44) // Elevated Cards / Header / Chips
val AquaFoam = Color(0xFF6AF0D4)           // Primary Accent / Download Indicator
val DeepTeal = Color(0xFF075C61)           // Brand Deep Teal
val TextPrimaryDark = Color(0xFFF1F5F9)    // Primary High-Contrast Text
val TextSecondaryDark = Color(0xFF94A3B8)  // Secondary / Description Text
val TextMutedDark = Color(0xFF64748B)      // Sub-muted Captions / Footers
val DownloadTeal = Color(0xFF6AF0D4)       // Download Indicator (#6AF0D4)
val UploadBlue = Color(0xFF8BA8FF)         // Upload Indicator (#8BA8FF)
val BorderDark = Color(0xFF26364A)         // Clean Subtle Border (#26364A)
val WarningAmber = Color(0xFFF5B544)       // Warning (#F5B544)
val ErrorRed = Color(0xFFF87171)           // Error (#F87171)

// --- Light Theme Tokens ---
val LightBackground = Color(0xFFF8FAFC)    // Light Neutral Canvas
val LightSurface = Color(0xFFFFFFFF)       // Crisp Clean White Surface
val LightSurfaceVariant = Color(0xFFF1F5F9)// Subtle Contrast Card Variant
val DeepTealPrimaryLight = Color(0xFF075C61)// Deep Teal Brand for Light Mode
val TextPrimaryLight = Color(0xFF0F172A)   // High-Contrast Dark Text
val TextSecondaryLight = Color(0xFF64748B) // Secondary Accessible Slate
val TextMutedLight = Color(0xFF94A3B8)     // Muted Gray
val DownloadTealLight = Color(0xFF0D9488)  // High-Contrast Teal for Download
val UploadBlueLight = Color(0xFF3B82F6)    // Crisp Blue for Upload
val BorderLight = Color(0xFFE2E8F0)        // Subtle Border on Light Canvas
val WarningAmberLight = Color(0xFFD97706)  // Accessible Warning Amber
val ErrorRedLight = Color(0xFFDC2626)      // Accessible Error Red

// --- Heatmap Teal Intensity Scale (Dark Theme) ---
val HeatmapMissingDark = Color(0xFF283647)  // Missing data
val HeatmapZeroDark = Color(0xFF1B2A3E)     // Genuine zero usage recorded
val HeatmapVeryLowDark = Color(0xFF194E50)  // Very low
val HeatmapLowDark = Color(0xFF207B76)      // Low
val HeatmapMediumDark = Color(0xFF32AFA0)   // Medium
val HeatmapHighDark = Color(0xFF6AF0D4)     // High

// --- Heatmap Teal Intensity Scale (Light Theme) ---
val HeatmapMissingLight = Color(0xFFE2E8F0) // Missing data
val HeatmapZeroLight = Color(0xFFF1F5F9)    // Genuine zero usage recorded
val HeatmapVeryLowLight = Color(0xFF99F6E4) // Very low
val HeatmapLowLight = Color(0xFF2DD4BF)     // Low
val HeatmapMediumLight = Color(0xFF14B8A6)  // Medium
val HeatmapHighLight = Color(0xFF0D9488)    // High

// --- Backward-Compatibility Aliases ---
val DarkBackground = DarkNavyBackground
val DarkSurface = NavySurface
val DarkCardBorder = BorderDark
val Cyan400 = AquaFoam
val Cyan500 = Color(0xFF32AFA0)
val Blue400 = UploadBlue
val Blue500 = Color(0xFF3B82F6)
val Emerald400 = AquaFoam
val Emerald500 = DeepTeal
val Slate900 = Color(0xFF0B1220)
val Slate800 = NavySurfaceVariant
val Slate700 = Color(0xFF26364A)
val Slate600 = Color(0xFF64748B)
val Slate400 = TextSecondaryDark
val Slate200 = Color(0xFFE2E8F0)
val Amber500 = WarningAmber
val Rose500 = ErrorRed
