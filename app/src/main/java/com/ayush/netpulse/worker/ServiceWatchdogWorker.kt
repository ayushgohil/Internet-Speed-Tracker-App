package com.ayush.netpulse.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ayush.netpulse.service.SpeedTrackerService
import com.ayush.netpulse.util.PreferencesManager
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that verifies SpeedTrackerService is actively running
 * during system maintenance windows even if the app was stationary and offline overnight.
 */
class ServiceWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager.getInstance(applicationContext)
        val isEnabled = prefs.isBackgroundMonitoringEnabled
        val isRunning = SpeedTrackerService.isServiceRunningState.value

        Log.d(TAG, "Watchdog check: monitoringEnabled=$isEnabled, isRunning=$isRunning")
        prefs.appendRevivalLog("watchdog", "check (monitoringEnabled=$isEnabled, serviceRunning=$isRunning)")

        if (isEnabled && !isRunning) {
            Log.w(TAG, "Service is stopped while monitoring was enabled! Reviving service now...")
            try {
                prefs.appendRevivalLog("watchdog", "recovery requested")
                SpeedTrackerService.start(applicationContext, "watchdog")
            } catch (e: Throwable) {
                Log.e(TAG, "Watchdog failed to revive service directly; scheduling alarm: ${e.message}", e)
                prefs.appendRevivalLog("watchdog", "recovery exception (${e.javaClass.simpleName}: ${e.message})")
                SpeedTrackerService.scheduleRestartAlarm(applicationContext, 1000L)
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "ServiceWatchdogWorker"
        private const val WORK_NAME = "netpulse_service_watchdog_periodic"

        fun enqueuePeriodic(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
