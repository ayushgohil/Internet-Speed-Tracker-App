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
    private const val DOWNLOAD_BYTES = 5_000_000
    private const val UPLOAD_BYTES = 1_000_000

    data class Latency(val samples: List<Double>, val attempts: Int) {
        val average: Double? get() = samples.takeIf { it.isNotEmpty() }?.average()
        val jitter: Double? get() = samples.zipWithNext { a, b -> kotlin.math.abs(a - b) }
            .takeIf { it.isNotEmpty() }?.average()
    }
    data class Speed(val downloadMbps: Double, val uploadMbps: Double)

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

    /** Bounded transfer test: at most 5 MB download and 1 MB upload payload. */
    suspend fun speed(): Speed = withContext(Dispatchers.IO) {
        fun connection(path: String) = (URL("https://$HOST$path").openConnection() as HttpsURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            useCaches = false
            instanceFollowRedirects = false
            setRequestProperty("Accept-Encoding", "identity")
        }
        val download = connection("/__down?bytes=$DOWNLOAD_BYTES")
        val buffer = ByteArray(16_384)
        var received = 0L
        val start = SystemClock.elapsedRealtimeNanos()
        try {
            check(download.responseCode == 200) { "Download endpoint unavailable" }
            download.inputStream.use { input ->
                while (received < DOWNLOAD_BYTES) {
                    ensureActive()
                    check(SystemClock.elapsedRealtimeNanos() - start < 30_000_000_000L) { "Download timed out" }
                    val count = input.read(buffer, 0, minOf(buffer.size.toLong(), DOWNLOAD_BYTES - received).toInt())
                    if (count < 0) break
                    received += count
                }
            }
            check(received == DOWNLOAD_BYTES.toLong()) { "Incomplete download" }
        } finally { download.disconnect() }
        val downloadMbps = received * 8000.0 / (SystemClock.elapsedRealtimeNanos() - start).coerceAtLeast(1)
        ensureActive()
        val upload = connection("/__up").apply {
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(UPLOAD_BYTES)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        val uploadStart = SystemClock.elapsedRealtimeNanos()
        try {
            upload.outputStream.use { out ->
                var remaining = UPLOAD_BYTES
                while (remaining > 0) {
                    ensureActive()
                    val count = minOf(remaining, buffer.size)
                    out.write(buffer, 0, count)
                    remaining -= count
                }
            }
            check(upload.responseCode in 200..299) { "Upload endpoint unavailable" }
        } finally { upload.disconnect() }
        Speed(downloadMbps, UPLOAD_BYTES * 8000.0 /
            (SystemClock.elapsedRealtimeNanos() - uploadStart).coerceAtLeast(1))
    }
}
