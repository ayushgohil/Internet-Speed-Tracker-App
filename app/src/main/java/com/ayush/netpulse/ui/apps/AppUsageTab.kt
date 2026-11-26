package com.ayush.netpulse.ui.apps

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.ui.draw.clip
import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import com.ayush.netpulse.ui.theme.NetPulseDimensions
import com.ayush.netpulse.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.util.Calendar

private data class AppUsage(val uid: Int, val name: String, val mobileRx: Long, val mobileTx: Long,
    val wifiRx: Long, val wifiTx: Long, val icon: ImageBitmap? = null) {
    val mobile get() = mobileRx + mobileTx
    val wifi get() = wifiRx + wifiTx
    val total get() = mobile + wifi
}

@Suppress("DEPRECATION")
private suspend fun loadApps(context: Context, days: Int): List<AppUsage> = withContext(Dispatchers.IO) {
    val manager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    val start = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1 - days)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = System.currentTimeMillis()
    val totals = mutableMapOf<Int, LongArray>()
    for ((type, subscriber, offset) in listOf(
        Triple(ConnectivityManager.TYPE_MOBILE, null, 0),
        Triple(ConnectivityManager.TYPE_WIFI, "", 2))) {
        val stats = manager.querySummary(type, subscriber, start, end)
        try {
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket() && stats.getNextBucket(bucket)) {
                val bytes = totals.getOrPut(bucket.uid) { LongArray(4) }
                bytes[offset] += bucket.rxBytes.coerceAtLeast(0)
                bytes[offset + 1] += bucket.txBytes.coerceAtLeast(0)
            }
        } finally { stats.close() }
    }
    val pm = context.packageManager
    val launcherPackages = pm.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER), 0)
        .groupBy { it.activityInfo.applicationInfo.uid }
    totals.map { (uid, bytes) ->
        val packages = (pm.getPackagesForUid(uid).orEmpty().toList() + launcherPackages[uid].orEmpty().map { it.activityInfo.packageName }).distinct().sorted()
        val name = when {
            uid == -5 -> "Tethering / hotspot"
            uid == -4 -> "Removed apps"
            packages.size > 1 -> "Shared UID $uid (${packages.size} apps)"
            packages.isEmpty() -> if (uid < 10000) "System traffic ($uid)" else "Unidentified app ($uid)"
            else -> try { pm.getApplicationLabel(pm.getApplicationInfo(packages.first(), 0)).toString() }
                catch (_: Exception) { packages.first() }
        }
        val icon = if (packages.size == 1) try { pm.getApplicationIcon(packages.first()).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null } else null
        AppUsage(uid, name, bytes[0], bytes[1], bytes[2], bytes[3], icon)
    }.filter { it.total > 0 }.sortedByDescending { it.total }
}

private data class CachedApps(val records: List<AppUsage>, val loadedAt: Long, val date: String, val updatedAt: Long)
private val appCache = android.util.LruCache<Int, CachedApps>(3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageTab(hasPermission: Boolean, onRequestUsageAccess: () -> Unit, isVisible: Boolean) {
    val context = LocalContext.current
    val colors = LocalNetPulseColors.current
    var days by rememberSaveable { mutableStateOf(1) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf(emptyList<AppUsage>()) }
    var loadedDays by remember { mutableStateOf(0) }
    var lastUpdated by remember { mutableStateOf(0L) }
    var selected by remember { mutableStateOf<AppUsage?>(null) }
    LaunchedEffect(hasPermission, days, refresh, isVisible) {
        if (!hasPermission) { appCache.evictAll(); records = emptyList(); loadedDays = 0; selected = null; loading = false; return@LaunchedEffect }
        if (!isVisible) { loading = false; return@LaunchedEffect }
        val date = java.time.LocalDate.now().toString()
        val cached = appCache.get(days)?.takeIf { it.date == date }
        records = cached?.records.orEmpty()
        lastUpdated = cached?.updatedAt ?: 0L
        loadedDays = if (cached != null) days else 0
        error = null
        selected = null
        if (cached != null && refresh == 0 && SystemClock.elapsedRealtime() - cached.loadedAt < 60_000) return@LaunchedEffect
        loading = true
        try {
            val result = loadApps(context, days)
            records = result
            loadedDays = days
            lastUpdated = System.currentTimeMillis()
            appCache.put(days, CachedApps(result, SystemClock.elapsedRealtime(), date, lastUpdated))
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "Android could not return app usage. Check Usage Access and retry." }
        finally { loading = false }
    }
    if (!hasPermission) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("App usage", style = MaterialTheme.typography.headlineSmall)
            Text("Allow Usage Access to see Android's per-app data statistics.")
            Button(onClick = onRequestUsageAccess) { Text("Grant access") }
        }
        return
    }
    fun amount(app: AppUsage) = when (filter) { "Mobile" -> app.mobile; "Wi-Fi" -> app.wifi; else -> app.total }
    val total = records.sumOf { amount(it) }
    val visible = (if (loadedDays == days) records else emptyList()).filter { amount(it) > 0 && it.name.contains(search, ignoreCase = true) }.sortedByDescending { amount(it) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = NetPulseDimensions.screenPadding), verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App usage", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Refresh") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Today", 7 to "7 days", 30 to "30 days").forEach { (value, label) ->
                    FilterChip(selected = days == value, onClick = { days = value }, label = { Text(label) })
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Reported usage · $filter", style = MaterialTheme.typography.labelMedium)
                    Text(if (loadedDays != days) "—" else NetworkUtils.formatBytes(total),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (lastUpdated > 0) Text("Updated " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(lastUpdated)), style = MaterialTheme.typography.labelSmall)
                    Text("UID totals can differ from device totals due to attribution, VPN, tethering and query timing. Hidden apps may be grouped.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Mobile", "Wi-Fi").forEach { value ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                }
            }
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Search apps") })
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (!loading && error == null && visible.isEmpty()) item { Text("No matching usage records.") }
        items(visible, key = { it.uid }) { app ->
            Card(onClick = { selected = app }, shape = RoundedCornerShape(NetPulseDimensions.cardRadius), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(NetPulseDimensions.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (app.icon != null) Image(app.icon, contentDescription = null, modifier = Modifier.size(NetPulseDimensions.iconSize).clip(RoundedCornerShape(10.dp)))
                            else Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.Apps, contentDescription = null) }
                            Text(app.name, fontWeight = FontWeight.SemiBold)
                        }
                        Text(NetworkUtils.formatBytes(amount(app)))
                    }
                    Text("Mobile ${NetworkUtils.formatBytes(app.mobile)}", color = colors.upload,
                        style = MaterialTheme.typography.labelSmall)
                    Text("Wi-Fi ${NetworkUtils.formatBytes(app.wifi)}", color = colors.download,
                        style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(progress = { if (total > 0) (amount(app).toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    selected?.let { app ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(app.name) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mobile download: ${NetworkUtils.formatBytes(app.mobileRx)}")
                Text("Mobile upload: ${NetworkUtils.formatBytes(app.mobileTx)}")
                Text("Wi-Fi download: ${NetworkUtils.formatBytes(app.wifiRx)}")
                Text("Wi-Fi upload: ${NetworkUtils.formatBytes(app.wifiTx)}")
                Text("Total: ${NetworkUtils.formatBytes(app.total)}")
            } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } })
    }
}
