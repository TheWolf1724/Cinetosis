package com.thewolf1724.cinetosis.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.service.DetectionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Al encender el teléfono, si el usuario lo tiene activado, arranca el servicio de **detección**
 * (invisible, sin overlay). El overlay solo se encenderá si la detección detecta coche.
 *
 * Nota: en BOOT solo se inicia el modo de sensores (sin GPS), ya que iniciar un servicio en primer
 * plano de tipo "location" desde el arranque está restringido por el sistema.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val repository = SettingsRepository(context.applicationContext)
        val settings = runBlocking { repository.settings.first() }
        if (settings.autoStartOnBoot && settings.autoDetect && Settings.canDrawOverlays(context)) {
            DetectionService.start(context, fromBoot = true)
        }
    }
}
