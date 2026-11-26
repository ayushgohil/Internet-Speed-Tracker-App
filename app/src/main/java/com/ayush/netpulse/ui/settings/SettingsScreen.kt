package com.ayush.netpulse.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ayush.netpulse.util.DataLimit
import com.ayush.netpulse.util.NetworkUtils
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.data.model.DiagnosticsInfo
import com.ayush.netpulse.ui.dashboard.DashboardUiState
import com.ayush.netpulse.ui.logs.LogsTab
import com.ayush.netpulse.ui.theme.LocalNetPulseColors

@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    onBack: () -> Unit,
    onToggleBackgroundMonitoring: () -> Unit,
    onToggleSpeedInStatusBar: () -> Unit,
    onToggleShowOnLockScreen: () -> Unit,
    onSetDataLimitGb: (Double) -> Unit,
    onSetDailyDataLimitGb: (Double) -> Unit = {},
    onSetDataLimitCycleDay: (Int) -> Unit,
    onSetSpeedUnit: (String) -> Unit,
    onSetNotificationStyle: (String) -> Unit = {},
    onSetNotificationRefreshInterval: (Long) -> Unit = {},
    onSetAppTheme: (String) -> Unit = {},
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onSetRetentionDays: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onReconcileUsage: () -> Unit,
    initialOpenSpeedLogs: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showSpeedLogs by rememberSaveable { mutableStateOf(initialOpenSpeedLogs) }

    if (showSpeedLogs) {
        LogsTab(
            records = uiState.recentRecords,
            onClearLogs = onClearLogs,
            onBack = { showSpeedLogs = false },
            modifier = modifier
        )
        return
    }

    val netPulseColors = LocalNetPulseColors.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showDataLimitDialog by remember { mutableStateOf(false) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var showCycleDayDialog by remember { mutableStateOf(false) }
    var showNotificationStyleDialog by remember { mutableStateOf(false) }
    var showRefreshIntervalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val allPermissionsReady = uiState.hasUsageAccessPermission &&
            uiState.hasNotificationPermission &&
            uiState.isBatteryOptimizationIgnored

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. APPEARANCE
        item {
            SectionHeader(title = "Appearance")
            PreferenceGroup {
                PreferenceClickableRow(
                    title = "Theme",
                    value = when (uiState.appTheme) {
                        "Light" -> "Light (Blueberry + Cream Soda)"
                        "System" -> "Follow system"
                        else -> "Dark (Midnight Blueberry)"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
        }

        // 2. MONITORING
        item {
            SectionHeader(title = "Monitoring")
            PreferenceGroup {
                // Background Monitoring Toggle
                PreferenceSwitchRow(
                    title = "Background monitoring",
                    subtitle = "Track speeds even when app is closed",
                    checked = uiState.isBackgroundServiceRunning,
                    onCheckedChange = { onToggleBackgroundMonitoring() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Speed Display Units
                PreferenceClickableRow(
                    title = "Speed display units",
                    value = uiState.speedUnitPreference,
                    onClick = { showUnitDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Status Bar Numeric Speed
                PreferenceSwitchRow(
                    title = "Status-bar numeric speed",
                    subtitle = "Show live download rate in status bar",
                    checked = uiState.showSpeedInStatusBar,
                    onCheckedChange = { onToggleSpeedInStatusBar() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Show notification details on the lock screen (off by default)
                PreferenceSwitchRow(
                    title = "Show on lock screen",
                    subtitle = "Show live speed and usage while the phone is locked",
                    checked = uiState.showOnLockScreen,
                    onCheckedChange = { onToggleShowOnLockScreen() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Notification Style
                PreferenceClickableRow(
                    title = "Notification style",
                    value = when (uiState.notificationStyle) {
                        "Detailed" -> "Detailed"
                        else -> "Minimal (compact)"
                    },
                    onClick = { showNotificationStyleDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Notification Refresh Preference
                PreferenceClickableRow(
                    title = "Notification refresh rate",
                    value = when (uiState.notificationRefreshIntervalMs) {
                        1000L -> "1 second (interactive)"
                        else -> "2.5 seconds (battery saver)"
                    },
                    onClick = { showRefreshIntervalDialog = true }
                )
            }
        }

        item { CustomizationSettings() }

        // DATA LIMIT
        item {
            SectionHeader(title = "Data limit")
            PreferenceGroup {
                PreferenceClickableRow(
                    title = "Daily mobile data limit",
                    value = if (uiState.dailyDataLimitBytes > 0) "${DataLimit.formatGb(uiState.dailyDataLimitBytes)} GB" else "Off",
                    onClick = { showDailyLimitDialog = true }
                )
                if (uiState.dailyDataLimitBytes > 0 && uiState.hasUsageAccessPermission) {
                    val used = uiState.dailyUsage.totalMobileBytes
                    val percent = DataLimit.percentUsed(used, uiState.dailyDataLimitBytes)
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Today: ${NetworkUtils.formatBytes(used)} used ($percent%)",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth())
                        Text("Resets at local midnight. Alerts at 80% and 100%.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                PreferenceClickableRow(
                    title = "Monthly mobile data limit",
                    value = if (uiState.dataLimitBytes > 0L) "${DataLimit.formatGb(uiState.dataLimitBytes)} GB" else "Off",
                    onClick = { showDataLimitDialog = true }
                )

                if (uiState.dataLimitBytes > 0L) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    PreferenceClickableRow(
                        title = "Plan renews on day",
                        value = "${uiState.dataLimitCycleDay}",
                        onClick = { showCycleDayDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    val percent = DataLimit.percentUsed(uiState.cycleMobileBytes, uiState.dataLimitBytes)
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = if (uiState.hasUsageAccessPermission) {
                                "Used this cycle: ${NetworkUtils.formatBytes(uiState.cycleMobileBytes)} of " +
                                    "${DataLimit.formatGb(uiState.dataLimitBytes)} GB ($percent%)"
                            } else "Allow Usage Access to track the limit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (percent >= DataLimit.WARN_PERCENT) netPulseColors.warning else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You get an alert at ${DataLimit.WARN_PERCENT}% and when the limit is reached. " +
                                "Counts mobile data only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. PERMISSIONS & BATTERY
        item {
            SectionHeader(title = "Permissions & Battery")
            PreferenceGroup {
                if (allPermissionsReady) {
                    // All permissions ready summary row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPermissionsDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Permissions — All ready",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Attention needed warning row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPermissionsDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = netPulseColors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Permissions — Attention needed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap to review missing permissions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = netPulseColors.warning
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                    // Inline items needing attention for immediate resolution
                    if (!uiState.hasUsageAccessPermission) {
                        PermissionActionRow(
                            name = "Usage Access",
                            status = "Needs Permission",
                            isGranted = false,
                            onRequest = onRequestUsageAccess
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }

                    if (!uiState.hasNotificationPermission) {
                        PermissionActionRow(
                            name = "Notifications",
                            status = "Needs Permission",
                            isGranted = false,
                            onRequest = onRequestNotificationPermission
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }

                    if (!uiState.isBatteryOptimizationIgnored) {
                        PermissionActionRow(
                            name = "Battery Restrictions",
                            status = "Optimized",
                            isGranted = false,
                            onRequest = onRequestBatteryOptimization
                        )
                    }
                }

                // Exact alarms (Android 12+): lets the app restart itself quickly after being killed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val context = LocalContext.current
                    var exactAlarmsAllowed by remember {
                        mutableStateOf(canScheduleExactAlarms(context))
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        exactAlarmsAllowed = canScheduleExactAlarms(context)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                    PreferenceClickableRow(
                        title = "Reliable restarts (exact alarms)",
                        value = if (exactAlarmsAllowed) "Allowed" else "Tap to allow",
                        valueColor = if (exactAlarmsAllowed) MaterialTheme.colorScheme.primary
                        else netPulseColors.warning,
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            } catch (e: Exception) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        // 4. HISTORY & STORAGE
        item {
            SectionHeader(title = "History & Storage")
            PreferenceGroup {
                PreferenceClickableRow(
                    title = "Speed log retention",
                    value = "${uiState.diagnosticsInfo.logRetentionDays} days",
                    onClick = { showRetentionDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                PreferenceClickableRow(
                    title = "Clear speed logs",
                    value = "Keep daily usage",
                    valueColor = netPulseColors.error,
                    onClick = { showClearConfirmDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // History reconciliation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReconcileUsage() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "History reconciliation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Re-sync past 90 days from system statistics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isReconcilingUsage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconcile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5. ADVANCED
        item {
            SectionHeader(title = "Advanced")
            PreferenceGroup {
                // Diagnostics entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDiagnosticsDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diagnostics & System Details",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Service status, OS process exits, and timestamps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                // Speed Logs entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpeedLogs = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speed Logs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Raw speed telemetry samples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.recentRecords.size} samples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 6. ABOUT
        item {
            SectionHeader(title = "About")
            PreferenceGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "NetPulse",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NetPulse",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Privacy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "100% Local & Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All network metrics and data statistics are processed locally on your device. Zero tracking, zero telemetry servers, and complete offline privacy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Diagnostics Dialog
    if (showDiagnosticsDialog) {
        DiagnosticsDetailsDialog(
            diagnostics = uiState.diagnosticsInfo,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    // Permissions Details Dialog
    if (showPermissionsDialog) {
        PermissionsDetailsDialog(
            uiState = uiState,
            onRequestUsageAccess = onRequestUsageAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onDismiss = { showPermissionsDialog = false }
        )
    }

    // App Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "App Theme",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(
                        "Dark" to "Dark (Midnight Blueberry)",
                        "Light" to "Light (Blueberry + Cream Soda)",
                        "System" to "Follow system"
                    ).forEach { (themeKey, label) ->
                        val isSelected = uiState.appTheme == themeKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetAppTheme(themeKey)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Refresh Interval Dialog
    if (showRefreshIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshIntervalDialog = false },
            title = {
                Text(
                    text = "Notification Refresh Rate",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(
                        1000L to "1 second (Interactive)",
                        2500L to "2.5 seconds (Battery saver)"
                    ).forEach { (intervalMs, label) ->
                        val isSelected = uiState.notificationRefreshIntervalMs == intervalMs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetNotificationRefreshInterval(intervalMs)
                                    showRefreshIntervalDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRefreshIntervalDialog = false }) {
                    Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Clear Logs Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Speed Logs?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete all temporary speed samples. Your daily usage history will remain untouched.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLogs()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(text = "Clear", color = netPulseColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Data limit dialog (GB, decimals allowed; empty or 0 turns it off)
    if (showDailyLimitDialog) {
        var input by remember { mutableStateOf(if (uiState.dailyDataLimitBytes > 0)
            (uiState.dailyDataLimitBytes.toDouble() / DataLimit.BYTES_PER_GB).toString() else "") }
        val parsed = input.trim().toDoubleOrNull()
        val valid = input.isBlank() || (parsed != null && parsed.isFinite() && parsed in 0.0..1000.0)
        AlertDialog(onDismissRequest = { showDailyLimitDialog = false },
            title = { Text("Daily data limit") },
            text = {
                Column {
                    Text("Enter your daily mobile allowance in GB. Leave blank to disable.")
                    OutlinedTextField(value = input, onValueChange = { input = it },
                        label = { Text("Limit (GB)") }, singleLine = true, isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = { TextButton(enabled = valid, onClick = {
                onSetDailyDataLimitGb(parsed ?: 0.0)
                showDailyLimitDialog = false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showDailyLimitDialog = false }) { Text("Cancel") } })
    }

    if (showDataLimitDialog) {
        var input by remember {
            mutableStateOf(if (uiState.dataLimitBytes > 0L) DataLimit.formatGb(uiState.dataLimitBytes) else "")
        }
        val parsed = input.trim().toDoubleOrNull()
        val valid = input.isBlank() || (parsed != null && parsed >= 0.0 && parsed <= 10000.0)
        AlertDialog(
            onDismissRequest = { showDataLimitDialog = false },
            title = {
                Text("Monthly data limit", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter your mobile data plan in GB. Leave empty to turn the limit off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Limit (GB)") },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        onSetDataLimitGb(parsed ?: 0.0)
                        showDataLimitDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDataLimitDialog = false }) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Billing cycle day dialog
    if (showCycleDayDialog) {
        var input by remember { mutableStateOf(uiState.dataLimitCycleDay.toString()) }
        val day = input.trim().toIntOrNull()
        val valid = day != null && day in DataLimit.MIN_CYCLE_DAY..DataLimit.MAX_CYCLE_DAY
        AlertDialog(
            onDismissRequest = { showCycleDayDialog = false },
            title = {
                Text("Plan renews on day", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Day of the month your data plan resets (1 to ${DataLimit.MAX_CYCLE_DAY}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Day") },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        onSetDataLimitCycleDay(day ?: 1)
                        showCycleDayDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCycleDayDialog = false }) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Log Retention Dialog
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = {
                Text(
                    text = "Speed Log Retention",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(7, 30, 90).forEach { days ->
                        val isSelected = uiState.diagnosticsInfo.logRetentionDays == days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetRetentionDays(days)
                                    showRetentionDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$days days",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Speed Unit Dialog
    if (showUnitDialog) {
        AlertDialog(
            onDismissRequest = { showUnitDialog = false },
            title = {
                Text(
                    text = "Speed Display Units",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf("Auto", "KB/s", "MB/s").forEach { unit ->
                        val isSelected = uiState.speedUnitPreference == unit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetSpeedUnit(unit)
                                    showUnitDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (unit) {
                                    "Auto" -> "Automatic (Adaptive)"
                                    "KB/s" -> "Kilobytes per second (KB/s)"
                                    else -> "Megabytes per second (MB/s)"
                                },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUnitDialog = false }) {
                    Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Notification Style Dialog
    if (showNotificationStyleDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationStyleDialog = false },
            title = {
                Text(
                    text = "Notification Style",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf(
                        "Minimal" to "Minimal (Compact 2-line speed & usage)",
                        "Detailed" to "Detailed (Expanded card with breakdown)"
                    ).forEach { (styleKey, label) ->
                        val isSelected = uiState.notificationStyle == styleKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetNotificationStyle(styleKey)
                                    showNotificationStyleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNotificationStyleDialog = false }) {
                    Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun PreferenceGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun PreferenceClickableRow(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.canScheduleExactAlarms() == true
    } else true

@Composable
private fun PermissionActionRow(
    name: String,
    status: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    val netPulseColors = LocalNetPulseColors.current
    val statusColor = if (isGranted) MaterialTheme.colorScheme.primary else netPulseColors.warning

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }

        if (!isGranted) {
            OutlinedButton(
                onClick = onRequest,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Grant", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PermissionsDetailsDialog(
    uiState: DashboardUiState,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permissions & Battery",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionActionRow(
                    name = "Usage Access",
                    status = if (uiState.hasUsageAccessPermission) "Granted" else "Required for historical data",
                    isGranted = uiState.hasUsageAccessPermission,
                    onRequest = onRequestUsageAccess
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                PermissionActionRow(
                    name = "Notifications",
                    status = if (uiState.hasNotificationPermission) "Granted" else "Required for live meter",
                    isGranted = uiState.hasNotificationPermission,
                    onRequest = onRequestNotificationPermission
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                PermissionActionRow(
                    name = "Battery Restrictions",
                    status = if (uiState.isBatteryOptimizationIgnored) "Unrestricted" else "Recommended Unrestricted",
                    isGranted = uiState.isBatteryOptimizationIgnored,
                    onRequest = onRequestBatteryOptimization
                )
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
private fun DiagnosticsDetailsDialog(
    diagnostics: DiagnosticsInfo,
    onDismiss: () -> Unit
) {
    val netPulseColors = LocalNetPulseColors.current
    val serviceStatusColor = when (diagnostics.resolvedServiceStatus) {
        "Active" -> MaterialTheme.colorScheme.primary
        "Needs Permission" -> netPulseColors.warning
        "Stopped" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> netPulseColors.warning
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Diagnostics & Details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiagRow("Service Status", diagnostics.resolvedServiceStatus, serviceStatusColor)
                DiagRow("Last Service Start", diagnostics.formatTimestamp(diagnostics.lastServiceStartTimestamp))
                DiagRow("Last Speed Sample", diagnostics.formatTimestamp(diagnostics.lastSpeedSampleTimestamp))
                DiagRow("Last Displayed Speed", diagnostics.formatTimestamp(diagnostics.lastDisplayedSpeedTimestamp))
                DiagRow("Last Notification Attempt", diagnostics.formatTimestamp(diagnostics.lastAttemptedNotificationTimestamp))
                DiagRow("Last Notification Update", diagnostics.formatTimestamp(diagnostics.lastNotificationUpdateTimestamp))
                if (diagnostics.serviceRestartCount > 0) {
                    DiagRow("OS Restarts Count", "${diagnostics.serviceRestartCount} times", MaterialTheme.colorScheme.primary)
                }
                DiagRow("Historical Reconciliation", diagnostics.formatTimestamp(diagnostics.lastReconciliationTimestamp))
                DiagRow("Confirmed OS Exit Reason", diagnostics.confirmedProcessExitReason, MaterialTheme.colorScheme.onSurfaceVariant)
                DiagRow(
                    "Device & OS",
                    "${diagnostics.deviceManufacturer} ${diagnostics.deviceModel} (Android ${diagnostics.androidVersion}, API ${diagnostics.apiLevel})",
                    MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (diagnostics.revivalLog.isNotBlank()) {
                    Text(
                        text = "Recent restart attempts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = diagnostics.revivalLog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }

                if (!diagnostics.lastNotificationException.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(netPulseColors.error.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Notification Issue: ${diagnostics.lastNotificationException}",
                            style = MaterialTheme.typography.bodySmall,
                            color = netPulseColors.error,
                            fontSize = 11.sp
                        )
                    }
                }

                if (!diagnostics.lastKnownError.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(netPulseColors.error.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Last Known Error: ${diagnostics.lastKnownError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = netPulseColors.error,
                            fontSize = 11.sp
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
private fun DiagRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}
