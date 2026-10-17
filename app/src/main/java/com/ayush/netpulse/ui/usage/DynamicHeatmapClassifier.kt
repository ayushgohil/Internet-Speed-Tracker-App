package com.ayush.netpulse.ui.usage

import androidx.compose.ui.graphics.Color
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.ui.theme.NetPulseExtendedColors

enum class HeatmapIntensityLevel {
    MISSING,
    ZERO,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4,
    LEVEL_5
}

data class HeatmapThresholds(
    val t1: Long,
    val t2: Long,
    val t3: Long,
    val t4: Long,
    val minPositive: Long,
    val maxPositive: Long
)

object DynamicHeatmapClassifier {

    /**
     * Computes 5-level quantile thresholds for positive data consumption values.
     * Guaranteed to handle any dataset including 1 GB - 20 GB ranges,
     * identical values, small sets, and extreme outliers.
     */
    fun computeThresholds(values: List<Long>): HeatmapThresholds? {
        val positiveSorted = values.filter { it > 0L }.sorted()
        if (positiveSorted.isEmpty()) return null

        val minVal = positiveSorted.first()
        val maxVal = positiveSorted.last()

        if (minVal == maxVal) {
            return HeatmapThresholds(
                t1 = minVal,
                t2 = minVal,
                t3 = minVal,
                t4 = minVal,
                minPositive = minVal,
                maxPositive = maxVal
            )
        }

        fun quantile(fraction: Double): Long {
            val index = (fraction * (positiveSorted.size - 1)).toInt()
            return positiveSorted[index.coerceIn(0, positiveSorted.size - 1)]
        }

        val t1 = quantile(0.20)
        val t2 = quantile(0.40).coerceAtLeast(t1)
        val t3 = quantile(0.60).coerceAtLeast(t2)
        val t4 = quantile(0.80).coerceAtLeast(t3)

        return HeatmapThresholds(
            t1 = t1,
            t2 = t2,
            t3 = t3,
            t4 = t4,
            minPositive = minVal,
            maxPositive = maxVal
        )
    }

    /**
     * Classifies a day's usage into one of 7 states:
     * - MISSING (no record in database)
     * - ZERO (explicit 0 bytes consumption recorded)
     * - LEVEL_1 .. LEVEL_5 (quantile intensity)
     */
    fun classify(
        hasEntity: Boolean,
        bytes: Long,
        thresholds: HeatmapThresholds?
    ): HeatmapIntensityLevel {
        if (!hasEntity) return HeatmapIntensityLevel.MISSING
        if (bytes <= 0L) return HeatmapIntensityLevel.ZERO
        if (thresholds == null) return HeatmapIntensityLevel.LEVEL_1

        if (thresholds.minPositive == thresholds.maxPositive) {
            // When all recorded positive values are identical, keep them all at consistent level 3
            return HeatmapIntensityLevel.LEVEL_3
        }

        return when {
            bytes <= thresholds.t1 -> HeatmapIntensityLevel.LEVEL_1
            bytes <= thresholds.t2 -> HeatmapIntensityLevel.LEVEL_2
            bytes <= thresholds.t3 -> HeatmapIntensityLevel.LEVEL_3
            bytes <= thresholds.t4 -> HeatmapIntensityLevel.LEVEL_4
            else -> HeatmapIntensityLevel.LEVEL_5
        }
    }

    /**
     * Resolves the exact Color token for an intensity level given current theme extended colors.
     */
    fun getColor(level: HeatmapIntensityLevel, colors: NetPulseExtendedColors): Color {
        return when (level) {
            HeatmapIntensityLevel.MISSING -> colors.heatmapMissing
            HeatmapIntensityLevel.ZERO -> colors.heatmapZero
            HeatmapIntensityLevel.LEVEL_1 -> colors.heatmapLevel1
            HeatmapIntensityLevel.LEVEL_2 -> colors.heatmapLevel2
            HeatmapIntensityLevel.LEVEL_3 -> colors.heatmapLevel3
            HeatmapIntensityLevel.LEVEL_4 -> colors.heatmapLevel4
            HeatmapIntensityLevel.LEVEL_5 -> colors.heatmapLevel5
        }
    }
}
