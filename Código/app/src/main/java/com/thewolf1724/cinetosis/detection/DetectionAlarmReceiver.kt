package com.thewolf1724.cinetosis.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.thewolf1724.cinetosis.data.SettingsRepository
import com.thewolf1724.cinetosis.service.OverlayService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.sqrt

/**
 * Comprobación periódica (disparada por [DetectionScheduler]) de si el usuario va en coche.
 * No usa servicio en primer plano ni notificación: hace un muestreo corto del acelerómetro con
 * [goAsync] (límite ~10 s) y, si parece un vehículo, enciende el overlay.
 */
class DetectionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val settings = runBlocking { SettingsRepository(app).settings.first() }

        // Si el usuario desactivó la detección, no reprogramamos (se detiene la cadena).
        if (!settings.autoDetect) {
            DetectionScheduler.cancel(app)
            return
        }
        // Reprograma la siguiente comprobación.
        DetectionScheduler.schedule(app, settings.detectionMode)

        // No hay nada que hacer si ya está activo, no hay permiso, o se apagó hace poco.
        if (OverlayService.isRunning) return
        if (!Settings.canDrawOverlays(app)) return
        if (System.currentTimeMillis() - DetectionState.manualOffAt(app) < COOLDOWN_MS) return

        val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) ?: return

        val pending = goAsync()
        val magnitudes = ArrayList<Float>(64)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                magnitudes.add(sqrt(x * x + y * y + z * z))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL)

        Handler(Looper.getMainLooper()).postDelayed({
            sensorManager.unregisterListener(listener)
            val vehicle = VehicleClassifier.isVehicleByMotion(
                magnitudes.toFloatArray(),
                WINDOW_MS / 1000f,
            )
            if (vehicle && !OverlayService.isRunning && Settings.canDrawOverlays(app)) {
                OverlayService.start(app)
            }
            pending.finish()
        }, WINDOW_MS)
    }

    companion object {
        private const val WINDOW_MS = 8_000L      // muestreo < 10 s (límite de un BroadcastReceiver)
        private const val COOLDOWN_MS = 120_000L  // 2 min tras un apagado manual
    }
}
