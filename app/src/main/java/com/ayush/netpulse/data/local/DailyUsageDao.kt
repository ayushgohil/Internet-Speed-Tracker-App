package com.ayush.netpulse.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyUsage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyUsage(limit: Int = 90): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC")
    fun getAllDailyUsage(): Flow<List<DailyUsageEntity>>

    @Query("DELETE FROM daily_usage WHERE date = :date")
    suspend fun deleteUsageForDate(date: String): Int
}
