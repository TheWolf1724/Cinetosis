package com.thewolf1724.cinetosis.ui

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.tile.CinetosisTileService
import java.util.function.Consumer

/** Utilidades de permisos y del acceso rápido (Quick Settings Tile). */
object Permissions {

    /** ¿Tenemos permiso para dibujar sobre otras apps? */
    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Intent que abre la pantalla del sistema para conceder el permiso de overlay. */
    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )

    /** ¿La app está exenta de la optimización de batería? (Necesario para que la detección sobreviva.) */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Intent que pide al sistema eximir a la app de la optimización de batería. */
    @SuppressLint("BatteryLife")
    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )

    /** ¿Está concedido el permiso de ubicación fina? (Solo para los modos de detección con GPS.) */
    fun hasLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    /** ¿Está concedido el permiso de notificaciones? (En Android 12 e inferiores no hace falta.) */
    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

    /** ¿El sistema permite pedir añadir el tile automáticamente? (Android 13+) */
    val canRequestAddTile: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Pide al sistema añadir el acceso rápido de Cinetosis a los Ajustes rápidos (Android 13+).
     * En versiones anteriores no existe API: el usuario lo añade desde el editor de Ajustes rápidos.
     */
    fun requestAddTile(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(context, CinetosisTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_tile),
            context.mainExecutor,
            Consumer { /* resultado ignorado: el usuario acepta o rechaza en el diálogo */ },
        )
    }
}
