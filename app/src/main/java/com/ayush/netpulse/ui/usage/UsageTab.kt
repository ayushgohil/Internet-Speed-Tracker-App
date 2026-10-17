package com.ayush.netpulse.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.data.local.DailyUsageEntity
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import com.ayush.netpulse.util.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class UsageFilterType(val label: String) {
    TOTAL("Total"),
    MOBILE("Mobile"),
    WIFI("Wi-Fi")
}

data class DayHeatmapCell(
    val dateStr: String,
    val displayDate: String,
    val dayOfWeek: Int, // 1 (Sun) .. 7 (Sat)
    val monthName: String,
    val isToday: Boolean,
    val entity: DailyUsageEntity?
)

@Composable
fun UsageTab(
    uiState: DashboardUiState,
    onRequestUsageAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {
    UsageTab(
        recentDailyRecords = uiState.recentDailyRecords,
        hasUsageAccessPermission = uiState.hasUsageAccessPermission,
        isLoadingUsage = uiState.isLoadingUsage,
        isReconcilingUsage = uiState.isReconcilingUsage,
        onRequestUsageAccess = onRequestUsageAccess,
        onRefreshUsage = onRefreshUsage,
        modifier = modifier,
        lazyListState = lazyListState
    )
}

@Composable
fun UsageTab(
    recentDailyRecords: List<DailyUsageEntity>,
    hasUsageAccessPermission: Boolean,
    isLoadingUsage: Boolean,
    isReconcilingUsage: Boolean,
    onRequestUsageAccess: () -> Unit,
    onRefreshUsage: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {
    var selectedFilter by remember { mutableStateOf(UsageFilterType.TOTAL) }
    var selectedDayDetail by remember { mutableStateOf<DayHeatmapCell?>(null) }

    // Precompute 90-day sequence only when DB records change
    val daysList = remember(recentDailyRecords) {
        generate90DaysGrid(recentDailyRecords)
    }

    // Precompute reversed list once to avoid allocating 90 items per recomposition
    val reversedDaysList = remember(daysList) {
        daysList.reversed()
    }

    // Precompute weeks layout once
    val weeks = remember(daysList) {
        chunkIntoWeeks(daysList)
    }

    // Precompute dataset-aware quantile thresholds for dynamic heatmap
    val thresholds = remember(daysList, selectedFilter) {
        val values = daysList.mapNotNull { cell ->
            cell.entity?.let { e ->
                when (selectedFilter) {
                    UsageFilterType.MOBILE -> e.totalMobileBytes
                    UsageFilterType.WIFI -> e.totalWifiBytes
                    UsageFilterType.TOTAL -> e.totalAllBytes
                }
            }
        }
        DynamicHeatmapClassifier.computeThresholds(values)
    }

    // Precompute intensity level for each cell to eliminate per-item calculations
    val cellLevelMap = remember(daysList, selectedFilter, thresholds) {
        daysList.associate { cell ->
            val bytes = when (selectedFilter) {
                UsageFilterType.MOBILE -> cell.entity?.totalMobileBytes ?: 0L
                UsageFilterType.WIFI -> cell.entity?.totalWifiBytes ?: 0L
                UsageFilterType.TOTAL -> cell.entity?.totalAllBytes ?: 0L
            }
            cell.dateStr to DynamicHeatmapClassifier.classify(
                hasEntity = cell.entity != null,
                bytes = bytes,
                thresholds = thresholds
            )
        }
    }

    if (!hasUsageAccessPermission) {
        UsagePermissionRequiredCard(
            onRequestUsageAccess = onRequestUsageAccess,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item(key = "header") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Usage History",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last 90 days • Android NetworkStatsManager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLoadingUsage || isReconcilingUsage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(
                        onClick = onRefreshUsage,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Filter Chips: Mobile | Wi-Fi | Total
        item(key = "filter_chips") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageFilterType.values().forEach { filterType ->
                    val isSelected = selectedFilter == filterType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filterType },
                        label = {
                            Text(
                                text = filterType.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // 3. Dynamic Activity Heatmap Card
        item(key = "heatmap_card") {
            CalendarHeatmapCard(
                weeks = weeks,
                cellLevelMap = cellLevelMap,
                thresholds = thresholds,
                onSelectCell = { cell -> selectedDayDetail = cell }
            )
        }

        // 4. Detailed Daily Usage Table (Last 90 days) Title
        item(key = "table_title") {
            Text(
                text = "Daily Usage (Last 90 days)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
        }

        // Table Header
        item(key = "table_header") {
            DailyTableHeaderRow()
        }

        // Stable-Keyed Table Rows: Prevents full list recomposition and ensures smooth 60/120fps scrolling
        items(
            items = reversedDaysList,
            key = { it.dateStr }
        ) { cell ->
            DailyTableRow(
                cell = cell,
                onClick = { selectedDayDetail = cell }
            )
        }

        item(key = "footer_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Detail Breakdown Dialog
    selectedDayDetail?.let { cell ->
        UsageDetailDialog(
            cell = cell,
            onDismiss = { selectedDayDetail = null }
        )
    }
}

@Composable
private fun CalendarHeatmapCard(
    weeks: List<List<DayHeatmapCell?>>,
    cellLevelMap: Map<String, HeatmapIntensityLevel>,
    thresholds: HeatmapThresholds?,
    onSelectCell: (DayHeatmapCell) -> Unit
) {
    val npColors = LocalNetPulseColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Heatmap",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Quantile-Aware • 90 Days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontally scrollable weeks grid
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                // Weekday labels column (Mon, Wed, Fri)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 6.dp, top = 20.dp)
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                        Text(
                            text = if (idx % 2 == 0) day else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            modifier = Modifier.size(13.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Month label header if week is the start of a month
                            val firstValidDay = week.firstOrNull { it != null }
                            val monthLabel = if (firstValidDay?.dateStr?.endsWith("-01") == true ||
                                (firstValidDay != null && week.indexOf(firstValidDay) == 0 && firstValidDay.dateStr.endsWith("-02"))
                            ) {
                                firstValidDay.monthName
                            } else {
                                ""
                            }

                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .height(14.dp)
                                    .width(13.dp),
                                maxLines = 1
                            )

                            // 7 days of the week
                            for (dayIndex in 0 until 7) {
                                val cell = week.getOrNull(dayIndex)
                                if (cell != null) {
                                    val level = cellLevelMap[cell.dateStr] ?: HeatmapIntensityLevel.MISSING
                                    val color = DynamicHeatmapClassifier.getColor(level, npColors)

                                    Box(
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(color)
                                            .border(
                                                width = if (cell.isToday) 1.5.dp else 0.5.dp,
                                                color = if (cell.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .clickable { onSelectCell(cell) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coherent Dynamic Heatmap Legend with thresholds
            HeatmapLegend(thresholds = thresholds, npColors = npColors)
        }
    }
}

@Composable
private fun HeatmapLegend(
    thresholds: HeatmapThresholds?,
    npColors: com.ayush.netpulse.ui.theme.NetPulseExtendedColors
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LegendCell(color = npColors.heatmapMissing)
                Text(
                    text = "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                LegendCell(color = npColors.heatmapZero)
                Text(
                    text = "0 B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (thresholds != null) "≤${NetworkUtils.formatBytes(thresholds.minPositive)}" else "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                LegendCell(color = npColors.heatmapLevel1)
                LegendCell(color = npColors.heatmapLevel2)
                LegendCell(color = npColors.heatmapLevel3)
                LegendCell(color = npColors.heatmapLevel4)
                LegendCell(color = npColors.heatmapLevel5)
                Text(
                    text = if (thresholds != null) "≥${NetworkUtils.formatBytes(thresholds.maxPositive)}" else "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        if (thresholds != null && thresholds.minPositive < thresholds.maxPositive) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quintiles: L1 ≤ ${NetworkUtils.formatBytes(thresholds.t1)} • L2 ≤ ${NetworkUtils.formatBytes(thresholds.t2)} • L3 ≤ ${NetworkUtils.formatBytes(thresholds.t3)} • L4 ≤ ${NetworkUtils.formatBytes(thresholds.t4)} • L5 > ${NetworkUtils.formatBytes(thresholds.t4)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 9.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LegendCell(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
    )
}

@Composable
private fun DailyTableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "DATE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            text = "MOBILE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "WI-FI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "TOTAL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun DailyTableRow(
    cell: DayHeatmapCell,
    onClick: () -> Unit
) {
    val npColors = LocalNetPulseColors.current
    val entity = cell.entity
    val isToday = cell.isToday

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Column
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = if (isToday) "Today" else cell.displayDate,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
            )
        }

        // Mobile Data Column
        Text(
            text = if (entity != null) entity.formattedMobile else "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (entity != null && entity.totalMobileBytes > 0) npColors.upload else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Wi-Fi Column
        Text(
            text = if (entity != null) entity.formattedWifi else "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (entity != null && entity.totalWifiBytes > 0) npColors.download else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Total Column
        Text(
            text = if (entity != null) entity.formattedTotal else "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (entity != null && entity.totalAllBytes > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun UsageDetailDialog(
    cell: DayHeatmapCell,
    onDismiss: () -> Unit
) {
    val npColors = LocalNetPulseColors.current
    val entity = cell.entity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Usage Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cell.displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (entity == null) {
                    Text(
                        text = "Historical statistics were not recorded or are unavailable from the Android operating system for this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Mobile Data Breakdown
                    UsageDetailSection(
                        title = "Mobile Data",
                        icon = Icons.Default.CellTower,
                        accentColor = npColors.upload,
                        total = entity.formattedMobile,
                        rx = NetworkUtils.formatBytes(entity.mobileRxBytes),
                        tx = NetworkUtils.formatBytes(entity.mobileTxBytes)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Wi-Fi Breakdown
                    UsageDetailSection(
                        title = "Wi-Fi Data",
                        icon = Icons.Default.SignalWifi4Bar,
                        accentColor = npColors.download,
                        total = entity.formattedWifi,
                        rx = NetworkUtils.formatBytes(entity.wifiRxBytes),
                        tx = NetworkUtils.formatBytes(entity.wifiTxBytes)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Combined
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Combined Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = entity.formattedTotal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun UsageDetailSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    total: String,
    rx: String,
    tx: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Text(text = total, style = MaterialTheme.typography.bodyMedium, color = accentColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "↓ Download: $rx", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "↑ Upload: $tx", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UsagePermissionRequiredCard(
    onRequestUsageAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val npColors = LocalNetPulseColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(npColors.warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Permission Required",
                        tint = npColors.warning,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Usage Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Android requires the 'Usage Access' permission to query NetworkStatsManager for daily Wi-Fi and mobile data statistics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestUsageAccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grant Usage Access",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Generates an ordered 90-day sequence from 89 days ago up to today,
 * mapped to any existing DailyUsageEntity in Room.
 */
private fun generate90DaysGrid(existingRecords: List<DailyUsageEntity>): List<DayHeatmapCell> {
    val recordsMap = existingRecords.associateBy { it.date }
    val displaySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())

    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val result = ArrayList<DayHeatmapCell>(90)

    for (offset in 89 downTo 0) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayCalendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        val dateStr = NetworkUtils.formatDateString(cal.timeInMillis)
        val entity = recordsMap[dateStr]

        result.add(
            DayHeatmapCell(
                dateStr = dateStr,
                displayDate = displaySdf.format(cal.time),
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                monthName = monthSdf.format(cal.time),
                isToday = (offset == 0),
                entity = entity
            )
        )
    }

    return result
}

/**
 * Organizes days into 7-day columns (weeks) for the contribution grid
 */
private fun chunkIntoWeeks(days: List<DayHeatmapCell>): List<List<DayHeatmapCell?>> {
    val weeks = mutableListOf<MutableList<DayHeatmapCell?>>()
    var currentWeek = MutableList<DayHeatmapCell?>(7) { null }

    days.forEach { day ->
        // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to Monday-based index (Mon=0..Sun=6)
        val mondayBasedDayIndex = when (day.dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        currentWeek[mondayBasedDayIndex] = day

        // When Sunday (6) is filled or we reached the end
        if (mondayBasedDayIndex == 6) {
            weeks.add(currentWeek)
            currentWeek = MutableList(7) { null }
        }
    }

    if (currentWeek.any { it != null }) {
        weeks.add(currentWeek)
    }

    return weeks
}
