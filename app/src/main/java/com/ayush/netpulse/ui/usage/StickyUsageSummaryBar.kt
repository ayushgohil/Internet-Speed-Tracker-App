package com.ayush.netpulse.ui.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import com.ayush.netpulse.util.NetworkUtils

@Composable
fun StickyUsageSummaryBar(
    totalMobileBytes: Long,
    totalWifiBytes: Long,
    totalCombinedBytes: Long,
    daysCount: Int,
    recordedDaysCount: Int,
    modifier: Modifier = Modifier
) {
    val npColors = LocalNetPulseColors.current
    val isPartial = recordedDaysCount < daysCount

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Row 1: "Last 90 Days" + Total Combined (with Cream Soda accent if appropriate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Last 90 Days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPartial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($recordedDaysCount/$daysCount days)",
                            style = MaterialTheme.typography.labelSmall,
                            color = npColors.warning,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = NetworkUtils.formatBytes(totalCombinedBytes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Row 2: Mobile: 145 GB | Wi-Fi: 220 GB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mobile: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NetworkUtils.formatBytes(totalMobileBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = npColors.upload,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wi-Fi: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = NetworkUtils.formatBytes(totalWifiBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = npColors.download,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
