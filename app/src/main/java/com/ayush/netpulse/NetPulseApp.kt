package com.ayush.netpulse

import android.app.Application
import com.ayush.netpulse.data.local.NetPulseDatabase
import com.ayush.netpulse.data.repository.NetworkRepository

class NetPulseApp : Application() {

    val database: NetPulseDatabase by lazy {
        NetPulseDatabase.getDatabase(this)
    }

    val networkRepository: NetworkRepository by lazy {
        NetworkRepository(
            context = applicationContext,
            speedRecordDao = database.speedRecordDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NetPulseApp
            private set
    }
}
