package com.ayush.netpulse.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import com.ayush.netpulse.data.local.SpeedRecordDao
import com.ayush.netpulse.data.local.SpeedRecordEntity
import com.ayush.netpulse.data.model.DataUsage
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

class NetworkRepository(
    private val context: Context,
    private val speedRecordDao: SpeedRecordDao
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var lastTimestamp: Long = -1L

    /**
     * Samples live speed using TrafficStats deltas
     */
    fun sampleCurrentSpeed(): LiveSpeed {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        if (currentRx == TrafficStats.UNSUPPORTED.toLong() || currentTx == TrafficStats.UNSUPPORTED.toLong()) {
            return LiveSpeed(0L, 0L, currentTime)
        }

        if (lastRxBytes < 0 || lastTxBytes < 0 || lastTimestamp < 0) {
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastTimestamp = currentTime
            return LiveSpeed(0L, 0L, currentTime)
        }

        val elapsedMillis = currentTime - lastTimestamp
        if (elapsedMillis <= 0) {
            return LiveSpeed(0L, 0L, currentTime)
        }

        val elapsedSeconds = elapsedMillis / 1000.0
        val rxDelta = (currentRx - lastRxBytes).coerceAtLeast(0L)
        val txDelta = (currentTx - lastTxBytes).coerceAtLeast(0L)

        val downBps = (rxDelta / elapsedSeconds).toLong()
        val upBps = (txDelta / elapsedSeconds).toLong()

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTimestamp = currentTime

        return LiveSpeed(downBps, upBps, currentTime)
    }

    /**
     * Resets delta baseline when tracking stops or resumes
     */
    fun resetSpeedBaseline() {
        lastRxBytes = -1L
        lastTxBytes = -1L
        lastTimestamp = -1L
    }

    /**
     * Observes real-time network connectivity type changes
     */
    fun observeNetworkType(): Flow<NetworkType> = callbackFlow {
        fun getCurrentNetworkType(): NetworkType {
            val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.DISCONNECTED
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.DISCONNECTED
            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
        }

        trySend(getCurrentNetworkType())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkType())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentNetworkType())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getCurrentNetworkType())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * Queries daily Wi-Fi and Mobile data usage from NetworkStatsManager
     */
    suspend fun getTodayDataUsage(): Result<DataUsage> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.hasUsageStatsPermission(context)) {
            return@withContext Result.failure(
                SecurityException("Usage Access permission (PACKAGE_USAGE_STATS) is not granted.")
            )
        }

        if (networkStatsManager == null) {
            return@withContext Result.failure(
                IllegalStateException("NetworkStatsManager is unavailable on this device.")
            )
        }

        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            var wifiRx = 0L
            var wifiTx = 0L
            var mobileRx = 0L
            var mobileTx = 0L

            // Wi-Fi usage
            try {
                val wifiBucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    startOfDay,
                    now
                )
                wifiRx = wifiBucket.rxBytes
                wifiTx = wifiBucket.txBytes
            } catch (e: Exception) {
                Log.w("NetworkRepository", "Could not query Wi-Fi summary: ${e.message}")
            }

            // Mobile usage
            try {
                val mobileBucket = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE,
                    null,
                    startOfDay,
                    now
                )
                mobileRx = mobileBucket.rxBytes
                mobileTx = mobileBucket.txBytes
            } catch (e: Exception) {
                Log.w("NetworkRepository", "Could not query Mobile data summary: ${e.message}")
            }

            Result.success(
                DataUsage(
                    mobileRxBytes = mobileRx,
                    mobileTxBytes = mobileTx,
                    wifiRxBytes = wifiRx,
                    wifiTxBytes = wifiTx,
                    queryStartTime = startOfDay,
                    queryEndTime = now
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Persists a speed record to Room
     */
    suspend fun saveSpeedRecord(speed: LiveSpeed, networkType: NetworkType) {
        withContext(Dispatchers.IO) {
            try {
                speedRecordDao.insert(
                    SpeedRecordEntity(
                        timestamp = speed.timestamp,
                        downloadBps = speed.downloadBytesPerSec,
                        uploadBps = speed.uploadBytesPerSec,
                        networkType = networkType.name
                    )
                )
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error saving speed record: ${e.message}")
            }
        }
    }

    /**
     * Flow of recent speed records from Room
     */
    fun getRecentSpeedRecords(limit: Int = 10): Flow<List<SpeedRecordEntity>> {
        return speedRecordDao.getRecentRecords(limit)
    }
}
