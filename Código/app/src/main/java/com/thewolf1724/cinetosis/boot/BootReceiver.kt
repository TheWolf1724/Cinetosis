package com.thewolf1724.cinetosis.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.detection.DetectionScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Al encender el teléfono, si el usuario lo tiene activado, **programa** la detección periódica
 * (sin servicio en primer plano ni notificación). El overlay solo se encenderá si se detecta coche.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val settings = runBlocking { SettingsRepository(context.applicationContext).settings.first() }
        if (settings.autoStartOnBoot && settings.autoDetect && Settings.canDrawOverlays(context)) {
            DetectionScheduler.schedule(context, settings.detectionMode)
        }
    }
}
