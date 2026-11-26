package com.ayush.netpulse.ui.tools

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var page by rememberSaveable { mutableStateOf("home") }
    BackHandler(enabled = isVisible && page != "home") { page = "home" }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val events by ConnectionHistory.events.collectAsState()
    var active by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var phase by remember { mutableStateOf("Ready") }
    var measured by remember { mutableStateOf(0.0) }
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
    LaunchedEffect(page) { job?.cancel(); error = null }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(page) { listState.scrollToItem(0) }
    fun runTest(name: String, block: suspend () -> Unit) {
        if (active != null) return
        active = name
        error = null
        job = scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "$name failed. Check your connection or try again." }
            finally { if (name == "Speed test" && phase != "Completed") phase = if (error != null) "Failed" else "Stopped"; active = null }
        }
    }
    fun format(value: Double?) = value?.let { String.format(Locale.US, "%.1f", it) } ?: "Unavailable"
    val outages = events.count { it.state == "Disconnected" || it.state.endsWith("Unvalidated") || it.state.endsWith("Sign-in required") }
    val shown = if (onlyOutages) events.filter { it.state == "Disconnected" || it.state.endsWith("Unvalidated") || it.state.endsWith("Sign-in required") } else events
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), state = listState,
        contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (page == "home") {
            item {
                Text("Network tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Test, inspect and manage your connection.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Card(onClick = { page = "speed" }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Speed, null, modifier = Modifier.size(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Internet speed test", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Measure download and upload", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text("Network diagnostics", style = MaterialTheme.typography.titleMedium) }
            item { ToolGrid(listOf(ToolDestination("latency", "Latency", "TCP response", Icons.Default.Speed), ToolDestination("dns", "DNS lookup", "Name resolution", Icons.Default.Search), ToolDestination("wifi", "Wi-Fi analyzer", "Channels and signal", Icons.Default.Wifi), ToolDestination("cellular", "Cellular", "SIM and radio", Icons.Default.CellTower), ToolDestination("devices", "Local devices", "Advertised services", Icons.Default.Share), ToolDestination("connection", "Connection log", "Network events", Icons.Default.History))) { page = it } }
            item { Text("Data and monitoring", style = MaterialTheme.typography.titleMedium) }
            item { ToolGrid(listOf(ToolDestination("tests", "Test history", "Saved results", Icons.Default.History), ToolDestination("export", "Export data", "CSV and JSON", Icons.Default.Share), ToolDestination("backup", "Backup", "Save and restore", Icons.Default.Storage), ToolDestination("details", "Network details", "Current connection", Icons.Default.Info), ToolDestination("diagnostics", "Diagnostics", "Service health", Icons.Default.Build))) { page = it } }
        } else item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { page = "home" }) { Icon(Icons.Default.ArrowBack, "Back to tools") }
                Text(when (page) {
                    "speed" -> "Speed test"; "latency" -> "Latency"; "dns" -> "DNS lookup"; "wifi" -> "Wi-Fi analyzer"
                    "cellular" -> "Cellular"; "devices" -> "Local devices"; "connection" -> "Connection history"
                    "tests" -> "Test history"; "export" -> "Export data"; "backup" -> "Backup"; "details" -> "Network details"; else -> "Diagnostics"
                }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (page == "speed") item {
            SpeedGauge(phase, measured, progress, active == "Speed test")
            ToolCard("Internet speed test", "Estimate transfer throughput using a bounded 5 MB download and 1 MB upload. Short tests may underestimate fast connections.",
                speed, "Run test", active == null) { confirmSpeed = true }
        }
        if (page == "latency") item {
            ToolCard("TCP latency", "Five TCP connection attempts to Cloudflare. Reports latency, variation and failed attempts; not ICMP ping.",
                latency, "Measure latency", active == null) {
                latency = null
                runTest("Latency") {
                    val result = NetworkTests.latency()
                    latency = "Average: ${format(result.average)} ms\nVariation: ${format(result.jitter)} ms\nFailed: ${result.attempts - result.samples.size}/${result.attempts}"
                }
            }
        }
        if (page == "dns") item {
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
        if (page == "tests") item {
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
        if (page == "wifi") item { WifiAnalyzerCard() }
        if (page == "cellular") item { CellularDetailsCard() }
        if (page == "devices") item { LocalDeviceDiscoveryCard(isVisible) }
        if (page == "export") item { DataExportCard(uiState, events, testHistory) }
        if (page == "backup") item { UsageBackupCard(uiState.hasUsageAccessPermission) }
        if (page == "details") item { NetworkDetailsCard(uiState.networkType) }
        if (page == "connection") item {
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
        if (page == "connection" && shown.isEmpty()) item { Text("No matching connection events recorded.") }
        if (page == "connection") items(shown) { event ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.History, null, tint = if (event.state == "Disconnected" || event.state.endsWith("Unvalidated") || event.state.endsWith("Sign-in required")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                    Text(event.state, fontWeight = FontWeight.SemiBold)
                    Text(DateFormat.getDateTimeInstance().format(Date(event.time)), style = MaterialTheme.typography.bodySmall)
                    event.durationMs?.let { Text("Previous state: ${it / 1000}s observed",
                        style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        if (page == "diagnostics") item {
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
            progress = 0f
            measured = 0.0
            phase = "Connecting"
            runTest("Speed test") {
                val network = uiState.networkType.displayName
                val result = NetworkTests.speed { step, value, fraction ->
                    androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                        phase = step
                        measured = value
                        progress = fraction
                    }
                }
                phase = "Completed"
                measured = result.downloadMbps
                progress = 1f
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

private data class ToolDestination(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

@Composable
private fun ToolGrid(destinations: List<ToolDestination>, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        destinations.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tool ->
                    Card(onClick = { onSelect(tool.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(tool.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpeedGauge(phase: String, measured: Double, progress: Float, running: Boolean) {
    val colors = com.ayush.netpulse.ui.theme.LocalNetPulseColors.current
    val accent = if (phase == "Upload") colors.upload else colors.download
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .18f)
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val displayed by androidx.compose.animation.core.animateFloatAsState(
        targetValue = measured.coerceAtLeast(0.0).toFloat(), animationSpec = androidx.compose.animation.core.tween(450), label = "measuredSpeed")
    val fraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = gaugeFraction(measured), animationSpec = androidx.compose.animation.core.tween(450), label = "needle")
    val transfer by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f), animationSpec = androidx.compose.animation.core.tween(300), label = "transfer")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("THROUGHPUT", style = MaterialTheme.typography.labelMedium)
                Text(phase.uppercase(Locale.US), color = accent, style = MaterialTheme.typography.labelMedium)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1.15f), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * .53f)
                    val radius = minOf(size.width, size.height) * .43f
                    val origin = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                    val bounds = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawArc(track, 135f, 270f, false, origin, bounds, style = stroke)
                    if (fraction > 0) drawArc(accent, 135f, 270f * fraction, false, origin, bounds, style = stroke)
                    fun point(degrees: Float, r: Float): androidx.compose.ui.geometry.Offset {
                        val angle = Math.toRadians(degrees.toDouble())
                        return center + androidx.compose.ui.geometry.Offset((kotlin.math.cos(angle) * r).toFloat(), (kotlin.math.sin(angle) * r).toFloat())
                    }
                    for (tick in 0..40) {
                        val angle = 135f + 270f * tick / 40
                        drawLine(if (tick % 5 == 0) ink else track, point(angle, radius * .83f), point(angle, radius * if (tick % 5 == 0) .72f else .78f), if (tick % 5 == 0) 2.dp.toPx() else 1.dp.toPx())
                    }
                    val angle = 135f + 270f * fraction
                    drawLine(accent.copy(alpha = .16f), point(angle, radius * .52f), point(angle, radius * .94f), 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(accent, point(angle, radius * .60f), point(angle, radius * .95f), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (running) phase else if (phase == "Completed") "Download result" else phase, style = MaterialTheme.typography.labelLarge)
                    Text(String.format(Locale.US, "%.1f", displayed), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = accent)
                    Text("Mbps", style = MaterialTheme.typography.titleSmall)
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.75f), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0", style = MaterialTheme.typography.labelSmall)
                    Text("Log scale", style = MaterialTheme.typography.labelSmall)
                    Text("1000+", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (running) {
                LinearProgressIndicator(progress = { if (phase == "Upload") .5f + transfer * .5f else transfer * .5f }, modifier = Modifier.fillMaxWidth(), color = accent)
                Text("Measured transfer · ${(transfer * 100).toInt()}% of stage", style = MaterialTheme.typography.labelSmall)
            } else Text("Short transfer estimate · about 6 MB", style = MaterialTheme.typography.labelSmall)
        }
    }
}

internal fun gaugeFraction(mbps: Double): Float =
    (kotlin.math.ln1p(mbps.coerceAtLeast(0.0)) / kotlin.math.ln1p(1000.0)).toFloat().coerceIn(0f, 1f)
