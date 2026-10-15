package com.ayush.netpulse.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.data.local.SpeedRecordEntity
import com.ayush.netpulse.data.model.NetworkType
import com.ayush.netpulse.ui.theme.Amber500
import com.ayush.netpulse.ui.theme.Blue400
import com.ayush.netpulse.ui.theme.Cyan400
import com.ayush.netpulse.ui.theme.DarkBackground
import com.ayush.netpulse.ui.theme.DarkCardBorder
import com.ayush.netpulse.ui.theme.DarkSurface
import com.ayush.netpulse.ui.theme.Emerald400
import com.ayush.netpulse.ui.theme.Emerald500
import com.ayush.netpulse.ui.theme.Rose500
import com.ayush.netpulse.ui.theme.Slate400
import com.ayush.netpulse.ui.theme.Slate600
import com.ayush.netpulse.ui.theme.Slate700
import com.ayush.netpulse.ui.theme.Slate800
import com.ayush.netpulse.ui.theme.Slate900
import com.ayush.netpulse.ui.theme.Violet500
import com.ayush.netpulse.util.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onToggleMonitoring: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                HeaderSection(
                    isMonitoring = uiState.isMonitoringActive,
                    onToggle = onToggleMonitoring
                )
            }

            // Network Connection Status Badge
            item {
                NetworkStatusCard(networkType = uiState.networkType)
            }

            // Real-Time Speed Hero Card
            item {
                LiveSpeedHeroCard(
                    uiState = uiState
                )
            }

            // Data Usage Card (NetworkStatsManager)
            item {
                DataUsageSection(
                    uiState = uiState,
                    onRequestUsageAccess = onRequestUsageAccess,
                    onRefreshUsage = onRefreshUsage
                )
            }

            // Recent Room History Header
            item {
                Text(
                    text = "Recent Speed Log (Room)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Empty state or Room records
            if (uiState.recentRecords.isEmpty()) {
                item {
                    EmptyHistoryCard(isMonitoring = uiState.isMonitoringActive)
                }
            } else {
                items(uiState.recentRecords) { record ->
                    SpeedRecordRow(record = record)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    isMonitoring: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NetPulse",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                val pulseColor by animateColorAsState(
                    targetValue = if (isMonitoring) Emerald400 else Slate600,
                    label = "pulseColor"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(pulseColor)
                )
            }
            Text(
                text = if (isMonitoring) "Live Monitor Active" else "Monitoring Paused",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMonitoring) Emerald400 else Slate400
            )
        }

        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isMonitoring) Slate800 else Cyan400)
        ) {
            Icon(
                imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isMonitoring) "Pause" else "Resume",
                tint = if (isMonitoring) Color.White else DarkBackground
            )
        }
    }
}

@Composable
private fun NetworkStatusCard(networkType: NetworkType) {
    val (icon, tint, label) = when (networkType) {
        NetworkType.WIFI -> Triple(Icons.Default.SignalWifi4Bar, Cyan400, "Connected via Wi-Fi")
        NetworkType.CELLULAR -> Triple(Icons.Default.CellTower, Blue400, "Connected via Mobile Data")
        NetworkType.ETHERNET -> Triple(Icons.Default.Speed, Emerald400, "Connected via Ethernet")
        NetworkType.DISCONNECTED -> Triple(Icons.Default.SignalWifiOff, Rose500, "Disconnected from Internet")
        NetworkType.UNKNOWN -> Triple(Icons.Default.Speed, Amber500, "Network Active")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = networkType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
        }
    }
}

@Composable
private fun LiveSpeedHeroCard(
    uiState: DashboardUiState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Cyan400.copy(alpha = 0.35f), DarkCardBorder)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME SPEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan400,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.isMonitoringActive) "TrafficStats active" else "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Columns: Download and Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download Speed
                SpeedColumn(
                    title = "DOWNLOAD",
                    speedFormatted = if (uiState.isMonitoringActive) uiState.liveSpeed.formattedDownload else "0.0 KB/s",
                    icon = Icons.Default.ArrowDownward,
                    color = Emerald400,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Upload Speed
                SpeedColumn(
                    title = "UPLOAD",
                    speedFormatted = if (uiState.isMonitoringActive) uiState.liveSpeed.formattedUpload else "0.0 KB/s",
                    icon = Icons.Default.ArrowUpward,
                    color = Blue400,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpeedColumn(
    title: String,
    speedFormatted: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, Slate800, RoundedCornerShape(16.dp)),
        color = Slate900.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = speedFormatted,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DataUsageSection(
    uiState: DashboardUiState,
    onRequestUsageAccess: () -> Unit,
    onRefreshUsage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Data Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "NetworkStatsManager Summary",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }

                if (uiState.isLoadingUsage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Cyan400
                    )
                } else {
                    IconButton(
                        onClick = onRefreshUsage,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Usage",
                            tint = Cyan400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!uiState.hasUsageAccessPermission) {
                // Permission Request Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Slate800,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Usage Access Permission Needed",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android requires Usage Access to query system mobile and Wi-Fi data statistics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestUsageAccess,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Cyan400,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Permission in Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Usage Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UsageMetricCard(
                        title = "Mobile Data",
                        value = uiState.dailyUsage.formattedMobile,
                        icon = Icons.Default.CellTower,
                        accentColor = Blue400,
                        modifier = Modifier.weight(1f)
                    )
                    UsageMetricCard(
                        title = "Wi-Fi Data",
                        value = uiState.dailyUsage.formattedWifi,
                        icon = Icons.Default.SignalWifi4Bar,
                        accentColor = Cyan400,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp)),
                    color = Slate900.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Combined Today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                        Text(
                            text = uiState.dailyUsage.formattedTotal,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, Slate800, RoundedCornerShape(14.dp)),
        color = Slate900.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard(isMonitoring: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isMonitoring) "Sampling network activity into Room..." else "Monitoring paused.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
        }
    }
}

@Composable
private fun SpeedRecordRow(record: SpeedRecordEntity) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(record.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate800, RoundedCornerShape(12.dp)),
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = record.networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = NetworkUtils.formatSpeed(record.downloadBps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Blue400,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = NetworkUtils.formatSpeed(record.uploadBps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
