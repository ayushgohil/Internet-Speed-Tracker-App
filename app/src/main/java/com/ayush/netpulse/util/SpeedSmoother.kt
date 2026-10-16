package com.ayush.netpulse.util

import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType

/**
 * Provides time-window moving average smoothing for visual speed stability on the Dashboard,
 * preventing erratic single-sample jumps while preserving raw readings for logs and diagnostics.
 */
class SpeedSmoother(
    private val windowSize: Int = 3 // 3-second smoothing window at 1Hz sampling
) {
    private val downloadHistory = ArrayDeque<Long>(windowSize)
    private val uploadHistory = ArrayDeque<Long>(windowSize)
    private var lastNetworkType: NetworkType? = null
    private var consecutiveZeroCount = 0

    @Synchronized
    fun smooth(rawSpeed: LiveSpeed, currentNetworkType: NetworkType): LiveSpeed {
        // 1. Offline / Disconnected: zero immediately without stale smoothing
        if (currentNetworkType == NetworkType.DISCONNECTED) {
            reset()
            lastNetworkType = currentNetworkType
            return LiveSpeed(0L, 0L, rawSpeed.timestamp)
        }

        // 2. Transport switch (e.g. Wi-Fi <-> Mobile): reset smoothing history
        if (lastNetworkType != null && lastNetworkType != currentNetworkType) {
            reset()
        }
        lastNetworkType = currentNetworkType

        val rawDown = rawSpeed.downloadBytesPerSec.coerceAtLeast(0L)
        val rawUp = rawSpeed.uploadBytesPerSec.coerceAtLeast(0L)

        if (rawDown == 0L && rawUp == 0L) {
            consecutiveZeroCount++
            // 3. Avoid stale non-zero readings when connection becomes idle:
            // Two consecutive zeros immediately reset history to 0 without sluggish trailing
            if (consecutiveZeroCount >= 2) {
                reset()
                return LiveSpeed(0L, 0L, rawSpeed.timestamp)
            }
        } else {
            consecutiveZeroCount = 0
        }

        // Maintain sliding window
        if (downloadHistory.size >= windowSize) {
            downloadHistory.removeFirst()
        }
        if (uploadHistory.size >= windowSize) {
            uploadHistory.removeFirst()
        }

        downloadHistory.addLast(rawDown)
        uploadHistory.addLast(rawUp)

        val avgDown = downloadHistory.average().toLong()
        val avgUp = uploadHistory.average().toLong()

        // 4. Do not round small non-zero traffic to zero prematurely
        val finalDown = if (rawDown > 0L && avgDown == 0L) rawDown else avgDown
        val finalUp = if (rawUp > 0L && avgUp == 0L) rawUp else avgUp

        return LiveSpeed(finalDown, finalUp, rawSpeed.timestamp)
    }

    @Synchronized
    fun reset() {
        downloadHistory.clear()
        uploadHistory.clear()
        consecutiveZeroCount = 0
    }
}
