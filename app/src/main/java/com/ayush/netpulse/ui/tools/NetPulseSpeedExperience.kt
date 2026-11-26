package com.ayush.netpulse.ui.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import java.util.Locale
import kotlin.math.*

internal fun gaugeFraction(mbps: Double): Float =
    if (!mbps.isFinite() || mbps <= 0) 0f else (ln1p(mbps) / ln1p(1000.0)).toFloat().coerceIn(0f, 1f)

@Composable
internal fun NetPulseSpeedExperience(
    phase: String, measured: Double, progress: Float, running: Boolean,
    download: Double?, upload: Double?, latency: NetworkTests.Latency?, bytes: Long,
    network: String, budget: NetworkTests.Budget, enabled: Boolean, connected: Boolean,
    onBudget: (NetworkTests.Budget) -> Unit, onStart: () -> Unit, onCancel: () -> Unit, onHistory: () -> Unit
) {
    val colors = LocalNetPulseColors.current
    val accent by animateColorAsStateCompat(if (phase == "Upload" || phase == "Finishing") colors.upload else colors.download)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("NETPULSE", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Connection test", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onHistory, enabled = enabled) { Text("History") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultTile("Download", download, colors.download, Modifier.weight(1f))
                ResultTile("Upload", upload, colors.upload, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TCP latency", style = MaterialTheme.typography.labelSmall)
                    Text(latency?.average?.let { String.format(Locale.US, "%.0f ms", it) } ?: "—", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Variation", style = MaterialTheme.typography.labelSmall)
                    Text(latency?.jitter?.let { String.format(Locale.US, "%.0f ms", it) } ?: "—", fontWeight = FontWeight.SemiBold)
                }
            }
            AnimatedContent(targetState = when {
                running && phase == "Connecting" -> "connecting"
                running -> "gauge"
                phase == "Completed" -> "complete"
                else -> "ready"
            }, label = "testStage") { screen ->
                Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
                    when (screen) {
                        "connecting" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            CircularProgressIndicator(Modifier.size(112.dp), strokeWidth = 6.dp)
                            Text("Finding connection latency…", style = MaterialTheme.typography.titleMedium)
                        }
                        "gauge" -> PulseGauge(measured, accent, if (phase == "Finishing") "Upload" else phase)
                        "complete" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = colors.download.copy(alpha = .12f)) {
                                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                                    Text("✓", fontSize = 48.sp, color = colors.download)
                                }
                            }
                            Text("Test complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "%.2f MB payload used", bytes / 1_000_000.0), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onStart, enabled = enabled && connected) { Text("Test again") }
                        }
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val ring = MaterialTheme.colorScheme.primary
                            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawCircle(ring.copy(alpha = .10f), radius = size.minDimension * .49f)
                                    drawCircle(ring.copy(alpha = .35f), radius = size.minDimension * .46f, style = Stroke(2.dp.toPx()))
                                    drawCircle(ring.copy(alpha = .65f), radius = size.minDimension * .42f, style = Stroke(2.dp.toPx()))
                                }
                                Button(onClick = onStart, enabled = enabled && connected, shape = CircleShape, modifier = Modifier.size(145.dp)) {
                                    Text("GO", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(if (!connected) "No active connection" else if (phase == "Failed") "Test failed. Try again." else if (phase == "Stopped") "Test stopped" else "Ready to measure", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (running) {
                val overall = when (phase) { "Finishing" -> 1f; "Upload" -> .5f + progress * .5f; "Download" -> progress * .5f; else -> 0f }
                val animatedProgress by animateFloatAsState(overall.coerceIn(0f, 1f), tween(240), label = "testProgress")
                LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth(), color = accent)
                Text(if (phase == "Finishing") "Preparing measured results…" else phase + " · " + String.format(Locale.US, "%.2f MB transferred in stage", bytes / 1_000_000.0), style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = onCancel) { Text("Cancel test") }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(network, style = MaterialTheme.typography.bodyMedium)
                Text("Cloudflare HTTPS", style = MaterialTheme.typography.bodySmall)
            }
            Text("Test data budget", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NetworkTests.Budget.values().forEach { option ->
                    FilterChip(selected = budget == option, onClick = { onBudget(option) }, enabled = enabled,
                        modifier = Modifier.weight(1f), label = {
                            Column {
                                Text(option.label, style = MaterialTheme.typography.labelSmall)
                                Text("≤ ${option.maxMb} MB", style = MaterialTheme.typography.labelSmall)
                                Text("~${option.stageMillis * 2 / 1000}s", style = MaterialTheme.typography.labelSmall)
                            }
                        })
                }
            }
            Text("Standard is recommended for a sustained test. Transfer time targets exclude latency and connection setup. Fast connections can reach the payload cap early. Protocol overhead is extra. TCP latency is not ICMP ping.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun animateColorAsStateCompat(color: Color): State<Color> = androidx.compose.animation.animateColorAsState(color, tween(350), label = "stageColor")

@Composable
private fun ResultTile(label: String, result: Double?, color: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = color.copy(alpha = .10f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            AnimatedContent(result, label = "result") { value ->
                Text(value?.let { String.format(Locale.US, "%.2f", it) } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("Mbps", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PulseGauge(measured: Double, accent: Color, phase: String) {
    val target = if (measured.isFinite()) measured.coerceAtLeast(0.0).toFloat() else 0f
    val value by animateFloatAsState(target, spring(dampingRatio = 1f, stiffness = 105f), label = "liveSpeed")
    val fraction = gaugeFraction(value.toDouble())
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .25f)
    val measurer = rememberTextMeasurer()
    val marks = remember { listOf(0, 1, 5, 10, 25, 50, 100, 250, 500, 1000) }
    val labels = remember(measurer, muted) { marks.map { measurer.measure(it.toString(), TextStyle(color = muted, fontSize = 10.sp)) } }
    Box(Modifier.fillMaxWidth().aspectRatio(1.05f).semantics { contentDescription = "$phase speedometer" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height * .49f)
            val radius = size.minDimension * .42f
            val origin = center - Offset(radius, radius)
            val bounds = Size(radius * 2, radius * 2)
            val stroke = Stroke(15.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 135f, 270f, false, origin, bounds, style = stroke)
            if (fraction > .001f) {
                val gradient = Brush.sweepGradient(listOf(accent, lerp(accent, ink, .25f), accent), center)
                drawArc(gradient, 135f, 270f * fraction, false, origin, bounds, style = stroke)
            }
            fun point(angle: Float, r: Float): Offset {
                val radians = Math.toRadians(angle.toDouble())
                return center + Offset((cos(radians) * r).toFloat(), (sin(radians) * r).toFloat())
            }
            for (i in 0..54) {
                val a = 135f + i * 5f
                drawLine(track, point(a, radius * .88f), point(a, radius * .82f), 1.dp.toPx())
            }
            marks.forEachIndexed { index, mark ->
                val a = 135f + 270f * gaugeFraction(mark.toDouble())
                val label = labels[index]
                val p = point(a, radius * .69f)
                drawText(label, topLeft = p - Offset(label.size.width / 2f, label.size.height / 2f))
            }
            val angle = 135f + 270f * fraction
            val needle = Path().apply {
                val a = point(angle - 90f, radius * .035f)
                val b = point(angle + 90f, radius * .035f)
                val tip = point(angle, radius * .92f)
                moveTo(a.x, a.y); lineTo(tip.x, tip.y); lineTo(b.x, b.y); close()
            }
            drawPath(needle, Brush.linearGradient(listOf(accent.copy(alpha = .15f), accent), center, point(angle, radius)))
            drawCircle(accent, 5.dp.toPx(), center)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format(Locale.US, "%.2f", value.coerceAtLeast(0f)), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
            Text("Mbps · $phase", style = MaterialTheme.typography.labelLarge, color = accent)
        }
    }
}
