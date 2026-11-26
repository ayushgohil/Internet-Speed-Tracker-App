package com.ayush.netpulse.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ayush.netpulse.util.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class MonitoringTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { current ->
            current.launch {
                SpeedTrackerService.isServiceRunningState.collect { render() }
            }
        }
        render()
    }

    private fun render() {
        val running = SpeedTrackerService.isServiceRunningState.value
        val enabled = PreferencesManager.getInstance(this).isBackgroundMonitoringEnabled
        qsTile?.apply {
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (running) "NetPulse active" else if (enabled) "NetPulse resume" else "NetPulse paused"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (SpeedTrackerService.isServiceRunningState.value) {
            SpeedTrackerService.stop(this, "Quick Settings tile")
        } else {
            SpeedTrackerService.start(this, "quick-settings")
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope?.cancel()
        super.onDestroy()
    }
}
