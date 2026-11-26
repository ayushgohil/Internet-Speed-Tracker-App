package com.ayush.netpulse.ui.tools

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object SpeedTestHistory {
    data class Entry(val timestamp: Long, val download: Double, val upload: Double, val network: String)
    private const val LIMIT = 50
    private fun prefs(context: Context) = context.getSharedPreferences("netpulse_test_history", Context.MODE_PRIVATE)

    @Synchronized
    fun read(context: Context): List<Entry> = try {
        val array = JSONArray(prefs(context).getString("results", "[]"))
        (0 until minOf(array.length(), LIMIT)).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val down = item.optDouble("download")
            val up = item.optDouble("upload")
            if (!down.isFinite() || !up.isFinite() || down < 0 || up < 0) null
            else Entry(item.optLong("timestamp"), down, up, item.optString("network", "Unknown"))
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun add(context: Context, entry: Entry): List<Entry> {
        val entries = (listOf(entry) + read(context)).take(LIMIT)
        val array = JSONArray()
        entries.forEach {
            array.put(JSONObject().put("timestamp", it.timestamp).put("download", it.download)
                .put("upload", it.upload).put("network", it.network))
        }
        prefs(context).edit().putString("results", array.toString()).apply()
        return entries
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().remove("results").apply()
    }
}
