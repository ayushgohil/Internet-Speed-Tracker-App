package com.ayush.netpulse.ui.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AccessPoint(val name: String, val frequency: Int, val signal: Int)
private fun channel(f: Int): Int? = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5005..5895 -> (f - 5000) / 5
    f == 5935 -> 2
    f in 5955..7115 -> (f - 5950) / 5
    else -> null
}
private fun band(f: Int) = when {
    f in 2400..2500 -> "2.4 GHz"
    f in 4900..5924 -> "5 GHz"
    f in 5925..7125 -> "6 GHz"
    else -> "Other"
}

@Suppress("DEPRECATION")
@Composable
internal fun WifiAnalyzerCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf(emptyList<AccessPoint>()) }
    var status by remember { mutableStateOf("Scan nearby access points on demand.") }
    var scanning by remember { mutableStateOf(false) }
    fun permitted() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun scan() {
        if (scanning || !permitted()) return
        scanning = true
        scope.launch {
            try {
                val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val accepted = withContext(Dispatchers.IO) { manager.startScan() }
                if (accepted) delay(3000)
                points = withContext(Dispatchers.IO) {
                    manager.scanResults.map {
                        AccessPoint(it.SSID.ifBlank { "Hidden network" }, it.frequency, it.level)
                    }.sortedByDescending { it.signal }.take(100)
                }
                status = if (accepted) "Showing latest available results; scan may still be in progress."
                    else "Scan unavailable or throttled. Showing cached results."
                if (points.isEmpty()) status += " No networks returned. Check Wi-Fi and location settings."
            } catch (_: Exception) { status = "Scan unavailable. Check permission, Wi-Fi and location settings." }
            finally { scanning = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permitted()) scan() else status = "Precise location permission is required by Android for Wi-Fi scans."
    }
    val chartColor = MaterialTheme.colorScheme.primary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Wi-Fi analyzer", style = MaterialTheme.typography.titleMedium)
            Text("Nearby networks and channel occupancy. Android may throttle scans; results remain on this device.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                if (permitted()) scan() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, enabled = !scanning) { Text(if (scanning) "Scanning…" else "Scan Wi-Fi") }
            Text(status, style = MaterialTheme.typography.bodySmall)
            points.groupBy { band(it.frequency) }.toSortedMap().forEach { (name, entries) ->
                val channels = entries.mapNotNull { channel(it.frequency) }.groupingBy { it }.eachCount().toSortedMap()
                Text("$name · ${entries.size} access points", style = MaterialTheme.typography.titleSmall)
                if (channels.isNotEmpty()) {
                    val max = channels.values.maxOrNull() ?: 1
                    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                        val slot = size.width / channels.size
                        channels.values.forEachIndexed { index, count ->
                            val height = size.height * count / max
                            drawRect(chartColor, Offset(index * slot + slot * .15f, size.height - height), Size(slot * .7f, height))
                        }
                    }
                    Text(channels.entries.joinToString(" · ") { "Ch ${it.key}: ${it.value}" },
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            points.take(15).forEach { ap ->
                HorizontalDivider()
                Text(ap.name, style = MaterialTheme.typography.bodyMedium)
                Text("${ap.signal} dBm · ${ap.frequency} MHz · Ch ${channel(ap.frequency) ?: "?"}",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (points.size > 15) Text("Showing 15 strongest networks; chart includes up to 100.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun CellularDetailsCard() {
    val context = LocalContext.current
    var details by remember { mutableStateOf("Tap refresh to inspect active cellular subscriptions.") }
    fun read() {
        details = try {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptions = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            val active = subscriptions.activeSubscriptionInfoList.orEmpty().sortedBy { it.simSlotIndex }
            val defaultId = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
            fun describe(phone: TelephonyManager): String {
                val type = phone.dataNetworkType
                val tech = when (type) {
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                    TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                    TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
                    else -> "Other ($type)"
                }
                val signal = if (android.os.Build.VERSION.SDK_INT >= 28) phone.signalStrength?.level else null
                return "Operator: ${phone.networkOperatorName.ifBlank { "Unavailable" }}\nRadio: $tech\nSignal level: ${signal?.let { "$it / 4" } ?: "Unavailable"}"
            }
            if (active.isEmpty()) "No active subscriptions visible.\n" + describe(manager)
            else active.joinToString("\n\n") { sub ->
                val label = "SIM ${sub.simSlotIndex + 1}" + if (sub.subscriptionId == defaultId) " · Default data" else ""
                val info = try { describe(manager.createForSubscriptionId(sub.subscriptionId)) }
                    catch (_: Exception) { "Radio details unavailable" }
                "$label\nCarrier: ${sub.carrierName}\n$info"
            }
        } catch (_: Exception) { "Cellular information unavailable or restricted." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) read() else details = "Phone permission denied. Cellular details are optional."
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cellular information", style = MaterialTheme.typography.titleMedium)
            Text(details)
            Text("Android may hide subscriptions or radio details. Non-standalone 5G can report LTE. Usage totals are not split by SIM.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) read()
                else permission.launch(Manifest.permission.READ_PHONE_STATE)
            }) { Text("Refresh cellular details") }
        }
    }
}
