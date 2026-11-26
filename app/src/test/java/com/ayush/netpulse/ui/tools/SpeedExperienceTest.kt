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
        assertEquals(10, NetworkTests.Budget.LOW.maxMb)
        assertEquals(200, NetworkTests.Budget.STANDARD.maxMb)
        assertEquals(500, NetworkTests.Budget.EXTENDED.maxMb)
        assertTrue(NetworkTests.Budget.values().all { it.stageMillis > 0 && it.downloadBytes > 0 && it.uploadBytes > 0 })
    }
    @Test fun adaptiveRequestsRespectBounds() {
        assertEquals(5_000_000, NetworkTests.requestBytes(false, 0.0, 8000))
        assertEquals(25_000_000, NetworkTests.requestBytes(false, 1000.0, 8000))
        assertEquals(4_000_000, NetworkTests.requestBytes(true, 1000.0, 8000))
        assertTrue(NetworkTests.requestBytes(false, 50.0, 100) < NetworkTests.requestBytes(false, 50.0, 1000))
    }

    @Test fun progressTracksEitherTimeOrPayloadLimit() {
        assertEquals(.5f, NetworkTests.stageProgress(10, 100, 4000, 8000), .001f)
        assertEquals(1f, NetworkTests.stageProgress(100, 100, 1, 8000), 0f)
        assertEquals(1f, NetworkTests.stageProgress(1, 100, 8000, 8000), 0f)
    }
}
