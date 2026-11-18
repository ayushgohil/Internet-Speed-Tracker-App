package com.ayush.netpulse.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpeedSamplerTest {

    private lateinit var sampler: SpeedSampler

    @Before
    fun setUp() {
        sampler = SpeedSampler()
    }

    @Test
    fun firstSampleEstablishesBaselineAndReturnsZero() {
        val result = sampler.sample(
            currentRxBytes = 10_000L,
            currentTxBytes = 4_000L,
            elapsedRealtimeMillis = 1_000L,
            wallClockMillis = 50_000L
        )

        assertEquals(0L, result.downloadBytesPerSec)
        assertEquals(0L, result.uploadBytesPerSec)
        assertEquals(50_000L, result.timestamp)
    }

    @Test
    fun calculatesDownloadAndUploadRatesIndependently() {
        sampler.sample(1_000L, 2_000L, 1_000L, 10_000L)

        val result = sampler.sample(
            currentRxBytes = 11_000L,
            currentTxBytes = 4_000L,
            elapsedRealtimeMillis = 2_000L,
            wallClockMillis = 11_000L
        )

        assertEquals(10_000L, result.downloadBytesPerSec)
        assertEquals(2_000L, result.uploadBytesPerSec)
    }

    @Test
    fun ignoresShortIntervalsWithoutDiscardingAccumulatedBytes() {
        sampler.sample(1_000L, 1_000L, 1_000L, 10_000L)

        val shortSample = sampler.sample(1_500L, 1_250L, 1_025L, 10_025L)
        assertEquals(0L, shortSample.downloadBytesPerSec)
        assertEquals(0L, shortSample.uploadBytesPerSec)

        val validSample = sampler.sample(2_000L, 1_500L, 1_100L, 10_100L)
        assertEquals(10_000L, validSample.downloadBytesPerSec)
        assertEquals(5_000L, validSample.uploadBytesPerSec)
    }

    @Test
    fun counterResetEstablishesNewBaseline() {
        sampler.sample(10_000L, 8_000L, 1_000L, 10_000L)

        val resetSample = sampler.sample(100L, 50L, 2_000L, 11_000L)
        assertEquals(0L, resetSample.downloadBytesPerSec)
        assertEquals(0L, resetSample.uploadBytesPerSec)

        val nextSample = sampler.sample(1_100L, 550L, 3_000L, 12_000L)
        assertEquals(1_000L, nextSample.downloadBytesPerSec)
        assertEquals(500L, nextSample.uploadBytesPerSec)
    }

    @Test
    fun unsupportedCounterClearsBaseline() {
        sampler.sample(1_000L, 500L, 1_000L, 10_000L)

        val unsupported = sampler.sample(-1L, 700L, 2_000L, 11_000L)
        assertEquals(0L, unsupported.downloadBytesPerSec)
        assertEquals(0L, unsupported.uploadBytesPerSec)

        val nextSample = sampler.sample(5_000L, 2_500L, 3_000L, 12_000L)
        assertEquals(0L, nextSample.downloadBytesPerSec)
        assertEquals(0L, nextSample.uploadBytesPerSec)
    }

    @Test
    fun nonPositiveElapsedTimeResetsBaseline() {
        sampler.sample(1_000L, 500L, 1_000L, 10_000L)

        val invalidInterval = sampler.sample(2_000L, 1_000L, 1_000L, 10_100L)
        assertEquals(0L, invalidInterval.downloadBytesPerSec)

        val nextSample = sampler.sample(3_000L, 1_500L, 2_000L, 11_000L)
        assertEquals(1_000L, nextSample.downloadBytesPerSec)
        assertEquals(500L, nextSample.uploadBytesPerSec)
    }

    @Test
    fun explicitResetRequiresANewBaseline() {
        sampler.sample(1_000L, 500L, 1_000L, 10_000L)
        sampler.reset()

        val result = sampler.sample(5_000L, 2_500L, 2_000L, 11_000L)
        assertEquals(0L, result.downloadBytesPerSec)
        assertEquals(0L, result.uploadBytesPerSec)
    }

    @Test
    fun wallClockTimestampDoesNotAffectElapsedRateCalculation() {
        sampler.sample(1_000L, 500L, 1_000L, 100_000L)

        val result = sampler.sample(
            currentRxBytes = 3_000L,
            currentTxBytes = 1_500L,
            elapsedRealtimeMillis = 2_000L,
            wallClockMillis = 50_000L
        )

        assertEquals(2_000L, result.downloadBytesPerSec)
        assertEquals(1_000L, result.uploadBytesPerSec)
        assertEquals(50_000L, result.timestamp)
    }
}
