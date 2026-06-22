package com.thewolf1724.cinetosis.tile

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.thewolf1724.cinetosis.MainActivity
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.detection.DetectionState
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
        val turnOn = !OverlayService.isRunning
        if (turnOn) {
            OverlayService.start(this)
        } else {
            DetectionState.recordManualOff(this)
            OverlayService.stop(this)
        }
        // Actualización visual inmediata: el flag isRunning del servicio se actualiza de forma
        // asíncrona, así que reflejamos directamente la acción que acabamos de ordenar.
        setTileState(turnOn)
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

    /** Sincroniza el tile con el estado real del servicio (al abrir la bandeja de Ajustes rápidos). */
    private fun updateTile() = setTileState(OverlayService.isRunning)

    private fun setTileState(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.contentDescription = getString(
            if (active) R.string.tile_state_on else R.string.tile_state_off,
        )
        tile.updateTile()
    }
}
