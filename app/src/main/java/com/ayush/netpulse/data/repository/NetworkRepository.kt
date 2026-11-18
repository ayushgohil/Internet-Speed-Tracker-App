package com.ayush.netpulse.data.repository

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.SystemClock
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
import com.ayush.netpulse.util.SpeedSampler
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

    private val speedSampler = SpeedSampler()
    private var lastSampledNetworkType: NetworkType? = null

    /**
     * Samples live speed using cumulative TrafficStats counters.
     * Monotonic elapsed time is used for rate calculations; wall-clock time is
     * retained only for the LiveSpeed timestamp displayed and stored by the app.
     */
    @Synchronized
    fun sampleCurrentSpeed(networkType: NetworkType? = null): LiveSpeed {
        if (networkType != null) {
            if (lastSampledNetworkType != null && lastSampledNetworkType != networkType) {
                speedSampler.reset()
            }
            lastSampledNetworkType = networkType
        }

        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        return speedSampler.sample(
            currentRxBytes = currentRx,
            currentTxBytes = currentTx,
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            wallClockMillis = System.currentTimeMillis()
        )
    }

    /**
     * Resets delta baseline when tracking stops, resumes, or changes network transport.
     */
    @Synchronized
    fun resetSpeedBaseline() {
        speedSampler.reset()
        lastSampledNetworkType = null
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

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister network callback: ${e.message}")
            }
        }
    }

    /**
     * Multi-day historical usage recovery:
     * Reconciles usage for today, yesterday, and any missed calendar days up to maxLookbackDays.
     * Protects existing non-zero records from being overwritten with temporary zeros or delayed drops.
     */
    suspend fun reconcileMultiDayHistoricalUsage(maxLookbackDays: Int = 90): Result<Int> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.hasUsageStatsPermission(context)) {
            val err = "Usage Access permission is not granted."
            preferencesManager.lastReconciliationStatus = "Failed: $err"
            preferencesManager.lastKnownError = err
            return@withContext Result.failure(SecurityException(err))
        }

        if (networkStatsManager == null) {
            val err = "NetworkStatsManager is unavailable on this device."
            preferencesManager.lastReconciliationStatus = "Failed: $err"
            preferencesManager.lastKnownError = err
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            var daysReconciled = 0
            val now = System.currentTimeMillis()

            // Look back up to maxLookbackDays, or at least 1 day back (yesterday) for delayed carrier logs
            for (offset in (maxLookbackDays - 1) downTo 0) {
                val dayStartCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = dayStartCal.timeInMillis

                val dayEndCal = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfDay = if (offset == 0) now else dayEndCal.timeInMillis

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

                if (existingRecord != null) {
                    // Zero-value & delayed drop protection: preserve larger known totals
                    val finalWifiRx = maxOf(existingRecord.wifiRxBytes, wifiRx)
                    val finalWifiTx = maxOf(existingRecord.wifiTxBytes, wifiTx)
                    val finalMobileRx = maxOf(existingRecord.mobileRxBytes, mobileRx)
                    val finalMobileTx = maxOf(existingRecord.mobileTxBytes, mobileTx)

                    dailyUsageDao.upsert(
                        DailyUsageEntity(
                            date = dateStr,
                            mobileRxBytes = finalMobileRx,
                            mobileTxBytes = finalMobileTx,
                            wifiRxBytes = finalWifiRx,
                            wifiTxBytes = finalWifiTx,
                            lastUpdatedTimestamp = now
                        )
                    )
                    daysReconciled++
                } else if (queryTotalBytes > 0L || offset == 0) {
                    // Only insert new records if real usage occurred or it's today
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
                // If past day has queryTotalBytes == 0L and existingRecord == null, leave as unknown
            }

            val todayStr = NetworkUtils.formatDateString(now)
            preferencesManager.lastReconciledDate = todayStr
            preferencesManager.lastReconciledTimestamp = now
            preferencesManager.lastReconciliationStatus = "Success ($daysReconciled days updated at ${NetworkUtils.formatDateString(now)})"

            Result.success(daysReconciled)
        } catch (e: Exception) {
            val err = "Reconciliation error: ${e.localizedMessage}"
            preferencesManager.lastReconciliationStatus = err
            preferencesManager.lastKnownError = err
            Result.failure(e)
        }
    }

    /**
     * Queries daily Wi-Fi and Mobile data usage from NetworkStatsManager for today,
     * and triggers multi-day reconciliation if stale or forced.
     */
    suspend fun getTodayDataUsage(forceReconcile: Boolean = false): Result<DataUsage> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.hasUsageStatsPermission(context)) {
            val err = "Usage Access permission (PACKAGE_USAGE_STATS) is not granted."
            preferencesManager.lastKnownError = err
            return@withContext Result.failure(SecurityException(err))
        }

        if (networkStatsManager == null) {
            val err = "NetworkStatsManager is unavailable on this device."
            preferencesManager.lastKnownError = err
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            val now = System.currentTimeMillis()
            val timeSinceLastReconciliation = now - preferencesManager.lastReconciledTimestamp
            // Reconcile multi-day history if requested, or if 15 minutes have elapsed since last check
            if (forceReconcile || timeSinceLastReconciliation > 15 * 60 * 1000L) {
                reconcileMultiDayHistoricalUsage(maxLookbackDays = 90)
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis

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

            val todayUsage = DataUsage(
                mobileRxBytes = mobileRx,
                mobileTxBytes = mobileTx,
                wifiRxBytes = wifiRx,
                wifiTxBytes = wifiTx,
                queryStartTime = startOfDay,
                queryEndTime = now
            )

            // Cache for fast and non-blocking notification rendering
            preferencesManager.cachedTodayMobileBytes = todayUsage.totalMobileBytes
            preferencesManager.cachedTodayWifiBytes = todayUsage.totalWifiBytes
            preferencesManager.cachedTodayDate = NetworkUtils.formatDateString(now)

            // Monthly data limit: mobile usage since the billing cycle started, then alert if needed
            updateCycleUsageAndAlerts(now)
            com.ayush.netpulse.widget.UsageWidgetProvider.refresh(context)

            Result.success(todayUsage)
        } catch (e: Exception) {
            preferencesManager.lastKnownError = "Today usage query error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    private fun updateCycleUsageAndAlerts(now: Long) {
        if (preferencesManager.dataLimitBytes <= 0L) return
        try {
            val cycleStart = com.ayush.netpulse.util.DataLimit.cycleStartMillis(now, preferencesManager.dataLimitCycleDay)
            @Suppress("DEPRECATION")
            val bucket = networkStatsManager?.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                cycleStart,
                now
            ) ?: return
            preferencesManager.cachedCycleMobileBytes = bucket.rxBytes + bucket.txBytes
            com.ayush.netpulse.util.DataLimitNotifier.check(context, preferencesManager)
        } catch (e: Exception) {
            Log.w("NetworkRepository", "Could not query data-limit cycle usage: ${e.message}")
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
    fun getRecentDailyUsage(limit: Int = 90): Flow<List<DailyUsageEntity>> {
        return dailyUsageDao.getRecentDailyUsage(limit)
    }

    /**
     * Purges speed records older than cutoff timestamp (without touching daily usage records)
     */
    suspend fun deleteSpeedRecordsOlderThan(cutoffTimestamp: Long): Int = withContext(Dispatchers.IO) {
        speedRecordDao.deleteOldRecords(cutoffTimestamp)
    }

    /**
     * Clears recorded speed samples from Room
     */
    suspend fun clearSpeedRecords() {
        withContext(Dispatchers.IO) {
            speedRecordDao.clearAllRecords()
        }
    }

    companion object {
        private const val TAG = "NetworkRepository"
    }
}
