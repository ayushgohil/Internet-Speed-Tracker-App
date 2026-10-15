package com.ayush.netpulse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SpeedRecordEntity): Long

    @Query("SELECT * FROM speed_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 20): Flow<List<SpeedRecordEntity>>

    @Query("SELECT * FROM speed_records WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getRecordsSince(since: Long): List<SpeedRecordEntity>

    @Query("DELETE FROM speed_records WHERE timestamp < :before")
    suspend fun deleteOldRecords(before: Long): Int

    @Query("SELECT COUNT(*) FROM speed_records")
    suspend fun getCount(): Int
}
