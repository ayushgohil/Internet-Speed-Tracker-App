package com.ayush.netpulse.util

import com.ayush.netpulse.data.model.LiveSpeed

/**
 * Calculates live network speed from cumulative byte counters.
 *
 * elapsedRealtimeMillis must come from a monotonic clock (for example,
 * SystemClock.elapsedRealtime()), while wallClockMillis is only used as
 * the timestamp exposed to the rest of the app.
 */
class SpeedSampler(
    private val minimumIntervalMillis: Long = 50L
) {
    private var lastRxBytes = UNINITIALIZED
    private var lastTxBytes = UNINITIALIZED
    private var lastElapsedRealtimeMillis = UNINITIALIZED

    init {
        require(minimumIntervalMillis >= 0L) {
            "minimumIntervalMillis must not be negative"
        }
    }

    @Synchronized
    fun sample(
        currentRxBytes: Long,
        currentTxBytes: Long,
        elapsedRealtimeMillis: Long,
        wallClockMillis: Long
    ): LiveSpeed {
        // TrafficStats.UNSUPPORTED is -1. Treat any negative counter as unavailable.
        if (currentRxBytes < 0L || currentTxBytes < 0L || elapsedRealtimeMillis < 0L) {
            reset()
            return LiveSpeed(0L, 0L, wallClockMillis)
        }

        if (hasNoBaseline()) {
            updateBaseline(currentRxBytes, currentTxBytes, elapsedRealtimeMillis)
            return LiveSpeed(0L, 0L, wallClockMillis)
        }

        // A lower cumulative counter means the counter reset or the device restarted.
        if (currentRxBytes < lastRxBytes || currentTxBytes < lastTxBytes) {
            updateBaseline(currentRxBytes, currentTxBytes, elapsedRealtimeMillis)
            return LiveSpeed(0L, 0L, wallClockMillis)
        }

        val elapsedMillis = elapsedRealtimeMillis - lastElapsedRealtimeMillis

        // A non-positive elapsed interval is invalid; establish a fresh baseline.
        if (elapsedMillis <= 0L) {
            updateBaseline(currentRxBytes, currentTxBytes, elapsedRealtimeMillis)
            return LiveSpeed(0L, 0L, wallClockMillis)
        }

        // Ignore very short intervals without advancing the baseline. The next valid
        // sample can then use the full interval instead of losing those bytes.
        if (elapsedMillis <= minimumIntervalMillis) {
            return LiveSpeed(0L, 0L, wallClockMillis)
        }

        val elapsedSeconds = elapsedMillis / 1000.0
        val downloadBytesPerSecond = ((currentRxBytes - lastRxBytes) / elapsedSeconds).toLong()
        val uploadBytesPerSecond = ((currentTxBytes - lastTxBytes) / elapsedSeconds).toLong()

        updateBaseline(currentRxBytes, currentTxBytes, elapsedRealtimeMillis)

        return LiveSpeed(
            downloadBytesPerSec = downloadBytesPerSecond.coerceAtLeast(0L),
            uploadBytesPerSec = uploadBytesPerSecond.coerceAtLeast(0L),
            timestamp = wallClockMillis
        )
    }

    @Synchronized
    fun reset() {
        lastRxBytes = UNINITIALIZED
        lastTxBytes = UNINITIALIZED
        lastElapsedRealtimeMillis = UNINITIALIZED
    }

    private fun hasNoBaseline(): Boolean =
        lastRxBytes == UNINITIALIZED ||
            lastTxBytes == UNINITIALIZED ||
            lastElapsedRealtimeMillis == UNINITIALIZED

    private fun updateBaseline(rxBytes: Long, txBytes: Long, elapsedRealtimeMillis: Long) {
        lastRxBytes = rxBytes
        lastTxBytes = txBytes
        lastElapsedRealtimeMillis = elapsedRealtimeMillis
    }

    private companion object {
        const val UNINITIALIZED = -1L
    }
}
