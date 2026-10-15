package com.ayush.netpulse.ui.usage

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.theme.Amber500
import com.ayush.netpulse.ui.theme.Blue400
import com.ayush.netpulse.ui.theme.Cyan400
import com.ayush.netpulse.ui.theme.DarkBackground
import com.ayush.netpulse.ui.theme.DarkCardBorder
import com.ayush.netpulse.ui.theme.DarkSurface
import com.ayush.netpulse.ui.theme.Emerald400
import com.ayush.netpulse.ui.theme.Slate400
import com.ayush.netpulse.ui.theme.Slate600
import com.ayush.netpulse.ui.theme.Slate800
import com.ayush.netpulse.ui.theme.Slate900

@Composable
fun UsageTab(
    uiState: DashboardUiState,
    onRequestUsageAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Usage",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android NetworkStatsManager Analytics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }

                if (uiState.isLoadingUsage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Cyan400
                    )
                } else {
                    IconButton(
                        onClick = onRefreshUsage,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Usage",
                            tint = Cyan400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Usage Permission Banner if not granted
        if (!uiState.hasUsageAccessPermission) {
            item {
                UsagePermissionCard(onRequestUsageAccess = onRequestUsageAccess)
            }
        } else {
            // Today's Usage Breakdown Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UsageMetricCard(
                        title = "Today's Mobile",
                        value = uiState.dailyUsage.formattedMobile,
                        icon = Icons.Default.CellTower,
                        accentColor = Blue400,
                        modifier = Modifier.weight(1f)
                    )
                    UsageMetricCard(
                        title = "Today's Wi-Fi",
                        value = uiState.dailyUsage.formattedWifi,
                        icon = Icons.Default.SignalWifi4Bar,
                        accentColor = Cyan400,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Total Combined Today
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate800, RoundedCornerShape(14.dp)),
                    color = DarkSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Combined Consumption",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Total data consumed today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                        Text(
                            text = uiState.dailyUsage.formattedTotal,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Cyan400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Historical Daily Usage Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Daily History (Room)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (uiState.recentDailyRecords.isEmpty()) {
            item {
                EmptyUsageHistoryCard()
            }
        } else {
            items(uiState.recentDailyRecords) { dailyRecord ->
                DailyUsageRow(dailyUsage = dailyRecord)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UsagePermissionCard(onRequestUsageAccess: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Amber500.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        color = Slate900,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Amber500,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Usage Access Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android requires Usage Access to query system mobile and Wi-Fi data statistics.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(12.dp))
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
        color = DarkSurface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyUsageRow(dailyUsage: DailyUsageEntity) {
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dailyUsage.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: ${dailyUsage.formattedTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cyan400
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Mobile: ${dailyUsage.formattedMobile}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Blue400,
                    fontSize = 12.sp
                )
                Text(
                    text = "Wi-Fi: ${dailyUsage.formattedWifi}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Emerald400,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyUsageHistoryCard() {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DataUsage,
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No daily history recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
        }
    }
}
