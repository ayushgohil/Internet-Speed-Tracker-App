package com.ayush.netpulse.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayush.netpulse.util.ScreenUsageTracker
import com.ayush.netpulse.util.NetworkUtils
import java.time.LocalDate

@Composable
internal fun UsageInsightsCard(uiState: DashboardUiState) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { ScreenUsageTracker.load(context) }
    val observed by ScreenUsageTracker.totals.collectAsState()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val previous = uiState.recentDailyRecords.firstOrNull { it.date == yesterday }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usage insights", style = MaterialTheme.typography.titleMedium)
            if (uiState.hasUsageAccessPermission && previous != null) {
                val today = uiState.dailyUsage.totalAllBytes
                val difference = today - previous.totalAllBytes
                Text("Today so far: " + NetworkUtils.formatBytes(today))
                Text("Yesterday (full day): " + NetworkUtils.formatBytes(previous.totalAllBytes))
                Text(if (difference > 0) "Today has exceeded yesterday by " + NetworkUtils.formatBytes(difference)
                    else "Today has not yet exceeded yesterday's total.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Daily comparisons require usage access and recorded history.", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            Text("Screen-state traffic · today", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Screen on")
                Text(NetworkUtils.formatBytes(observed.on))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Screen off")
                Text(NetworkUtils.formatBytes(observed.off))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Unclassified")
                Text(NetworkUtils.formatBytes(observed.unknown))
            }
            Text("Estimated from device byte counters during active background monitoring. Screen transitions and long gaps are unclassified; stopped periods are not measured.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
