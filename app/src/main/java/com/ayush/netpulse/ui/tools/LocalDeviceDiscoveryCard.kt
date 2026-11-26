package com.ayush.netpulse.ui.tools

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** User-initiated DNS-SD browsing, not an exhaustive IP or port scanner. */
@Composable
internal fun LocalDeviceDiscoveryCard(isVisible: Boolean) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.NSD_SERVICE) as? NsdManager }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Discover advertised services on your local Wi-Fi.") }
    val services = remember { mutableStateMapOf<String, String>() }
    val types = listOf("_ipp._tcp.", "_airplay._tcp.", "_googlecast._tcp.", "_smb._tcp.", "_http._tcp.")
    var generation by remember { mutableStateOf(0) }
    LaunchedEffect(isVisible, scanning, generation) {
        if (!isVisible && scanning) scanning = false
        if (isVisible && scanning) {
            delay(12_000)
            scanning = false
            status = "Discovery finished. Only advertised services are shown."
        }
    }
    DisposableEffect(scanning, isVisible, generation) {
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()
        val token = generation
        var alive = true
        if (scanning && isVisible && manager != null) {
            types.forEach { type ->
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) {}
                    override fun onDiscoveryStopped(serviceType: String) {}
                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        handler.post { if (alive && token == generation) status = "Some service types are unavailable (Android error $errorCode)." }
                    }
                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                    override fun onServiceFound(info: NsdServiceInfo) {
                        handler.post {
                            if (alive && token == generation && services.size < 100) {
                                services["${info.serviceName}|$type"] = "${info.serviceName} · $type"
                            }
                        }
                    }
                    override fun onServiceLost(info: NsdServiceInfo) {
                        handler.post { if (alive && token == generation) services.remove("${info.serviceName}|$type") }
                    }
                }
                try {
                    manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                    listeners.add(listener)
                } catch (_: RuntimeException) { status = "Some service types are restricted or unavailable." }
            }
        }
        onDispose {
            alive = false
            listeners.forEach { try { manager?.stopServiceDiscovery(it) } catch (_: RuntimeException) {} }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local device discovery", style = MaterialTheme.typography.titleMedium)
            Text("Browse printers, media receivers, shared services and web services advertising themselves using DNS-SD. Use only on networks you own or have permission to inspect.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = manager != null, onClick = {
                if (scanning) { scanning = false; status = "Discovery stopped." }
                else {
                    services.clear()
                    generation++
                    status = "Discovering local services…"
                    scanning = true
                }
            }) { Text(if (scanning) "Stop discovery" else "Discover services") }
            Text(status, style = MaterialTheme.typography.bodySmall)
            Text("${services.size} advertised services (not unique devices)", style = MaterialTheme.typography.labelMedium)
            services.values.sorted().take(30).forEach { value ->
                HorizontalDivider()
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            Text("Silent devices and services outside these types will not appear. Nothing is uploaded or saved.",
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
