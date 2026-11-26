package com.ayush.netpulse.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.util.ConnectionHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private fun csvCell(value: Any?): String {
    val raw = value?.toString().orEmpty()
    val safe = if (raw.startsWith("=") || raw.startsWith("+") || raw.startsWith("-") || raw.startsWith("@") || raw.startsWith("\t") || raw.startsWith("\r")) "'$raw" else raw
    return "\"" + safe.replace("\"", "\"\"") + "\""
}

@Composable
internal fun DataExportCard(
    uiState: DashboardUiState,
    events: List<ConnectionHistory.Event>,
    tests: List<SpeedTestHistory.Entry>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val daily = if (uiState.hasUsageAccessPermission) uiState.recentDailyRecords else emptyList()
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val data = pending
        pending = null
        if (uri == null || data == null) { busy = false; status = "Export cancelled." }
        else scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open file")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(data) }
                }
                status = "CSV saved successfully."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status = "Export failed. Try another destination." }
            finally { busy = false }
        }
    }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val data = pending
        pending = null
        if (uri == null || data == null) { busy = false; status = "Export cancelled." }
        else scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open file")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(data) }
                }
                status = "JSON saved successfully."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status = "Export failed. Try another destination." }
            finally { busy = false }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Export data", style = MaterialTheme.typography.titleMedium)
            Text("Save recorded daily usage as CSV, or a JSON snapshot of available daily records, recent speed samples, test results and connection events. Choose a destination using Android's file picker.",
                style = MaterialTheme.typography.bodySmall)
            Text("${daily.size} daily records · ${tests.size} tests · ${events.size} events",
                style = MaterialTheme.typography.labelSmall)
            OutlinedButton(enabled = !busy && daily.isNotEmpty(), onClick = {
                pending = buildString {
                    append("date,mobile_rx_bytes,mobile_tx_bytes,wifi_rx_bytes,wifi_tx_bytes,total_bytes\r\n")
                    daily.sortedBy { it.date }.forEach {
                        append(listOf(it.date, it.mobileRxBytes, it.mobileTxBytes, it.wifiRxBytes, it.wifiTxBytes, it.totalAllBytes).joinToString(",") { value -> csvCell(value) })
                        append("\r\n")
                    }
                }
                busy = true
                try { csv.launch("netpulse-usage-${LocalDate.now()}.csv") }
                catch (_: Exception) { busy = false; pending = null; status = "File picker unavailable." }
            }) { Text("Export usage CSV") }
            OutlinedButton(enabled = !busy, onClick = {
                val root = JSONObject().put("schema_version", 1).put("exported_at_ms", System.currentTimeMillis())
                val usage = JSONArray()
                daily.forEach {
                    usage.put(JSONObject().put("date", it.date).put("mobile_rx_bytes", it.mobileRxBytes)
                        .put("mobile_tx_bytes", it.mobileTxBytes).put("wifi_rx_bytes", it.wifiRxBytes)
                        .put("wifi_tx_bytes", it.wifiTxBytes))
                }
                val samples = JSONArray()
                uiState.recentRecords.forEach {
                    samples.put(JSONObject().put("timestamp_ms", it.timestamp).put("download_bytes_per_second", it.downloadBps)
                        .put("upload_bytes_per_second", it.uploadBps).put("network", it.networkType))
                }
                val speedTests = JSONArray()
                tests.forEach {
                    speedTests.put(JSONObject().put("timestamp_ms", it.timestamp).put("download_mbps", it.download)
                        .put("upload_mbps", it.upload).put("network", it.network))
                }
                val connections = JSONArray()
                events.forEach {
                    connections.put(JSONObject().put("timestamp_ms", it.time).put("state", it.state)
                        .put("previous_duration_ms", it.durationMs ?: JSONObject.NULL))
                }
                pending = root.put("daily_usage", usage).put("recent_speed_samples", samples)
                    .put("speed_tests", speedTests).put("connection_events", connections).toString(2)
                busy = true
                try { json.launch("netpulse-snapshot-${LocalDate.now()}.json") }
                catch (_: Exception) { busy = false; pending = null; status = "File picker unavailable." }
            }) { Text("Export JSON snapshot") }
            Text("Exports are not restorable backups. Files may contain private usage and connection history.",
                style = MaterialTheme.typography.labelSmall)
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
