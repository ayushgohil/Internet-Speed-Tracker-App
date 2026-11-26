package com.ayush.netpulse.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.data.model.NetworkType
import kotlinx.coroutines.delay

@Suppress("DEPRECATION")
@Composable
internal fun NetworkDetailsCard(networkType: NetworkType) {
    val context = LocalContext.current
    var details by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    LaunchedEffect(networkType) {
        while (true) {
            details = try {
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = manager.activeNetwork
                val caps = network?.let { manager.getNetworkCapabilities(it) }
                val links = network?.let { manager.getLinkProperties(it) }
                buildList {
                    add("Internet" to when {
                        caps == null -> "Disconnected"
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) -> "Sign-in required"
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "Validated"
                        else -> "Not validated"
                    })
                    if (caps != null) {
                        add("VPN" to if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) "Active" else "Not active on default network")
                        val info = if (Build.VERSION.SDK_INT >= 29) caps.transportInfo as? WifiInfo else null
                        if (info != null) {
                            if (info.rssi in -126..0) add("Wi-Fi signal" to "${info.rssi} dBm")
                            if (info.frequency > 0) add("Frequency" to "${info.frequency} MHz")
                            if (info.linkSpeed > 0) add("Link speed" to "${info.linkSpeed} Mbps")
                        }
                    }
                    links?.linkAddresses?.firstOrNull { it.address is java.net.Inet4Address }?.let {
                        add("Local IPv4" to (it.address.hostAddress ?: "Unavailable"))
                    }
                }
            } catch (_: SecurityException) { listOf("Details" to "Restricted by Android") }
            delay(5_000L)
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Network information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            details.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End)
                }
            }
            Text("Link speed is not an internet speed test. Details depend on Android permissions.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
