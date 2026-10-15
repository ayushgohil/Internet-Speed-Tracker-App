package com.ayush.netpulse.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ayush.netpulse.util.NetworkUtils

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey
    val date: String, // ISO date string in local timezone: "yyyy-MM-dd"
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    val totalMobileBytes: Long get() = mobileRxBytes + mobileTxBytes
    val totalWifiBytes: Long get() = wifiRxBytes + wifiTxBytes
    val totalAllBytes: Long get() = totalMobileBytes + totalWifiBytes

    val formattedMobile: String get() = NetworkUtils.formatBytes(totalMobileBytes)
    val formattedWifi: String get() = NetworkUtils.formatBytes(totalWifiBytes)
    val formattedTotal: String get() = NetworkUtils.formatBytes(totalAllBytes)
}
