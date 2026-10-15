package com.ayush.netpulse.data.repository

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.util.Log
import com.ayush.netpulse.data.local.DailyUsageDao
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.data.local.SpeedRecordDao
import com.ayush.netpulse.data.local.SpeedRecordEntity
import com.ayush.netpulse.data.model.DataUsage
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.util.NetworkUtils
import com.ayush.netpulse.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

class NetworkRepository(
    private val context: Context,
    private val speedRecordDao: SpeedRecordDao,
    private val dailyUsageDao: DailyUsageDao,
    private val preferencesManager: PreferencesManager
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
     * Multi-day historical usage recovery:
     * Reconciles usage for today, yesterday, and any missed calendar days up to maxLookbackDays.
     * Protects existing non-zero records from being overwritten with temporary zeros.
     */
    suspend fun reconcileMultiDayHistoricalUsage(maxLookbackDays: Int = 14): Result<Int> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.hasUsageStatsPermission(context)) {
            val err = "Usage Access permission is not granted."
            preferencesManager.lastReconciliationStatus = "Failed: $err"
            return@withContext Result.failure(SecurityException(err))
        }

        if (networkStatsManager == null) {
            val err = "NetworkStatsManager is unavailable on this device."
            preferencesManager.lastReconciliationStatus = "Failed: $err"
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            var daysReconciled = 0
            val now = System.currentTimeMillis()

            // Look back up to maxLookbackDays, or at least 1 day back (yesterday) for delayed carrier logs
            for (offset in (maxLookbackDays - 1) downTo 0) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis

                val endOfDay = if (offset == 0) {
                    now
                } else {
                    cal.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }

                val dateStr = NetworkUtils.formatDateString(startOfDay)

                var wifiRx = 0L
                var wifiTx = 0L
                var mobileRx = 0L
                var mobileTx = 0L

                // Wi-Fi query
                try {
                    @Suppress("DEPRECATION")
                    val wifiBucket = networkStatsManager.querySummaryForDevice(
                        ConnectivityManager.TYPE_WIFI,
                        "",
                        startOfDay,
                        endOfDay
                    )
                    wifiRx = wifiBucket.rxBytes
                    wifiTx = wifiBucket.txBytes
                } catch (e: Exception) {
                    Log.w("NetworkRepository", "Wi-Fi query error for $dateStr: ${e.message}")
                }

                // Mobile query
                try {
                    @Suppress("DEPRECATION")
                    val mobileBucket = networkStatsManager.querySummaryForDevice(
                        ConnectivityManager.TYPE_MOBILE,
                        null,
                        startOfDay,
                        endOfDay
                    )
                    mobileRx = mobileBucket.rxBytes
                    mobileTx = mobileBucket.txBytes
                } catch (e: Exception) {
                    Log.w("NetworkRepository", "Mobile data query error for $dateStr: ${e.message}")
                }

                val queryTotalBytes = wifiRx + wifiTx + mobileRx + mobileTx
                val existingRecord = dailyUsageDao.getUsageForDate(dateStr)

                // Zero-value protection: Avoid replacing valid stored historical data with zero if stats are temporarily delayed
                if (offset > 0 && queryTotalBytes == 0L && existingRecord != null && existingRecord.totalAllBytes > 0L) {
                    Log.d("NetworkRepository", "Skipping zero-overwrite for $dateStr; retaining existing ${existingRecord.formattedTotal}")
                } else if (queryTotalBytes > 0L || existingRecord == null || offset == 0) {
                    dailyUsageDao.upsert(
                        DailyUsageEntity(
                            date = dateStr,
                            mobileRxBytes = mobileRx,
                            mobileTxBytes = mobileTx,
                            wifiRxBytes = wifiRx,
                            wifiTxBytes = wifiTx,
                            lastUpdatedTimestamp = now
                        )
                    )
                    daysReconciled++
                }
            }

            val todayStr = NetworkUtils.formatDateString(now)
            preferencesManager.lastReconciledDate = todayStr
            preferencesManager.lastReconciledTimestamp = now
            preferencesManager.lastReconciliationStatus = "Success ($daysReconciled days updated at ${NetworkUtils.formatDateString(now)})"

            Result.success(daysReconciled)
        } catch (e: Exception) {
            preferencesManager.lastReconciliationStatus = "Error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * Queries daily Wi-Fi and Mobile data usage from NetworkStatsManager for today,
     * and triggers multi-day reconciliation.
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
            // First reconcile any missing days
            reconcileMultiDayHistoricalUsage(maxLookbackDays = 7)

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

            try {
                @Suppress("DEPRECATION")
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

            try {
                @Suppress("DEPRECATION")
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

    /**
     * Flow of recent daily usage summaries from Room
     */
    fun getRecentDailyUsage(limit: Int = 7): Flow<List<DailyUsageEntity>> {
        return dailyUsageDao.getRecentDailyUsage(limit)
    }
}
