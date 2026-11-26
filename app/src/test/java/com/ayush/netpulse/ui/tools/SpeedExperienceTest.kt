package com.ayush.netpulse.ui.tools

import org.junit.Assert.*
import org.junit.Test

class SpeedExperienceTest {
    @Test fun gaugeIsBoundedAndMonotonic() {
        val rates = listOf(0.0, 1.0, 5.0, 10.0, 100.0, 500.0, 1000.0, 2000.0)
        val fractions = rates.map(::gaugeFraction)
        assertEquals(0f, fractions.first(), 0f)
        assertEquals(1f, fractions.last(), 0f)
        assertTrue(fractions.zipWithNext().all { (a, b) -> a <= b })
        assertEquals(0f, gaugeFraction(Double.NaN), 0f)
        assertEquals(0f, gaugeFraction(-1.0), 0f)
    }

    @Test fun decimalMbpsUsesElapsedNanoseconds() {
        assertEquals(8.0, NetworkTests.throughputMbps(1_000_000, 1_000_000_000), 0.00001)
        assertEquals(0.0, NetworkTests.throughputMbps(0, 100), 0.0)
    }

    @Test fun budgetCapsAreExplicit() {
        assertEquals(6, NetworkTests.Budget.LOW.maxMb)
        assertEquals(52, NetworkTests.Budget.STANDARD.maxMb)
        assertEquals(130, NetworkTests.Budget.EXTENDED.maxMb)
        assertTrue(NetworkTests.Budget.values().all { it.stageMillis > 0 && it.downloadBytes > 0 && it.uploadBytes > 0 })
    }
}
