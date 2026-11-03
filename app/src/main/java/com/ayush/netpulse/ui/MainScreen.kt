package com.ayush.netpulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.ui.dashboard.DashboardTab
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.navigation.NavScreen
import com.ayush.netpulse.ui.settings.SettingsScreen
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import com.ayush.netpulse.ui.usage.StickyUsageSummaryBar
import com.ayush.netpulse.ui.usage.UsageTab

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun MainScreen(
    uiState: DashboardUiState,
    onToggleMonitoring: () -> Unit,
    onToggleBackgroundService: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRefreshUsage: () -> Unit,
    onClearLogs: () -> Unit,
    onSetLogRetentionDays: (Int) -> Unit = {},
    onToggleSpeedInStatusBar: () -> Unit = {},
    onToggleShowOnLockScreen: () -> Unit = {},
    onSetSpeedUnit: (String) -> Unit = {},
    onSetNotificationStyle: (String) -> Unit = {},
    onSetNotificationRefreshInterval: (Long) -> Unit = {},
    onSetAppTheme: (String) -> Unit = {},
    onReconcileUsage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val screens = remember { NavScreen.values() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { screens.size })
    val coroutineScope = rememberCoroutineScope()
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var openSettingsDirectlyToLogs by rememberSaveable { mutableStateOf(false) }

    // Preserve Usage tab scroll position across tab switching
    val usageLazyListState = rememberLazyListState()

    // Aggregate 90-day totals for sticky usage summary (recomputed only when DB records change)
    val usageSummary = remember(uiState.recentDailyRecords) {
        var mobile = 0L
        var wifi = 0L
        var recorded = 0
        uiState.recentDailyRecords.forEach {
            mobile += it.totalMobileBytes
            wifi += it.totalWifiBytes
            if (it.totalAllBytes > 0) recorded++
        }
        Triple(mobile, wifi, recorded)
    }

    if (isSettingsOpen) {
        BackHandler {
            isSettingsOpen = false
            openSettingsDirectlyToLogs = false
        }
        SettingsScreen(
            uiState = uiState,
            onBack = {
                isSettingsOpen = false
                openSettingsDirectlyToLogs = false
            },
            onToggleBackgroundMonitoring = onToggleBackgroundService,
            onToggleSpeedInStatusBar = onToggleSpeedInStatusBar,
            onToggleShowOnLockScreen = onToggleShowOnLockScreen,
            onSetSpeedUnit = onSetSpeedUnit,
            onSetNotificationStyle = onSetNotificationStyle,
            onSetNotificationRefreshInterval = onSetNotificationRefreshInterval,
            onSetAppTheme = onSetAppTheme,
            onRequestUsageAccess = onRequestUsageAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onSetRetentionDays = onSetLogRetentionDays,
            onClearLogs = onClearLogs,
            onReconcileUsage = onReconcileUsage,
            initialOpenSpeedLogs = openSettingsDirectlyToLogs,
            modifier = modifier
        )
    } else {
        // Handle Back button: on Usage tab, navigate back to Overview first
        BackHandler(enabled = pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "NetPulse",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                // Discreet monitoring status indicator badge
                                val isMonitoring = uiState.isBackgroundServiceRunning
                                val npColors = LocalNetPulseColors.current
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMonitoring) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isMonitoring) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isMonitoring) npColors.download else MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Text(
                                            text = if (isMonitoring) "Live" else "Paused",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                openSettingsDirectlyToLogs = false
                                isSettingsOpen = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    // Primary Tabs: Overview | Usage
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    ) {
                        screens.forEach { screen ->
                            val selected = pagerState.currentPage == screen.ordinal
                            Tab(
                                selected = selected,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(screen.ordinal)
                                    }
                                },
                                text = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Sticky Usage Summary: Pinned only on Usage screen
                if (pagerState.currentPage == NavScreen.USAGE.ordinal && uiState.hasUsageAccessPermission) {
                    StickyUsageSummaryBar(
                        totalMobileBytes = usageSummary.first,
                        totalWifiBytes = usageSummary.second,
                        totalCombinedBytes = usageSummary.first + usageSummary.second,
                        daysCount = 90,
                        recordedDaysCount = usageSummary.third
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                beyondBoundsPageCount = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (screens[page]) {
                    NavScreen.OVERVIEW -> {
                        DashboardTab(
                            uiState = uiState,
                            onToggleMonitoring = onToggleMonitoring,
                            onToggleBackgroundService = onToggleBackgroundService,
                            onNavigateToUsage = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(NavScreen.USAGE.ordinal)
                                }
                            },
                            onNavigateToLogs = {
                                openSettingsDirectlyToLogs = true
                                isSettingsOpen = true
                            },
                            onOpenSettings = {
                                openSettingsDirectlyToLogs = false
                                isSettingsOpen = true
                            },
                            onRequestUsageAccess = onRequestUsageAccess,
                            onRequestBatteryOptimization = onRequestBatteryOptimization,
                            onRefresh = onRefreshUsage
                        )
                    }
                    NavScreen.USAGE -> {
                        UsageTab(
                            recentDailyRecords = uiState.recentDailyRecords,
                            hasUsageAccessPermission = uiState.hasUsageAccessPermission,
                            isLoadingUsage = uiState.isLoadingUsage,
                            isReconcilingUsage = uiState.isReconcilingUsage,
                            onRequestUsageAccess = onRequestUsageAccess,
                            onRefreshUsage = onRefreshUsage,
                            lazyListState = usageLazyListState
                        )
                    }
                }
            }
        }
    }
}
