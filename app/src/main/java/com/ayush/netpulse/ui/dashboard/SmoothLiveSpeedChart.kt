package com.ayush.netpulse.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    val latestSample by rememberUpdatedState(latest)
    val sampleCount by rememberUpdatedState(samples.size)
    LaunchedEffect(animate, resumed, samples.size >= 2) {
        if (!animate || !resumed || sampleCount < 2) {
            viewport.value = latestSample.toDouble()
            return@LaunchedEffect
        }
        var previousFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val delta = ((frame - previousFrame) / 1_000_000.0).coerceIn(0.0, 100.0)
            previousFrame = frame
            val newest = latestSample.toDouble()
            // Keep a small display buffer so arriving samples are already available when drawn.
            val targetEnd = newest + 150.0
            val error = targetEnd - viewport.value
            viewport.value = if (kotlin.math.abs(error) > 12_000) newest - 750.0
                else viewport.value + (delta * (1.0 + error / 1000.0).coerceIn(.25, 2.0)).coerceAtMost((newest + 1500.0 - viewport.value).coerceAtLeast(0.0))
        }
    }
    LaunchedEffect(latest, animate, resumed) {
        if (!animate || !resumed || samples.size < 2) viewport.value = latest.toDouble()
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (!animate) "Recent traffic" else "Live traffic", style = MaterialTheme.typography.labelMedium)
        Text("Peak: " + NetworkUtils.formatSpeed(peak), style = MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(8.dp))
    val groups = remember(samples) { splitLiveSamples(samples) }
    val downPath = remember { Path() }
    val upPath = remember { Path() }
    val area = remember { Path() }
    val gradient = remember(colors.download) { Brush.verticalGradient(listOf(colors.download.copy(alpha = .18f), colors.download.copy(alpha = .02f))) }
    val strokeWidth = 2.dp
    Canvas(Modifier.fillMaxWidth().height(108.dp).semantics {
        contentDescription = "Live traffic chart, peak " + NetworkUtils.formatSpeed(peak)
    }) {
        val bottom = (size.height - 2.dp.toPx()).coerceAtLeast(0f)
        repeat(3) { i -> val y = bottom * i / 2f; drawLine(grid, Offset(0f, y), Offset(size.width, y)) }
        val end = viewport.value
        val start = end - 60_000.0
        val ceiling = maxOf(scale.value, target, 1f)
        val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun x(sample: LiveSpeed) = ((sample.timestamp - start) / 60_000.0 * size.width).toFloat()
        fun y(sample: LiveSpeed, download: Boolean): Float {
            val value = if (download) sample.downloadBytesPerSec else sample.uploadBytesPerSec
            return bottom * (1f - (value / ceiling).coerceIn(0f, 1f))
        }
        fun series(download: Boolean, color: Color, path: Path) {
            groups.forEach { group ->
                if (group.size == 1) {
                    drawCircle(color, 2.dp.toPx(), Offset(x(group[0]), y(group[0], download)))
                } else {
                    path.reset()
                    var px = x(group[0])
                    var py = y(group[0], download)
                    path.moveTo(px, py)
                    for (index in 1 until group.size) {
                        val nx = x(group[index])
                        val ny = y(group[index], download)
                        val mid = (px + nx) / 2f
                        path.cubicTo(mid, py, mid, ny, nx, ny)
                        px = nx
                        py = ny
                    }
                    if (download) {
                        area.reset()
                        area.addPath(path)
                        area.lineTo(px, bottom)
                        area.lineTo(x(group[0]), bottom)
                        area.close()
                        drawPath(area, gradient)
                    }
                    drawPath(path, color, style = stroke)
                }
            }
        }
        clipRect {
            series(true, colors.download, downPath)
            series(false, colors.upload, upPath)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("60s ago", style = MaterialTheme.typography.labelSmall)
        Text(if (samples.size < 2) "Collecting samples" else "Now", style = MaterialTheme.typography.labelSmall)
    }
}

/** Separate missing intervals once per sample update, not on every rendered frame. */
internal fun splitLiveSamples(samples: List<LiveSpeed>): List<List<LiveSpeed>> {
    val groups = mutableListOf<MutableList<LiveSpeed>>()
    var previous: Long? = null
    samples.forEach { sample ->
        if (previous == null || sample.timestamp - previous!! > 12_000L) groups.add(mutableListOf())
        groups.last().add(sample)
        previous = sample.timestamp
    }
    return groups
}
