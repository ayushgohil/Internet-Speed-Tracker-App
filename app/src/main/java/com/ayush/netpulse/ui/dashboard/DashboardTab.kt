package com.ayush.netpulse.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.ui.theme.LocalNetPulseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    uiState: DashboardUiState,
    onToggleMonitoring: () -> Unit = {},
    onToggleBackgroundService: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestUsageAccess: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val npColors = LocalNetPulseColors.current
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. UNIFIED LIVE NETWORK ACTIVITY HERO
        item(key = "live_speed_hero") {
            LiveSpeedHeroCard(
                downloadFormatted = uiState.liveSpeed.formattedDownload,
                uploadFormatted = uiState.liveSpeed.formattedUpload,
                networkType = uiState.networkType,
                isMonitoringRunning = uiState.isBackgroundServiceRunning,
                onToggleMonitoring = onToggleBackgroundService
            )
        }

        // 2. OVERNIGHT RESILIENCE ADVISORY (Shown when battery is still restricted / optimized)
        if (!uiState.isBatteryOptimizationIgnored) {
            item(key = "battery_optimization_advisory") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onRequestBatteryOptimization() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, npColors.warning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(npColors.warning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalWifiOff,
                                contentDescription = null,
                                tint = npColors.warning,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Active Overnight",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Allow Unrestricted Battery so Android doesn't kill the notification when offline overnight.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Text(
                            text = "Allow",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 3. COMPACT TODAY'S DATA CONSUMPTION CARD
        item(key = "today_consumption") {
            TodayConsumptionCard(
                hasPermission = uiState.hasUsageAccessPermission,
                mobileBytes = uiState.dailyUsage.mobileRxBytes + uiState.dailyUsage.mobileTxBytes,
                wifiBytes = uiState.dailyUsage.wifiRxBytes + uiState.dailyUsage.wifiTxBytes,
                mobileFormatted = uiState.dailyUsage.formattedMobile,
                wifiFormatted = uiState.dailyUsage.formattedWifi,
                totalFormatted = uiState.dailyUsage.formattedTotal,
                onRequestPermission = onRequestUsageAccess
            )
        }
    }

    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    )
}
}

/**
 * Coherent Live Speed Hero combining:
 * - Current Connection status
 * - Discreet monitoring control
 * - Prominent download speed with complementary upload speed
 * - Tabular numerical stability
 */
@Composable
private fun LiveSpeedHeroCard(
    downloadFormatted: String,
    uploadFormatted: String,
    networkType: NetworkType,
    isMonitoringRunning: Boolean,
    onToggleMonitoring: () -> Unit
) {
    val npColors = LocalNetPulseColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Network Connection Badge on Left, Small Discoverable Monitoring Toggle on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Chip
                ConnectionStatusBadge(networkType = networkType)

                // Compact Monitoring Switch & Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleMonitoring() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val dotColor by animateColorAsState(
                        targetValue = if (isMonitoringRunning) npColors.download else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "monitoringDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = if (isMonitoringRunning) "Tracking" else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMonitoringRunning) npColors.download else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isMonitoringRunning,
                        onCheckedChange = { onToggleMonitoring() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Telemetry Area: Prominent Download Speed as Primary Visual Anchor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Column: Download (Primary)
                Column(modifier = Modifier.weight(1.3f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(npColors.download.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download",
                                tint = npColors.download,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = downloadFormatted,
                        style = MaterialTheme.typography.headlineLarge,
                        color = npColors.download,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                // Right Column: Upload (Clearly visible, complementary secondary)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Upload",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(npColors.upload.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload",
                                tint = npColors.upload,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = uploadFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = npColors.upload,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subdued metadata footer
            Text(
                text = if (isMonitoringRunning) "Real-time telemetry • ~1s sampling" else "Monitoring paused by user",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Compact Connection Status Badge showing connection icon and transport name
 */
@Composable
private fun ConnectionStatusBadge(networkType: NetworkType) {
    val npColors = LocalNetPulseColors.current

    val (icon, tint, label) = when (networkType) {
        NetworkType.WIFI -> Triple(Icons.Default.SignalWifi4Bar, npColors.download, "Wi-Fi")
        NetworkType.CELLULAR -> Triple(Icons.Default.CellTower, npColors.upload, "Mobile Data")
        NetworkType.ETHERNET -> Triple(Icons.Default.SignalWifi4Bar, npColors.download, "Ethernet")
        NetworkType.DISCONNECTED -> Triple(Icons.Default.SignalWifiOff, MaterialTheme.colorScheme.onSurfaceVariant, "Offline")
        NetworkType.UNKNOWN -> Triple(Icons.Default.SignalWifi4Bar, MaterialTheme.colorScheme.onSurfaceVariant, "Connecting...")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Single coherent Today's Data Consumption section:
 * - Combined usage today as headline
 * - Proportional usage bar (Mobile vs Wi-Fi)
 * - Clean mobile & Wi-Fi row
 */
@Composable
private fun TodayConsumptionCard(
    hasPermission: Boolean,
    mobileBytes: Long,
    wifiBytes: Long,
    mobileFormatted: String,
    wifiFormatted: String,
    totalFormatted: String,
    onRequestPermission: () -> Unit
) {
    val npColors = LocalNetPulseColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: "Today's Usage" + Combined Total Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Consumption",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Since 12:00 AM local time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasPermission) {
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = npColors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Usage access required to display daily data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Grant", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                // Proportional Mobile vs Wi-Fi bar
                val totalBytes = (mobileBytes + wifiBytes).coerceAtLeast(1L)
                val mobileFraction = (mobileBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                val wifiFraction = (wifiBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (mobileBytes > 0L || wifiBytes > 0L) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (mobileFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(mobileFraction.coerceAtLeast(0.01f))
                                        .fillMaxSize()
                                        .background(npColors.upload)
                                )
                            }
                            if (wifiFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(wifiFraction.coerceAtLeast(0.01f))
                                        .fillMaxSize()
                                        .background(npColors.download)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sub-metrics Row: Mobile vs Wi-Fi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mobile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(npColors.upload)
                        )
                        Column {
                            Text(
                                text = "Mobile Data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = mobileFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Wi-Fi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(npColors.download)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Wi-Fi Data",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = wifiFormatted,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
