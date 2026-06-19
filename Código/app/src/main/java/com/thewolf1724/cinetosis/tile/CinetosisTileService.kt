package com.thewolf1724.cinetosis.tile

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.thewolf1724.cinetosis.MainActivity
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.service.OverlayService

/**
 * Quick Settings Tile: permite activar o desactivar el overlay desde la barra de notificaciones.
 * Si falta el permiso «Mostrar sobre otras apps», abre la app para concederlo.
 */
class CinetosisTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.canDrawOverlays(this)) {
            openAppForPermission()
            return
        }
        if (OverlayService.isRunning) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        updateTile()
    }

    private fun openAppForPermission() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = OverlayService.isRunning
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.contentDescription = getString(
            if (running) R.string.tile_state_on else R.string.tile_state_off,
        )
        tile.updateTile()
    }
}
