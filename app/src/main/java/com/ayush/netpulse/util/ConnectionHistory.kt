package com.ayush.netpulse.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bounded, device-local observations. A monitoring gap is never counted as an outage. */
object ConnectionHistory {
    data class Event(val time: Long, val state: String, val durationMs: Long?)
    private const val PREFS = "netpulse_connection_history"
    private const val LIMIT = 500
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var previous: String? = null
    private var startedAt = 0L
    private var generation = 0L

    @Synchronized
    fun load(context: Context) {
        _events.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("events", "").orEmpty().lineSequence().mapNotNull { line ->
                val p = line.split("|")
                val time = p.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                Event(time, p.getOrNull(1) ?: return@mapNotNull null, p.getOrNull(2)?.toLongOrNull())
            }.take(LIMIT).toList()
    }

    @Synchronized
    private fun record(context: Context, state: String) {
        if (previous == state) return
        val now = SystemClock.elapsedRealtime()
        val duration = if (previous == null) null else (now - startedAt).coerceAtLeast(0)
        _events.value = (listOf(Event(System.currentTimeMillis(), state, duration)) + _events.value).take(LIMIT)
        persist(context)
        previous = state
        startedAt = now
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("events",
            _events.value.joinToString("\n") { "${it.time}|${it.state}|${it.durationMs ?: ""}" }).apply()
    }

    @Synchronized
    fun clear(context: Context) {
        _events.value = emptyList()
        persist(context)
        previous = null
        startedAt = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun start(context: Context) {
        if (callback != null) return
        val app = context.applicationContext
        load(app)
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val token = ++generation
        fun observe() {
            synchronized(this@ConnectionHistory) {
                if (token != generation) return
                val state = try {
                    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                    if (caps == null) "Disconnected" else {
                        val transport = when {
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                            else -> "Other"
                        }
                        transport + when {
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) -> " · Sign-in required"
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> " · Validated"
                            else -> " · Unvalidated"
                        }
                    }
                } catch (_: SecurityException) { "Unknown" }
                record(app, state)
            }
        }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = observe()
            override fun onLost(network: Network) = observe()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = observe()
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            callback = cb
            observe()
        } catch (_: RuntimeException) {
            generation++
            record(app, "Monitoring unavailable")
            previous = null
        }
    }

    @Synchronized
    fun stop(context: Context) {
        val cb = callback ?: return
        generation++
        callback = null
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(cb)
        } catch (_: RuntimeException) { }
        record(context, "Monitoring stopped")
        previous = null
    }
}
