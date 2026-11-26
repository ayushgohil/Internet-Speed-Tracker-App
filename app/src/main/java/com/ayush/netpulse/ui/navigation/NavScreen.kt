package com.ayush.netpulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavScreen(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Speed),
    USAGE("Usage", Icons.Default.DataUsage),
    APPS("Apps", Icons.Default.Apps),
    TOOLS("Tools", Icons.Default.Build)
}
