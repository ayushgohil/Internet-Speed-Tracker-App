package com.ayush.netpulse

import android.app.Application
import com.ayush.netpulse.data.local.NetPulseDatabase
import com.ayush.netpulse.data.repository.NetworkRepository
import com.ayush.netpulse.util.PreferencesManager

class NetPulseApp : Application() {

    val database: NetPulseDatabase by lazy {
        NetPulseDatabase.getDatabase(this)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager.getInstance(this)
    }

    val networkRepository: NetworkRepository by lazy {
        NetworkRepository(
            context = applicationContext,
            speedRecordDao = database.speedRecordDao(),
            dailyUsageDao = database.dailyUsageDao(),
            preferencesManager = preferencesManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.ayush.netpulse.worker.LogRetentionWorker.enqueuePeriodic(this)
    }

    companion object {
        lateinit var instance: NetPulseApp
            private set
    }
}
