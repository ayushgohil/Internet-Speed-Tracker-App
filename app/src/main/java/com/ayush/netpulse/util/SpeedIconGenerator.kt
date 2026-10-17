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
 * Generates an ultra-legible, two-row monochrome status bar speed icon matching
 * the classic internet speed meter layout (number on top, unit on bottom).
 *
 * Requirements addressed:
 * - Two-row layout: bold centered numeric value on top, compact unit ("KB/s" or "MB/s") below.
 * - Purely numeric values: never displays decimals like "0.2K" or "0.8K" for sub-KB traffic.
 *   Sub-KB values round cleanly to 0 or 1 without decimals.
 * - Zero clipping across single digit ("0"), double digit ("84"), and triple digit ("300") speeds.
 * - Perfectly centered vertically and horizontally within 96x96 px monochrome alpha mask.
 * - In-memory caching for zero allocations when speed representation is unchanged.
 */
object SpeedIconGenerator {

    private const val BITMAP_SIZE = 96 // 96x96 px for sharp rendering on high-DPI displays
    private var lastFormattedText: String = ""
    private var lastCachedIcon: IconCompat? = null

    data class SpeedComponents(
        val value: String,
        val unit: String
    )

    /**
     * Splits speed into a purely numeric value and standard unit ("KB/s" or "MB/s").
     * No decimal places are shown (e.g., 0.2 KB/s -> "0", 84 KB/s -> "84", 1.2 MB/s -> "1").
     */
    fun getSpeedComponents(bytesPerSec: Long): SpeedComponents {
        if (bytesPerSec <= 0L) return SpeedComponents("0", "KB/s")

        val kbDouble = bytesPerSec / 1024.0
        return when {
            kbDouble >= 1000.0 -> {
                val mb = (kbDouble / 1024.0).roundToInt().coerceAtLeast(1)
                SpeedComponents("$mb", "MB/s")
            }
            else -> {
                val kb = kbDouble.roundToInt()
                SpeedComponents("$kb", "KB/s")
            }
        }
    }

    /**
     * Formats bytes per second into a compact string representation.
     * Guaranteed purely numeric (no decimals like "0.2K"):
     * Examples:
     * - 0 B/s -> "0"
     * - 200 B/s -> "0"
     * - 800 B/s -> "1K"
     * - 12 KB/s -> "12K"
     * - 84 KB/s -> "84K"
     * - 300 KB/s -> "300K"
     * - 1.2 MB/s -> "1M"
     */
    fun formatCompactSpeed(bytesPerSec: Long): String {
        val comp = getSpeedComponents(bytesPerSec)
        return if (comp.value == "0") "0" else "${comp.value}${if (comp.unit == "MB/s") "M" else "K"}"
    }

    /**
     * Generates a monochrome IconCompat containing the current speed indicator
     * rendered in 2 stacked rows (Number on top, Unit below).
     */
    @Synchronized
    fun createSpeedIcon(bytesPerSec: Long): IconCompat? {
        val comp = getSpeedComponents(bytesPerSec)
        val cacheKey = "${comp.value}|${comp.unit}"

        if (cacheKey == lastFormattedText && lastCachedIcon != null) {
            return lastCachedIcon
        }

        return try {
            val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val numberPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                isSubpixelText = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = when (comp.value.length) {
                    1 -> 54f // "0", "5"
                    2 -> 50f // "84", "15"
                    3 -> 42f // "300", "999"
                    else -> 34f
                }
            }

            val unitPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                isSubpixelText = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = 26f // "KB/s" or "MB/s"
            }

            val numBounds = Rect()
            numberPaint.getTextBounds(comp.value, 0, comp.value.length, numBounds)
            val h1 = numBounds.height().toFloat()

            val unitBounds = Rect()
            unitPaint.getTextBounds(comp.unit, 0, comp.unit.length, unitBounds)
            val h2 = unitBounds.height().toFloat()

            val gap = 4f
            val totalHeight = h1 + gap + h2

            val topY = (BITMAP_SIZE - totalHeight) / 2f
            val y1 = topY - numBounds.top.toFloat()
            val y2 = topY + h1 + gap - unitBounds.top.toFloat()
            val x = BITMAP_SIZE / 2f

            canvas.drawText(comp.value, x, y1, numberPaint)
            canvas.drawText(comp.unit, x, y2, unitPaint)

            val iconCompat = IconCompat.createWithBitmap(bitmap)
            lastFormattedText = cacheKey
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

