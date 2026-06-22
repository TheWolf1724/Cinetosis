package com.thewolf1724.cinetosis.detection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import com.thewolf1724.cinetosis.data.DetectionMode

/**
 * Programa comprobaciones periódicas de "voy en coche" mediante [AlarmManager], **sin** servicio en
 * primer plano y, por tanto, **sin notificación permanente**. Cada disparo se reprograma a sí mismo.
 *
 * El intervalo depende del modo (más frecuente = detecta antes, algo más de batería). Si el sistema
 * está en ahorro de batería, se usa el intervalo más largo. Con la app exenta de optimización de
 * batería, las alarmas se respetan con precisión.
 */
object DetectionScheduler {

    fun schedule(context: Context, mode: DetectionMode) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + intervalMs(context, mode)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun intervalMs(context: Context, mode: DetectionMode): Long {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (power.isPowerSaveMode) return INTERVAL_BATTERY
        return when (mode) {
            DetectionMode.BATTERY -> INTERVAL_BATTERY
            DetectionMode.BALANCED -> INTERVAL_BALANCED
            DetectionMode.EXTREME -> INTERVAL_EXTREME
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DetectionAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private const val INTERVAL_BATTERY = 180_000L   // ~3 min
    private const val INTERVAL_BALANCED = 90_000L   // ~90 s
    private const val INTERVAL_EXTREME = 45_000L    // ~45 s
}
