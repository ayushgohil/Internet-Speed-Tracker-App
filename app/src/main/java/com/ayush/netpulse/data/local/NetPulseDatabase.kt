package com.ayush.netpulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SpeedRecordEntity::class, DailyUsageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NetPulseDatabase : RoomDatabase() {

    abstract fun speedRecordDao(): SpeedRecordDao
    abstract fun dailyUsageDao(): DailyUsageDao

    companion object {
        @Volatile
        private var INSTANCE: NetPulseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_usage (
                        date TEXT PRIMARY KEY NOT NULL,
                        mobileRxBytes INTEGER NOT NULL,
                        mobileTxBytes INTEGER NOT NULL,
                        wifiRxBytes INTEGER NOT NULL,
                        wifiTxBytes INTEGER NOT NULL,
                        lastUpdatedTimestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): NetPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetPulseDatabase::class.java,
                    "netpulse_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
