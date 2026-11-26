package com.ayush.netpulse.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.dashboard.NetworkDetailsCard
import com.ayush.netpulse.util.ConnectionHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkToolsTab(uiState: DashboardUiState, isVisible: Boolean, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val events by ConnectionHistory.events.collectAsState()
    var active by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var speed by remember { mutableStateOf<String?>(null) }
    var latency by remember { mutableStateOf<String?>(null) }
    var dns by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var testHistory by remember { mutableStateOf(SpeedTestHistory.read(context)) }
    var confirmClearTests by remember { mutableStateOf(false) }
    var showAllTests by remember { mutableStateOf(false) }
    var confirmSpeed by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var onlyOutages by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) ConnectionHistory.load(context) else job?.cancel()
    }
    fun runTest(name: String, block: suspend () -> Unit) {
        if (active != null) return
        active = name
        error = null
        job = scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "$name failed. Check your connection or try again." }
            finally { active = null }
        }
    }
    fun format(value: Double?) = value?.let { String.format(Locale.US, "%.1f", it) } ?: "Unavailable"
    val outages = events.count { it.state == "Disconnected" || it.state.endsWith("Unvalidated") || it.state.endsWith("Sign-in required") }
    val shown = if (onlyOutages) events.filter { it.state == "Disconnected" || it.state.endsWith("Unvalidated") || it.state.endsWith("Sign-in required") } else events
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Network tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Active tests are optional and contact speed.cloudflare.com. Normal monitoring remains local.",
                style = MaterialTheme.typography.bodySmall)
        }
        item {
            ToolCard("Internet speed test", "Estimate transfer throughput using a bounded 5 MB download and 1 MB upload. Short tests may underestimate fast connections.",
                speed, "Run test", active == null) { confirmSpeed = true }
        }
        item {
            ToolCard("TCP latency", "Five TCP connection attempts to Cloudflare. Reports latency, variation and failed attempts; not ICMP ping.",
                latency, "Measure latency", active == null) {
                latency = null
                runTest("Latency") {
                    val result = NetworkTests.latency()
                    latency = "Average: ${format(result.average)} ms\nVariation: ${format(result.jitter)} ms\nFailed: ${result.attempts - result.samples.size}/${result.attempts}"
                }
            }
        }
        item {
            ToolCard("DNS lookup", "Resolve the test hostname using the device resolver. Results may be cached.",
                dns, "Resolve", active == null) {
                dns = null
                runTest("DNS") {
                    val result = NetworkTests.dns()
                    dns = "${result.first} ms\n" + result.second.joinToString("\n")
                }
            }
        }
        if (active != null) item {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Running $active…")
                TextButton(onClick = { job?.cancel() }) { Text("Cancel") }
            }
        }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Speed test history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${testHistory.size} saved results · up to 50 on this device", style = MaterialTheme.typography.bodySmall)
                    if (testHistory.isEmpty()) Text("Complete a speed test to save a result.")
                    (if (showAllTests) testHistory else testHistory.take(3)).forEach { entry ->
                        HorizontalDivider()
                        Text(DateFormat.getDateTimeInstance().format(Date(entry.timestamp)) + " · " + entry.network,
                            style = MaterialTheme.typography.labelSmall)
                        Text("↓ ${format(entry.download)} Mbps   ↑ ${format(entry.upload)} Mbps")
                    }
                    Row {
                        if (testHistory.size > 3) TextButton(onClick = { showAllTests = !showAllTests }) {
                            Text(if (showAllTests) "Show less" else "Show all")
                        }
                        TextButton(onClick = { confirmClearTests = true }, enabled = testHistory.isNotEmpty()) { Text("Clear") }
                    }
                }
            }
        }
        item { WifiAnalyzerCard() }
        item { CellularDetailsCard() }
        item { LocalDeviceDiscoveryCard(isVisible) }
        item { DataExportCard(uiState, events, testHistory) }
        item { UsageBackupCard(uiState.hasUsageAccessPermission) }
        item { NetworkDetailsCard(uiState.networkType) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connection history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$outages unvalidated / disconnected observations · ${events.size} events")
                    Text("Recorded while background monitoring runs. Up to 500 events; gaps are not measured outages.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !onlyOutages, onClick = { onlyOutages = false }, label = { Text("All") })
                        FilterChip(selected = onlyOutages, onClick = { onlyOutages = true }, label = { Text("Issues") })
                        TextButton(onClick = { confirmClear = true }, enabled = events.isNotEmpty()) { Text("Clear") }
                    }
                }
            }
        }
        if (shown.isEmpty()) item { Text("No matching connection events recorded.") }
        items(shown) { event ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(event.state, fontWeight = FontWeight.SemiBold)
                    Text(DateFormat.getDateTimeInstance().format(Date(event.time)), style = MaterialTheme.typography.bodySmall)
                    event.durationMs?.let { Text("Previous state: ${it / 1000}s observed",
                        style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        item {
            ToolCard("Monitoring diagnostics", "Check service status, permissions, and recovery details.",
                uiState.diagnosticsInfo.resolvedServiceStatus, "Open settings", true, onOpenSettings)
        }
    }
    if (confirmSpeed) AlertDialog(onDismissRequest = { confirmSpeed = false },
        title = { Text("Run internet speed test?") },
        text = { Text("This contacts Cloudflare and transfers approximately 6 MB plus protocol overhead, including on mobile data.") },
        confirmButton = { TextButton(onClick = {
            confirmSpeed = false
            speed = null
            runTest("Speed test") {
                val network = uiState.networkType.displayName
                val result = NetworkTests.speed()
                testHistory = SpeedTestHistory.add(context, SpeedTestHistory.Entry(System.currentTimeMillis(), result.downloadMbps, result.uploadMbps, network))
                speed = "Download: ${format(result.downloadMbps)} Mbps\nUpload: ${format(result.uploadMbps)} Mbps"
            }
        }) { Text("Start") } }, dismissButton = { TextButton(onClick = { confirmSpeed = false }) { Text("Cancel") } })
    if (confirmClearTests) AlertDialog(onDismissRequest = { confirmClearTests = false },
        title = { Text("Clear speed test history?") },
        text = { Text("Saved speed test results will be removed from this device.") },
        confirmButton = { TextButton(onClick = {
            SpeedTestHistory.clear(context)
            testHistory = emptyList()
            confirmClearTests = false
        }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClearTests = false }) { Text("Cancel") } })
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Clear connection history?") }, text = { Text("Only connection events will be removed.") },
        confirmButton = { TextButton(onClick = { ConnectionHistory.clear(context); confirmClear = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
}

@Composable
private fun ToolCard(title: String, description: String, result: String?, action: String, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
            result?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            OutlinedButton(onClick = onClick, enabled = enabled) { Text(action) }
        }
    }
}
