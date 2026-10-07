package com.myvpn.app.bg

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.myvpn.app.VpnManager

class QuickTileService : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        when (VpnManager.status.value) {
            VpnManager.Status.STARTED -> TunnelService.stop(applicationContext)
            VpnManager.Status.STOPPED -> TunnelService.start(applicationContext)
            else -> Unit
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val status = VpnManager.status.value
        tile.state = if (status == VpnManager.Status.STARTED) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = when (status) {
            VpnManager.Status.STARTED -> "متصل"
            VpnManager.Status.STARTING -> "در حال اتصال…"
            VpnManager.Status.STOPPING -> "در حال قطع…"
            else -> "قطع"
        }
        tile.updateTile()
    }
}
