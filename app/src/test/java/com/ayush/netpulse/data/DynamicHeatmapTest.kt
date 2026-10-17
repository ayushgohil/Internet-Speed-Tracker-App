package com.ayush.netpulse.data

import com.ayush.netpulse.ui.usage.DynamicHeatmapClassifier
import com.ayush.netpulse.ui.usage.HeatmapIntensityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicHeatmapTest {

    private val oneGb = 1024L * 1024L * 1024L

    @Test
    fun testFiveDistinctValues_DistributeAcrossLevels() {
        // Daily values: 1 GB, 2 GB, 5 GB, 10 GB, 20 GB
        val values = listOf(
            1 * oneGb,
            2 * oneGb,
            5 * oneGb,
            10 * oneGb,
            20 * oneGb
        )

        val thresholds = DynamicHeatmapClassifier.computeThresholds(values)
        assertNotNull(thresholds)

        val l1 = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 1 * oneGb, thresholds = thresholds)
        val l2 = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 2 * oneGb, thresholds = thresholds)
        val l3 = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 5 * oneGb, thresholds = thresholds)
        val l4 = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 10 * oneGb, thresholds = thresholds)
        val l5 = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 20 * oneGb, thresholds = thresholds)

        assertEquals(HeatmapIntensityLevel.LEVEL_1, l1)
        assertEquals(HeatmapIntensityLevel.LEVEL_2, l2)
        assertEquals(HeatmapIntensityLevel.LEVEL_3, l3)
        assertEquals(HeatmapIntensityLevel.LEVEL_4, l4)
        assertEquals(HeatmapIntensityLevel.LEVEL_5, l5)
    }

    @Test
    fun testIdenticalValues_MapToSameLevelWithoutArtificialDifferentiation() {
        val values = listOf(
            5 * oneGb,
            5 * oneGb,
            5 * oneGb,
            5 * oneGb
        )

        val thresholds = DynamicHeatmapClassifier.computeThresholds(values)
        assertNotNull(thresholds)
        assertEquals(thresholds!!.minPositive, thresholds.maxPositive)

        val levelA = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 5 * oneGb, thresholds = thresholds)
        val levelB = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 5 * oneGb, thresholds = thresholds)

        assertEquals(levelA, levelB)
        assertEquals(HeatmapIntensityLevel.LEVEL_3, levelA)
    }

    @Test
    fun testZeroUsage_DistinguishableFromMissingData() {
        val values = listOf(1 * oneGb, 5 * oneGb)
        val thresholds = DynamicHeatmapClassifier.computeThresholds(values)

        val zeroLevel = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 0L, thresholds = thresholds)
        val missingLevel = DynamicHeatmapClassifier.classify(hasEntity = false, bytes = 0L, thresholds = thresholds)

        assertEquals(HeatmapIntensityLevel.ZERO, zeroLevel)
        assertEquals(HeatmapIntensityLevel.MISSING, missingLevel)
        assertTrue(zeroLevel != missingLevel)
    }

    @Test
    fun testLargeOutlier_PreservesDistributionOfNormalDays() {
        // Range 1GB to 3GB with an extreme 100GB outlier
        val values = listOf(
            1 * oneGb,
            1200L * 1024L * 1024L,
            1500L * 1024L * 1024L,
            2 * oneGb,
            2500L * 1024L * 1024L,
            3 * oneGb,
            100 * oneGb // Extreme outlier
        )

        val thresholds = DynamicHeatmapClassifier.computeThresholds(values)
        assertNotNull(thresholds)

        val outlierLevel = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 100 * oneGb, thresholds = thresholds)
        val normalMinLevel = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 1 * oneGb, thresholds = thresholds)
        val normalMidLevel = DynamicHeatmapClassifier.classify(hasEntity = true, bytes = 2 * oneGb, thresholds = thresholds)

        assertEquals(HeatmapIntensityLevel.LEVEL_5, outlierLevel)
        assertEquals(HeatmapIntensityLevel.LEVEL_1, normalMinLevel)
        // Mid-range days are not collapsed into Level 1
        assertTrue(normalMidLevel == HeatmapIntensityLevel.LEVEL_2 || normalMidLevel == HeatmapIntensityLevel.LEVEL_3)
    }
}
