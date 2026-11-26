package com.ayush.netpulse.ui.tools

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal object NetworkTests {
    private const val HOST = "speed.cloudflare.com"
    enum class Budget(val label: String, val downloadBytes: Int, val uploadBytes: Int, val stageMillis: Long) {
        LOW("Low data", 5_000_000, 1_000_000, 3500),
        STANDARD("Balanced", 40_000_000, 12_000_000, 6500),
        EXTENDED("Extended", 100_000_000, 30_000_000, 8500);
        val maxMb get() = (downloadBytes + uploadBytes) / 1_000_000
    }
    data class Transfer(val mbps: Double, val bytes: Long)
    data class Progress(val stage: String, val mbps: Double = 0.0, val fraction: Float = 0f, val bytes: Long = 0)

    data class Latency(val samples: List<Double>, val attempts: Int) {
        val average: Double? get() = samples.takeIf { it.isNotEmpty() }?.average()
        val jitter: Double? get() = samples.zipWithNext { a, b -> kotlin.math.abs(a - b) }
            .takeIf { it.isNotEmpty() }?.average()
    }
    data class Speed(val downloadMbps: Double, val uploadMbps: Double, val bytes: Long = 0)

    /** TCP connect latency, not ICMP ping. DNS resolution is measured separately. */
    suspend fun latency(): Latency = withContext(Dispatchers.IO) {
        val ip = InetAddress.getByName(HOST)
        val samples = mutableListOf<Double>()
        repeat(5) {
            ensureActive()
            try {
                Socket().use { socket ->
                    val start = SystemClock.elapsedRealtimeNanos()
                    socket.connect(InetSocketAddress(ip, 443), 2500)
                    samples.add((SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0)
                }
            } catch (_: java.io.IOException) { }
        }
        Latency(samples, 5)
    }

    suspend fun dns(): Pair<Long, List<String>> = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        val addresses = InetAddress.getAllByName(HOST).mapNotNull { it.hostAddress }
        (SystemClock.elapsedRealtime() - start) to addresses
    }

    /** Sequential bounded HTTPS transfers. Duration is a soft target, byte caps are hard payload limits. */
    suspend fun speed(
        budget: Budget = Budget.LOW,
        onProgress: (Progress) -> Unit = {}
    ): Speed = withContext(Dispatchers.IO) {
        fun connection(path: String) = (URL("https://$HOST$path").openConnection() as HttpsURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Accept-Encoding", "identity")
        }
        val buffer = ByteArray(64 * 1024)
        // Noncompressible payload prevents an intermediary from artificially inflating results.
        java.util.Random().nextBytes(buffer)
        suspend fun transfer(upload: Boolean, cap: Int): Transfer {
            val stage = if (upload) "Upload" else "Download"
            val start = SystemClock.elapsedRealtimeNanos()
            var bytes = 0L
            var confirmed = 0L
            var tick = start
            var tickBytes = 0L
            var smooth = 0.0
            fun elapsed() = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
            fun publish(force: Boolean = false) {
                val now = SystemClock.elapsedRealtimeNanos()
                val interval = now - tick
                if (interval >= 80_000_000L || force) {
                    if (interval >= 20_000_000L && bytes > tickBytes) {
                        val rate = throughputMbps(bytes - tickBytes, interval)
                        smooth = if (smooth == 0.0) rate else smooth * .55 + rate * .45
                        tick = now
                        tickBytes = bytes
                    }
                    onProgress(Progress(stage, smooth, maxOf(bytes.toFloat() / cap, elapsed().toFloat() / budget.stageMillis).coerceIn(0f, 1f), bytes))
                }
            }
            onProgress(Progress(stage))
            while (bytes < cap && (bytes == 0L || elapsed() < budget.stageMillis)) {
                ensureActive()
                // Bounded requests limit the amount committed to any individual HTTP transfer.
                val chunk = minOf((cap - bytes).toInt(), if (upload) 1_000_000 else 5_000_000)
                val conn = connection(if (upload) "/__up" else "/__down?bytes=$chunk")
                val before = bytes
                try {
                    if (upload) {
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.setFixedLengthStreamingMode(chunk)
                        conn.setRequestProperty("Content-Type", "application/octet-stream")
                        conn.outputStream.use { out ->
                            while (bytes - before < chunk) {
                                ensureActive()
                                check(elapsed() < 30_000) { "Upload timed out" }
                                val count = minOf(buffer.size.toLong(), chunk - (bytes - before)).toInt()
                                out.write(buffer, 0, count)
                                bytes += count
                                publish()
                            }
                        }
                        check(conn.responseCode in 200..299) { "Upload endpoint unavailable" }
                        confirmed = bytes
                    } else {
                        check(conn.responseCode == 200) { "Download endpoint unavailable" }
                        conn.inputStream.use { input ->
                            while (bytes - before < chunk) {
                                ensureActive()
                                check(elapsed() < 30_000) { "Download timed out" }
                                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), chunk - (bytes - before)).toInt())
                                if (count < 0) break
                                bytes += count
                                publish()
                                if (elapsed() >= budget.stageMillis && bytes > 0) break
                            }
                        }
                        check(bytes > before) { "Empty download" }
                        confirmed = bytes
                    }
                } finally { conn.disconnect() }
                publish(true)
            }
            ensureActive()
            check(confirmed > 0) { "No successful transfer" }
            val result = throughputMbps(confirmed, SystemClock.elapsedRealtimeNanos() - start)
            onProgress(Progress(stage, result, 1f, confirmed))
            return Transfer(result, confirmed)
        }
        val down = transfer(false, budget.downloadBytes)
        val up = transfer(true, budget.uploadBytes)
        Speed(down.mbps, up.mbps, down.bytes + up.bytes)
    }

    internal fun throughputMbps(bytes: Long, nanos: Long): Double =
        bytes.coerceAtLeast(0) * 8000.0 / nanos.coerceAtLeast(1)
}
