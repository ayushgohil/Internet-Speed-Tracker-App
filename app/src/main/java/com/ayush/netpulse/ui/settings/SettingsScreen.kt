package com.ayush.netpulse.ui.settings

import android.os.Build
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
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
import com.ayush.netpulse.ui.theme.LocalNetPulseColors

@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    onBack: () -> Unit,
    onToggleBackgroundMonitoring: () -> Unit,
    onToggleSpeedInStatusBar: () -> Unit,
    onSetSpeedUnit: (String) -> Unit,
    onSetNotificationStyle: (String) -> Unit = {},
    onSetAppTheme: (String) -> Unit = {},
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onSetRetentionDays: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onReconcileUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netPulseColors = LocalNetPulseColors.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showNotificationStyleDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, netPulseColors.cardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. APPEARANCE & THEME SECTION
        item {
            SectionTitle(title = "Appearance & Theme")
            SettingsCard {
                SettingsClickableRow(
                    title = "App theme",
                    value = when (uiState.appTheme) {
                        "Light" -> "Light theme"
                        "System" -> "System default"
                        else -> "Dark (Deep Teal)"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
        }

        // 2. MONITORING SECTION
        item {
            SectionTitle(title = "Monitoring")
            SettingsCard {
                // Background Monitoring Toggle
                SettingsSwitchRow(
                    title = "Background Monitoring",
                    subtitle = "Keep tracking even when app is closed",
                    checked = uiState.isBackgroundServiceRunning,
                    onCheckedChange = { onToggleBackgroundMonitoring() }
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                // Dynamic Status Bar Speed Icon Toggle
                SettingsSwitchRow(
                    title = "Show speed in status bar",
                    subtitle = "Display download speed as icon text (Experimental)",
                    checked = uiState.showSpeedInStatusBar,
                    onCheckedChange = { onToggleSpeedInStatusBar() }
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                // Speed Display Units
                SettingsClickableRow(
                    title = "Speed display units",
                    value = uiState.speedUnitPreference,
                    onClick = { showUnitDialog = true }
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                // Notification Style
                SettingsClickableRow(
                    title = "Notification style",
                    value = when (uiState.notificationStyle) {
                        "Detailed" -> "Detailed (Speed + Usage)"
                        else -> "Minimal (recommended)"
                    },
                    onClick = { showNotificationStyleDialog = true }
                )
            }
        }

        // 3. PERMISSIONS SECTION
        item {
            SectionTitle(title = "Permissions")
            SettingsCard {
                PermissionItem(
                    name = "Usage Access",
                    description = "Required to read daily mobile & Wi-Fi usage history",
                    isGranted = uiState.hasUsageAccessPermission,
                    statusText = if (uiState.hasUsageAccessPermission) "Granted" else "Needs Permission",
                    onRequest = onRequestUsageAccess
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                PermissionItem(
                    name = "Notification & Status Bar",
                    description = if (uiState.hasNotificationPermission) {
                        "Promoted priority active • Tap to open system channel settings"
                    } else {
                        "Required for live speed status bar meter"
                    },
                    isGranted = uiState.hasNotificationPermission,
                    statusText = if (uiState.hasNotificationPermission) "Granted" else "Needs Permission",
                    onRequest = onRequestNotificationPermission
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                PermissionItem(
                    name = "Battery Optimization",
                    description = "Recommended: Set to Unrestricted to prevent overnight termination",
                    isGranted = uiState.isBatteryOptimizationIgnored,
                    statusText = if (uiState.isBatteryOptimizationIgnored) "Unrestricted" else "Optimized",
                    onRequest = onRequestBatteryOptimization
                )
            }
        }

        // 4. HISTORY & STORAGE SECTION
        item {
            SectionTitle(title = "History & Storage")
            SettingsCard {
                SettingsClickableRow(
                    title = "Speed log retention",
                    value = "${uiState.diagnosticsInfo.logRetentionDays} days",
                    onClick = { showRetentionDialog = true }
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

                SettingsClickableRow(
                    title = "Clear speed logs",
                    value = "Keep daily usage",
                    valueColor = netPulseColors.error,
                    onClick = { showClearConfirmDialog = true }
                )

                HorizontalDivider(color = netPulseColors.cardBorder, thickness = 0.5.dp)

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
                            text = "Reconcile usage history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Scan Android NetworkStats for past 90 days",
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

        // 5. DIAGNOSTICS SECTION
        item {
            SectionTitle(title = "Diagnostics")
            DiagnosticsContentCard(diagnostics = uiState.diagnosticsInfo)
        }

        // 6. ABOUT SECTION
        item {
            SectionTitle(title = "About")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "NetPulse",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
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
                                text = "Version 1.0.0 • Deep Teal Edition",
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Privacy",
                                    tint = netPulseColors.download,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "100% Local & Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = netPulseColors.download,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All network speed metrics and daily data statistics are processed strictly on your device. NetPulse requires no account, uses no internet servers, and performs zero tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs
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
                        "Dark" to "Dark (Deep Teal)",
                        "Light" to "Light",
                        "System" to "System default"
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
                                color = if (uiState.diagnosticsInfo.logRetentionDays == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.diagnosticsInfo.logRetentionDays == days) {
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
                                color = if (uiState.speedUnitPreference == unit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.speedUnitPreference == unit) {
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
                        "Minimal" to "Minimal (Slim 2-line speed & daily usage)",
                        "Detailed" to "Detailed (Expanded card with breakdown & Stop button)"
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
                                style = MaterialTheme.typography.bodyMedium
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
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val netPulseColors = LocalNetPulseColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, netPulseColors.cardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
private fun SettingsClickableRow(
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PermissionItem(
    name: String,
    description: String,
    isGranted: Boolean,
    statusText: String,
    onRequest: () -> Unit
) {
    val netPulseColors = LocalNetPulseColors.current
    val statusColor = if (isGranted) netPulseColors.download else netPulseColors.warning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRequest,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Open Settings", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DiagnosticsContentCard(diagnostics: DiagnosticsInfo) {
    val netPulseColors = LocalNetPulseColors.current
    val serviceStatusColor = when (diagnostics.resolvedServiceStatus) {
        "Active" -> netPulseColors.download
        "Needs Permission" -> netPulseColors.warning
        "Stopped" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> netPulseColors.warning
    }

    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                "Android & Device",
                "${diagnostics.deviceManufacturer} ${diagnostics.deviceModel} (Android ${diagnostics.androidVersion}, API ${diagnostics.apiLevel})",
                MaterialTheme.colorScheme.onSurfaceVariant
            )

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
    }
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

