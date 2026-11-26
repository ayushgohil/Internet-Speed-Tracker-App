package com.ayush.netpulse.ui.apps

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
    var sort by rememberSaveable { mutableStateOf("Total") }
    var showSort by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf(emptyList<AppUsage>()) }
    var loadedDays by remember { mutableStateOf(0) }
    var lastUpdated by remember { mutableStateOf(0L) }
    var selected by remember { mutableStateOf<AppUsage?>(null) }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ -> foreground = owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasPermission, days, refresh, isVisible, foreground) {
        if (!hasPermission) { appCache.evictAll(); records = emptyList(); loadedDays = 0; selected = null; loading = false; return@LaunchedEffect }
        if (!isVisible || !foreground) { loading = false; return@LaunchedEffect }
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
    fun amount(app: AppUsage): Long = when (filter) {
        "Mobile" -> when (sort) { "Download" -> app.mobileRx; "Upload" -> app.mobileTx; else -> app.mobile }
        "Wi-Fi" -> when (sort) { "Download" -> app.wifiRx; "Upload" -> app.wifiTx; else -> app.wifi }
        else -> when (sort) { "Download" -> app.mobileRx + app.wifiRx; "Upload" -> app.mobileTx + app.wifiTx; else -> app.total }
    }
    val current = if (loadedDays == days) records else emptyList()
    val total = current.sumOf { when (filter) { "Mobile" -> it.mobile; "Wi-Fi" -> it.wifi; else -> it.total } }
    val rankingTotal = current.sumOf { amount(it) }
    val visible = remember(current, filter, sort, search) { current.filter { amount(it) > 0 && it.name.contains(search, ignoreCase = true) }.sortedByDescending { amount(it) } }
    if (selected != null && isVisible) {
        BackHandler { selected = null }
        AppDetailsPage(selected!!, days) { selected = null }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = NetPulseDimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("App usage", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, "Refresh usage") }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reported usage · $filter", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
                    Text(if (loadedDays != days) "—" else NetworkUtils.formatBytes(total), color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    val wifi = current.sumOf { it.wifi }
                    val mobile = current.sumOf { it.mobile }
                    if (filter == "All") {
                        UsageSplitBar(wifi, mobile)
                        Text("Wi-Fi ${NetworkUtils.formatBytes(wifi)} · Mobile ${NetworkUtils.formatBytes(mobile)}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("${current.size} UID records" + if (lastUpdated > 0) " · Updated " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(lastUpdated)) else "",
                        color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Today", 7 to "7 days", 30 to "30 days").forEach { (value, label) ->
                    FilterChip(selected = days == value, onClick = { days = value }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Mobile", "Wi-Fi").forEach { value ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                }
            }
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(16.dp), placeholder = { Text("Search applications") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, "Clear search") } })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Top consumers", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Box {
                    TextButton(onClick = { showSort = true }) { Text("$sort ↓") }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        listOf("Total", "Download", "Upload").forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { sort = value; showSort = false })
                        }
                    }
                }
            }
        }
        if (loading && current.isEmpty()) items(5) { AppLoadingRow() }
        else if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { message -> item { UsageFeedback("Unable to refresh", message, "Retry") { refresh++ } } }
        if (!loading && error == null && visible.isEmpty()) item { UsageFeedback("No matching apps", if (search.isNotBlank()) "Try a different application name." else "No reported traffic for this selection.", if (search.isNotBlank()) "Clear search" else "Refresh") { if (search.isNotBlank()) search = "" else refresh++ } }
        items(visible, key = { it.uid }) { app ->
            Surface(onClick = { selected = app }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, 44)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(app.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(NetworkUtils.formatBytes(amount(app)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("Mobile ${NetworkUtils.formatBytes(app.mobile)} · Wi-Fi ${NetworkUtils.formatBytes(app.wifi)}", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(progress = { if (rankingTotal > 0) (amount(app).toDouble() / rankingTotal).toFloat().coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("UID totals can differ from device totals due to attribution, VPN, tethering and query timing. Some identities are restricted by Android.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun AppIcon(app: AppUsage, size: Int) {
    if (app.icon != null) Image(app.icon, contentDescription = null, modifier = Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)))
    else Box(Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.Apps, null) }
}

@Composable
internal fun UsageSplitBar(wifi: Long, mobile: Long) {
    val colors = LocalNetPulseColors.current
    val total = wifi.toDouble() + mobile.toDouble()
    Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (total > 0) {
            if (wifi > 0) Box(Modifier.weight((wifi / total).toFloat()).fillMaxHeight().background(colors.download))
            if (mobile > 0) Box(Modifier.weight((mobile / total).toFloat()).fillMaxHeight().background(colors.upload))
        }
    }
}

@Composable
private fun AppDetailsPage(app: AppUsage, days: Int, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to apps") }
                Text("Application details", style = MaterialTheme.typography.titleLarge)
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIcon(app, 64)
                    Text(app.name, style = MaterialTheme.typography.headlineSmall)
                    Text(if (days == 1) "Today" else "Last $days days", style = MaterialTheme.typography.labelMedium)
                    Text(NetworkUtils.formatBytes(app.total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    UsageSplitBar(app.wifi, app.mobile)
                }
            }
        }
        items(listOf(Triple("Wi-Fi", app.wifiRx, app.wifiTx), Triple("Mobile", app.mobileRx, app.mobileTx))) { (label, rx, tx) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(NetworkUtils.formatBytes(rx + tx), style = MaterialTheme.typography.headlineSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Download"); Text(NetworkUtils.formatBytes(rx), fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.End) { Text("Upload"); Text(NetworkUtils.formatBytes(tx), fontWeight = FontWeight.Bold) }
                    }
                    LinearProgressIndicator(progress = { if (rx + tx > 0) (rx.toDouble() / (rx + tx)).toFloat() else 0f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Text("Reported Android UID ${app.uid}. Shared or unidentified traffic cannot always be attributed to a single package.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun TopAppsTodayCard(isVisible: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppUsage>?>(null) }
    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        val date = java.time.LocalDate.now().toString()
        val cached = appCache.get(1)?.takeIf { it.date == date }
        apps = cached?.records
        if (cached == null || SystemClock.elapsedRealtime() - cached.loadedAt >= 60_000) {
            try {
                val result = loadApps(context, 1)
                appCache.put(1, CachedApps(result, SystemClock.elapsedRealtime(), date, System.currentTimeMillis()))
                apps = result
            } catch (e: CancellationException) { throw e } catch (_: Exception) { apps = cached?.records ?: emptyList() }
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Top apps today", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpen) { Text("View all") }
            }
            if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else if (apps!!.isEmpty()) Text("No app usage available.", style = MaterialTheme.typography.bodySmall)
            apps.orEmpty().take(3).forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIcon(app, 34)
                    Text(app.name, modifier = Modifier.weight(1f), maxLines = 2)
                    Text(NetworkUtils.formatBytes(app.total), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AppLoadingRow() {
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(placeholder))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(.55f).height(14.dp).clip(CircleShape).background(placeholder))
                Box(Modifier.fillMaxWidth(.85f).height(9.dp).clip(CircleShape).background(placeholder))
            }
        }
    }
}

@Composable
private fun UsageFeedback(title: String, description: String, action: String, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Apps, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
