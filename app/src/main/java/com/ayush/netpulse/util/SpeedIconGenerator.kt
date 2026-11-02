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
 * Generates a large, legible two-row monochrome status bar speed icon
 * (number on top, unit on bottom).
 *
 * The status bar shrinks this bitmap to roughly 24dp, so the artwork uses the whole canvas:
 * - A condensed bold font lets the digits grow tall while still fitting the narrow icon width.
 * - Text sizes are fitted to the available box instead of using fixed sizes, so single, double
 *   and triple digit speeds all fill the icon as much as possible without clipping.
 * - Purely numeric values: never displays decimals like "0.2K" for sub-KB traffic.
 * - Cached bitmap and text sizes so nothing is recomputed when the speed text is unchanged.
 */
object SpeedIconGenerator {

    private const val BITMAP_SIZE = 96 // 96x96 px for sharp rendering on high-DPI displays
    private const val SIDE_PADDING = 4f
    private const val MAX_NUMBER_HEIGHT = 58f // digits take ~60% of the icon height
    private const val MAX_UNIT_HEIGHT = 28f   // unit row ("KB/s" / "MB/s")
    private const val ROW_GAP = 3f
    private const val START_TEXT_SIZE = 120f
    private const val MIN_TEXT_SIZE = 16f

    // Condensed bold fits taller digits into the same width; falls back to normal bold if missing.
    private val condensedBold: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)

    private val textSizeCache = HashMap<String, Float>()
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

    private fun newTextPaint(): Paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        isSubpixelText = true
        typeface = condensedBold
        isFakeBoldText = true // Extra bold and punchy for maximum status bar visibility
        textAlign = Paint.Align.CENTER
    }

    /** Largest text size (px) whose bounds for [sample] fit inside [maxWidth] x [maxHeight]. */
    private fun fitTextSize(paint: Paint, sample: String, maxWidth: Float, maxHeight: Float): Float {
        val bounds = Rect()
        var size = START_TEXT_SIZE
        while (size > MIN_TEXT_SIZE) {
            paint.textSize = size
            paint.getTextBounds(sample, 0, sample.length, bounds)
            if (bounds.width() <= maxWidth && bounds.height() <= maxHeight) break
            size -= 1f
        }
        return size
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
            val maxWidth = BITMAP_SIZE - 2 * SIDE_PADDING

            val numberPaint = newTextPaint()
            val unitPaint = newTextPaint()

            // Digits share the same width and height, so sizing and positioning with zeros of the
            // same length keeps the layout stable no matter which digits are shown.
            val numberSample = "0".repeat(comp.value.length)
            numberPaint.textSize = textSizeCache.getOrPut("number${comp.value.length}") {
                fitTextSize(numberPaint, numberSample, maxWidth, MAX_NUMBER_HEIGHT)
            }
            unitPaint.textSize = textSizeCache.getOrPut("unit${comp.unit}") {
                fitTextSize(unitPaint, comp.unit, maxWidth, MAX_UNIT_HEIGHT)
            }

            val numBounds = Rect()
            numberPaint.getTextBounds(numberSample, 0, numberSample.length, numBounds)
            val h1 = numBounds.height().toFloat()

            val unitBounds = Rect()
            unitPaint.getTextBounds(comp.unit, 0, comp.unit.length, unitBounds)
            val h2 = unitBounds.height().toFloat()

            val totalHeight = h1 + ROW_GAP + h2

            val topY = (BITMAP_SIZE - totalHeight) / 2f
            val y1 = topY - numBounds.top.toFloat()
            val y2 = topY + h1 + ROW_GAP - unitBounds.top.toFloat()
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
