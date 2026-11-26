package com.ayush.netpulse.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ayush.netpulse.data.model.LiveSpeed
import com.ayush.netpulse.ui.theme.LocalNetPulseColors
import com.ayush.netpulse.util.NetworkUtils

@Composable
internal fun SmoothLiveSpeedChart(history: List<LiveSpeed>, animate: Boolean) {
    val colors = LocalNetPulseColors.current
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = .18f)
    val samples = remember(history) { history.distinctBy { it.timestamp }.sortedBy { it.timestamp } }
    val peak = remember(samples) { samples.maxOfOrNull { maxOf(it.downloadBytesPerSec, it.uploadBytesPerSec) } ?: 0L }
    val target = peak.coerceAtLeast(1024L).toFloat() * 1.15f
    val scale = remember { androidx.compose.animation.core.Animatable(target) }
    val latest = samples.lastOrNull()?.timestamp ?: 0L
    val viewport = remember { mutableStateOf(latest.toDouble()) }
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(target, animate, resumed) {
        if (animate && resumed) scale.animateTo(target, androidx.compose.animation.core.tween(650))
        else scale.snapTo(target)
    }
    LaunchedEffect(latest, animate, resumed) {
        if (!animate || !resumed || samples.size < 2 || kotlin.math.abs(viewport.value - latest) > 12_000) {
            viewport.value = latest.toDouble()
        }
        if (animate && resumed && samples.size >= 2) {
            val from = viewport.value
            val to = latest.toDouble() + 1000.0
            val start = withFrameNanos { it }
            var fraction: Double
            do {
                fraction = withFrameNanos { ((it - start) / 1_000_000_000.0).coerceIn(0.0, 1.0) }
                viewport.value = from + (to - from) * fraction
            } while (fraction < 1.0)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (!animate) "Recent traffic" else "Live traffic", style = MaterialTheme.typography.labelMedium)
        Text("Peak: " + NetworkUtils.formatSpeed(peak), style = MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(8.dp))
    Canvas(Modifier.fillMaxWidth().height(108.dp)) {
        val bottom = size.height - 2.dp.toPx()
        repeat(3) { i -> val y = bottom * i / 2f; drawLine(grid, Offset(0f, y), Offset(size.width, y)) }
        val end = viewport.value.toLong()
        val start = end - 60_000L
        val ceiling = maxOf(scale.value, 1f)
        fun series(value: (LiveSpeed) -> Long, color: Color, fill: Boolean) {
            val groups = mutableListOf<MutableList<Offset>>()
            var previous: Long? = null
            samples.forEach { sample ->
                if (previous == null || sample.timestamp - previous!! > 12_000L) groups.add(mutableListOf())
                groups.last().add(Offset((sample.timestamp - start) / 60_000f * size.width,
                    bottom * (1f - (value(sample) / ceiling).coerceIn(0f, 1f))))
                previous = sample.timestamp
            }
            groups.forEach { points ->
                if (points.size == 1) { drawCircle(color, 2.dp.toPx(), points.first()); return@forEach }
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.zipWithNext().forEach { (a, b) ->
                        val mid = (a.x + b.x) / 2f
                        cubicTo(mid, a.y, mid, b.y, b.x, b.y)
                    }
                }
                if (fill) {
                    val area = Path().apply { addPath(path); lineTo(points.last().x, bottom); lineTo(points.first().x, bottom); close() }
                    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .18f), color.copy(alpha = .02f))))
                }
                drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        clipRect {
            series({ it.downloadBytesPerSec }, colors.download, true)
            series({ it.uploadBytesPerSec }, colors.upload, false)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("60s ago", style = MaterialTheme.typography.labelSmall)
        Text(if (samples.size < 2) "Collecting samples" else "Now", style = MaterialTheme.typography.labelSmall)
    }
}
