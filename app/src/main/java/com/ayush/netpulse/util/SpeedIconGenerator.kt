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
 * Generates a tall, slim, easy-to-read two-row monochrome status bar speed icon
 * (number on top, unit below).
 *
 * The status bar shrinks this bitmap to roughly 37 px, so the artwork uses the whole canvas and is
 * laid out like the classic speed meters:
 * - Digits take about 54% of the height, a small gap about 8%, and the unit about 38%.
 * - Text is sized by HEIGHT first. It is then squeezed horizontally (textScaleX) so that even a
 *   three digit speed fits the width. This gives tall, slim digits on every phone, instead of
 *   depending on whether the phone's font happens to be narrow.
 * - No fake-bold: it made the digits look bulky once scaled down.
 * - 1, 2 and 3 digit speeds all use the same text height, so the icon never changes size.
 * - Purely numeric values: never displays decimals like "0.2K" for sub-KB traffic.
 * - Cached bitmap and text styles so nothing is recomputed when the speed text is unchanged.
 */
object SpeedIconGenerator {

    private const val BITMAP_SIZE = 96 // 96x96 px for sharp rendering on high-DPI displays
    private const val SIDE_PADDING = 4f

    // Row heights on the 96 px canvas (52 + 8 + 34 = 94, leaving a 1 px margin top and bottom).
    private const val NUMBER_HEIGHT = 52f
    private const val UNIT_HEIGHT = 34f
    private const val ROW_GAP = 8f

    private const val NUMBER_SCALE_X = 0.72f // slim digits
    private const val MIN_SCALE_X = 0.40f
    private const val START_TEXT_SIZE = 140f
    private const val MIN_TEXT_SIZE = 12f
    private const val SIZE_STEP = 0.5f

    private val boldTypeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    private data class RowStyle(val textSize: Float, val scaleX: Float)

    private val styleCache = HashMap<String, RowStyle>()
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
        typeface = boldTypeface
        textAlign = Paint.Align.CENTER
    }

    /**
     * Finds the text size whose bounds for [sample] are as tall as [targetHeight] (so the row gets
     * the height we want whatever the phone's font is), then the horizontal squeeze needed so the
     * row also fits in [maxWidth]. Width is measured unsqueezed and scaled linearly, so this does
     * not depend on how a given Android version reports bounds for a scaled paint.
     */
    private fun fitRow(sample: String, targetHeight: Float, baseScaleX: Float, maxWidth: Float): RowStyle {
        val paint = newTextPaint()
        val bounds = Rect()

        var size = START_TEXT_SIZE
        while (size > MIN_TEXT_SIZE) {
            paint.textSize = size
            paint.getTextBounds(sample, 0, sample.length, bounds)
            if (bounds.height() <= targetHeight) break
            size -= SIZE_STEP
        }
        paint.textSize = size
        paint.getTextBounds(sample, 0, sample.length, bounds)

        val naturalWidth = bounds.width().toFloat()
        val scaleX = if (naturalWidth > 0f) {
            minOf(baseScaleX, maxWidth / naturalWidth).coerceAtLeast(MIN_SCALE_X)
        } else {
            baseScaleX
        }
        return RowStyle(size, scaleX)
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

            // Digits all have the same height and width, so sizing and positioning with zeros of the
            // same length keeps the layout stable no matter which digits are shown.
            val numberSample = "0".repeat(comp.value.length)
            val numberStyle = styleCache.getOrPut("number${comp.value.length}") {
                fitRow(numberSample, NUMBER_HEIGHT, NUMBER_SCALE_X, maxWidth)
            }
            val unitStyle = styleCache.getOrPut("unit${comp.unit}") {
                fitRow(comp.unit, UNIT_HEIGHT, 1f, maxWidth)
            }

            val numberPaint = newTextPaint().apply {
                textSize = numberStyle.textSize
                textScaleX = numberStyle.scaleX
            }
            val unitPaint = newTextPaint().apply {
                textSize = unitStyle.textSize
                textScaleX = unitStyle.scaleX
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
        styleCache.clear()
        lastFormattedText = ""
        lastCachedIcon = null
    }
}
