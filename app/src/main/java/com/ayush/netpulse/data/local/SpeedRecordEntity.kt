package com.ayush.netpulse.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_records")
data class SpeedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val downloadBps: Long,
    val uploadBps: Long,
    val networkType: String
)
