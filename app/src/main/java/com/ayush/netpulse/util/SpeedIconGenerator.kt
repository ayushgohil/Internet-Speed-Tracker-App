package com.ayush.netpulse.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

object SpeedIconGenerator {

    private const val BITMAP_SIZE = 72 // 72x72 px standard crisp notification icon
    private var lastFormattedText: String = ""
    private var lastCachedIcon: IconCompat? = null

    /**
     * Formats bytes per second into a compact 1-4 character string suitable for the status bar icon:
     * Examples: "0", "12K", "128K", "1.2M", "15M"
     */
    fun formatCompactSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0"
        val kb = bytesPerSec / 1024
        return when {
            kb < 1 -> "0"
            kb < 1000 -> "${kb}K"
            else -> {
                val mb = kb / 1024.0
                if (mb < 10.0) {
                    String.format(Locale.US, "%.1fM", mb)
                } else {
                    "${mb.toInt()}M"
                }
            }
        }
    }

    /**
     * Generates a monochrome IconCompat containing the current download speed.
     * Caches the bitmap if the text representation has not changed.
     * Returns null if generation fails, allowing automatic fallback to the standard icon.
     */
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
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                // Adapt text size to fit within the 72px bounds
                textSize = when (text.length) {
                    1 -> 42f
                    2 -> 36f
                    3 -> 30f
                    else -> 24f
                }
            }

            // Calculate vertical center based on font metrics
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
}
