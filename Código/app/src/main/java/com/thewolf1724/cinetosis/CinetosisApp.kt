package com.thewolf1724.cinetosis

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

/**
 * Clase Application: crea el canal de notificación que usa el servicio en primer plano.
 *
 * El canal usa IMPORTANCE_MIN para que la notificación obligatoria del servicio en primer plano
 * sea lo más discreta posible: sin sonido, sin icono en la barra de estado y minimizada en la
 * bandeja. (Android exige una notificación mientras el servicio esté activo; no puede eliminarse.)
 */
class CinetosisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        // Elimina el canal antiguo (IMPORTANCE_LOW) para que aplique la nueva importancia MIN.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "cinetosis_overlay_quiet"
        private const val LEGACY_CHANNEL_ID = "cinetosis_overlay"
    }
}
