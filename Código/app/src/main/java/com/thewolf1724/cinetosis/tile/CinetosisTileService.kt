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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Quick Settings Tile: activa o desactiva el overlay desde la barra de notificaciones.
 * Si falta el permiso «Mostrar sobre otras apps», abre la app para concederlo.
 *
 * El estado visual se sincroniza **en tiempo real** observando [OverlayService.isRunningFlow]
 * mientras el panel está abierto, de modo que el tile refleja siempre el estado real del overlay.
 */
class CinetosisTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        // StateFlow reemite el valor actual al suscribirse, así que el tile queda sincronizado
        // de inmediato y se actualiza con cada cambio (también al activar desde la app o auto).
        OverlayService.isRunningFlow
            .onEach { running -> setTileState(running) }
            .launchIn(newScope)
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.canDrawOverlays(this)) {
            openAppForPermission()
            return
        }
        if (OverlayService.isRunning) {
            DetectionState.recordManualOff(this)
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        // Feedback inmediato; el StateFlow confirmará el estado real en cuanto cambie el servicio.
        setTileState(!OverlayService.isRunning)
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
