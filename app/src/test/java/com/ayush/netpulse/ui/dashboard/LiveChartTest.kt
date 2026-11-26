package com.ayush.netpulse.ui.dashboard

import com.ayush.netpulse.data.model.LiveSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChartTest {
    @Test fun emptySamplesHaveNoPaths() {
        assertTrue(splitLiveSamples(emptyList()).isEmpty())
    }
    @Test fun regularSamplesStayTogether() {
        val samples = listOf(LiveSpeed(0, 0, 1000), LiveSpeed(50, 10, 2000), LiveSpeed(0, 0, 3000))
        assertEquals(listOf(samples), splitLiveSamples(samples))
    }
    @Test fun longGapBreaksPathWithoutInventingSamples() {
        val a = LiveSpeed(100, 20, 1000)
        val b = LiveSpeed(200, 40, 13001)
        assertEquals(listOf(listOf(a), listOf(b)), splitLiveSamples(listOf(a, b)))
    }
}
