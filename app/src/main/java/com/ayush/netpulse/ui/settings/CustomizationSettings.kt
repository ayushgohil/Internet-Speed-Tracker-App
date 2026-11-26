package com.ayush.netpulse.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
internal fun CustomizationSettings() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("netpulse_overlay", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var granted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var networkCard by remember { mutableStateOf(prefs.getBoolean("network_card", true)) }
    var insightsCard by remember { mutableStateOf(prefs.getBoolean("insights_card", true)) }
    var quickActions by remember { mutableStateOf(prefs.getBoolean("quick_actions", true)) }
    var fontSize by remember { mutableStateOf(prefs.getInt("font_size", 12).coerceIn(10, 22).toFloat()) }
    var opacity by remember { mutableStateOf(prefs.getInt("opacity", 90).coerceIn(35, 100).toFloat()) }
    var downloadOnly by remember { mutableStateOf(prefs.getBoolean("download_only", false)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = Settings.canDrawOverlays(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Floating speed widget", style = MaterialTheme.typography.titleMedium)
            Text("Requires display over other apps permission and active background monitoring. Hidden while locked.",
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Show floating speeds")
                Switch(checked = enabled && granted, onCheckedChange = { checked ->
                    enabled = checked
                    prefs.edit().putBoolean("enabled", checked).apply()
                    if (checked && !granted) {
                        try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"))) } catch (_: Exception) { }
                    }
                })
            }
            Text(if (granted) "Permission granted" else "Permission not granted", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { prefs.edit().remove("x").remove("y").putInt("position_revision", prefs.getInt("position_revision", 0) + 1).apply() }) { Text("Reset widget position") }
            Text("Text size: ${fontSize.toInt()} sp")
            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 10f..22f,
                steps = 11, onValueChangeFinished = { prefs.edit().putInt("font_size", fontSize.toInt()).apply() })
            Text("Background opacity: ${opacity.toInt()}%")
            Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 35f..100f,
                onValueChangeFinished = { prefs.edit().putInt("opacity", opacity.toInt()).apply() })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Download only")
                Switch(downloadOnly, { downloadOnly = it; prefs.edit().putBoolean("download_only", it).apply() })
            }
            HorizontalDivider()
            Text("Dashboard cards", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Network details")
                Switch(networkCard, { networkCard = it; prefs.edit().putBoolean("network_card", it).apply() })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Usage insights")
                Switch(insightsCard, { insightsCard = it; prefs.edit().putBoolean("insights_card", it).apply() })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Quick actions")
                Switch(quickActions, { quickActions = it; prefs.edit().putBoolean("quick_actions", it).apply() })
            }
        }
    }
}
