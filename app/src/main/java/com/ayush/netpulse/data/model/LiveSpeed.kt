package com.ayush.netpulse.data.model

import com.ayush.netpulse.util.NetworkUtils

data class LiveSpeed(
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedDownload: String get() = NetworkUtils.formatSpeed(downloadBytesPerSec)
    val formattedUpload: String get() = NetworkUtils.formatSpeed(uploadBytesPerSec)
}
