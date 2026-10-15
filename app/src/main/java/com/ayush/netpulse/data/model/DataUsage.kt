package com.ayush.netpulse.data.model

import com.ayush.netpulse.util.NetworkUtils

data class DataUsage(
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val queryStartTime: Long = 0L,
    val queryEndTime: Long = 0L
) {
    val totalMobileBytes: Long get() = mobileRxBytes + mobileTxBytes
    val totalWifiBytes: Long get() = wifiRxBytes + wifiTxBytes
    val totalAllBytes: Long get() = totalMobileBytes + totalWifiBytes

    val formattedMobile: String get() = NetworkUtils.formatBytes(totalMobileBytes)
    val formattedWifi: String get() = NetworkUtils.formatBytes(totalWifiBytes)
    val formattedTotal: String get() = NetworkUtils.formatBytes(totalAllBytes)
}
