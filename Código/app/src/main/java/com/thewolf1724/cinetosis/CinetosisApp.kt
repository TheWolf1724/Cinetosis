package com.thewolf1724.cinetosis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Clase Application: crea el canal de notificación que usa el servicio en primer plano.
 */
class CinetosisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "cinetosis_overlay"
    }
}
