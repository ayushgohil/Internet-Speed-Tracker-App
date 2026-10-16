package com.ayush.netpulse.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.roundToInt

/**
 * Generates an ultra-legible monochrome status bar speed icon.
 *
 * Requirements addressed:
 * - Highly visible, bold monochrome label tinted by Android SystemUI.
 * - Short values without tiny decimals: "0", "12K", "300K", "1M", "15M".
 * - Canvas size 96x96 with maximized font size and centered bounds.
 * - Zero clipping across single-char ("0") to 4-char ("300K") values.
 * - In-memory caching to avoid rebuilding identical bitmaps.
 * - Safe exception handling returning null to trigger fallback to standard app icon.
 */
object SpeedIconGenerator {

    private const val BITMAP_SIZE = 96 // 96x96 px for sharp rendering on high-DPI displays
    private var lastFormattedText: String = ""
    private var lastCachedIcon: IconCompat? = null

    /**
     * Formats bytes per second into an ultra-compact 1-4 character string:
     * Examples:
     * - 0 B/s -> "0"
     * - 50 B/s -> "0"
     * - 300 B/s -> "0.3K" (prevents premature zero rounding for small active traffic)
     * - 800 B/s -> "0.8K"
     * - 12 KB/s -> "12K"
     * - 300 KB/s -> "300K"
     * - 950 KB/s -> "950K"
     * - 1.2 MB/s -> "1M" (no tiny dot or unreadable decimals for MB)
     * - 15.6 MB/s -> "16M"
     */
    fun formatCompactSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0"
        val kbDouble = bytesPerSec / 1024.0
        val kbInt = kbDouble.toInt()
        return when {
            kbInt >= 1000 -> {
                val mb = (kbDouble / 1024.0).roundToInt().coerceAtLeast(1)
                "${mb}M"
            }
            kbInt >= 1 -> "${kbInt}K"
            kbDouble >= 0.1 -> {
                // Between 102 B/s and 1023 B/s: display "0.X K" (e.g. "0.3K") so small active traffic doesn't show 0
                val tenth = (kbDouble * 10).roundToInt()
                if (tenth >= 10) "1K" else "0.${tenth}K"
            }
            else -> "0"
        }
    }

    /**
     * Generates a monochrome IconCompat containing the current speed label.
     * Caches the IconCompat if the text representation has not changed.
     * Returns null if generation fails, allowing automatic fallback to the standard icon.
     */
    @Synchronized
    fun createSpeedIcon(bytesPerSec: Long): IconCompat? {
        val text = formatCompactSpeed(bytesPerSec)

        // Cache optimization: Avoid generating new Bitmaps when the text is identical
        if (text == lastFormattedText && lastCachedIcon != null) {
            return lastCachedIcon
        }

        return try {
            val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint().apply {
                color = Color.WHITE // Monochrome alpha mask for system status bar tinting
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                // Maximized font sizing tailored to string length to prevent clipping and maximize legibility
                textSize = when (text.length) {
                    1 -> 68f // "0"
                    2 -> 56f // "1M", "9K"
                    3 -> 48f // "12K", "15M"
                    else -> 38f // "300K", "999K", "100M"
                }
            }

            // Calculate precise vertical centering using font bounds
            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val x = BITMAP_SIZE / 2f
            val y = (BITMAP_SIZE / 2f) + (textBounds.height() / 2f) - textBounds.bottom

            canvas.drawText(text, x, y, paint)

            val iconCompat = IconCompat.createWithBitmap(bitmap)
            lastFormattedText = text
            lastCachedIcon = iconCompat
            iconCompat
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Resets the icon cache.
     */
    @Synchronized
    fun clearCache() {
        lastFormattedText = ""
        lastCachedIcon = null
    }
}
