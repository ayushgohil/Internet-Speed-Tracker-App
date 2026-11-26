package com.ayush.netpulse.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.data.local.NetPulseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Portable daily-usage archive. Import never deletes or reduces an existing counter. */
internal object UsageBackup {
    private const val MAX_ROWS = 10000
    private const val MAX_BYTES = 8 * 1024 * 1024

    suspend fun write(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val rows = NetPulseDatabase.getDatabase(context).dailyUsageDao().getAllDailyUsage().first()
        require(rows.size <= MAX_ROWS) { "Too many records" }
        val array = JSONArray()
        rows.forEach { row ->
            array.put(JSONObject().put("date", row.date).put("mobile_rx", row.mobileRxBytes)
                .put("mobile_tx", row.mobileTxBytes).put("wifi_rx", row.wifiRxBytes)
                .put("wifi_tx", row.wifiTxBytes).put("updated", row.lastUpdatedTimestamp))
        }
        val data = JSONObject().put("format", "netpulse_daily_usage").put("version", 1)
            .put("records", array).toString(2)
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open destination")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(data) }
        rows.size
    }

    suspend fun read(context: Context, uri: Uri): List<DailyUsageEntity> = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open file")
        val bytes = stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BYTES) { "Backup exceeds size limit" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.getString("format") == "netpulse_daily_usage" && root.getInt("version") == 1)
        val rows = root.getJSONArray("records")
        require(rows.length() <= MAX_ROWS)
        val today = LocalDate.now()
        val seen = mutableSetOf<String>()
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val date = row.getString("date")
            require(LocalDate.parse(date).toString() == date && !LocalDate.parse(date).isAfter(today) && seen.add(date))
            fun counter(key: String): Long {
                val value = row.get(key)
                require(value is Int || value is Long)
                return (value as Number).toLong().also { require(it in 0..Long.MAX_VALUE / 4) }
            }
            DailyUsageEntity(date, counter("mobile_rx"), counter("mobile_tx"), counter("wifi_rx"),
                counter("wifi_tx"), counter("updated"))
        }
    }

    suspend fun merge(context: Context, rows: List<DailyUsageEntity>): Int = withContext(Dispatchers.IO) {
        val db = NetPulseDatabase.getDatabase(context)
        db.withTransaction {
            val dao = db.dailyUsageDao()
            rows.forEach { row ->
                val old = dao.getUsageForDate(row.date)
                dao.upsert(if (old == null) row else row.copy(
                    mobileRxBytes = maxOf(old.mobileRxBytes, row.mobileRxBytes),
                    mobileTxBytes = maxOf(old.mobileTxBytes, row.mobileTxBytes),
                    wifiRxBytes = maxOf(old.wifiRxBytes, row.wifiRxBytes),
                    wifiTxBytes = maxOf(old.wifiTxBytes, row.wifiTxBytes),
                    lastUpdatedTimestamp = maxOf(old.lastUpdatedTimestamp, row.lastUpdatedTimestamp)))
            }
            rows.size
        }
    }
}
