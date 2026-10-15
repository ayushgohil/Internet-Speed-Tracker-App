package com.ayush.netpulse.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ayush.netpulse.NetPulseApp
import com.ayush.netpulse.util.PreferencesManager
import java.util.concurrent.TimeUnit

class LogRetentionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? NetPulseApp ?: return Result.failure()
            val prefs = PreferencesManager.getInstance(applicationContext)
            val retentionDays = prefs.logRetentionDays.coerceAtLeast(1)

            val cutoffMillis = System.currentTimeMillis() - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
            val deletedCount = app.networkRepository.deleteSpeedRecordsOlderThan(cutoffMillis)

            prefs.lastCleanupTimestamp = System.currentTimeMillis()
            Log.d(TAG, "Log retention cleanup executed: purged $deletedCount records older than $retentionDays days (cutoff: $cutoffMillis)")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing log retention cleanup: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LogRetentionWorker"
        private const val WORK_NAME = "netpulse_log_retention_periodic"

        /**
         * Enqueues daily periodic log cleanup to maintain bounded database size.
         */
        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<LogRetentionWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Triggers an immediate one-off cleanup (e.g. when user changes retention setting).
         */
        fun triggerImmediate(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<LogRetentionWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}
