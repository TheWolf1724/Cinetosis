package com.thewolf1724.cinetosis.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Motor de movimiento. Se suscribe al acelerómetro lineal y al giroscopio, filtra la señal y
 * produce un vector 2D normalizado (~[-1, 1]) que indica hacia dónde deben desplazarse los puntos.
 *
 * Mapeo (teléfono en vertical):
 *  - [motionX]: lateral (curvas).            Positivo = derecha.
 *  - [motionY]: longitudinal (acelerar/frenar). Invertido: al acelerar, los puntos van hacia atrás.
 *
 * Nota: una compensación completa de la orientación con TYPE_ROTATION_VECTOR es trabajo futuro
 * (ver docs/ARQUITECTURA.md).
 */
class MotionEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile
    var motionX: Float = 0f
        private set

    @Volatile
    var motionY: Float = 0f
        private set

    /** Sensibilidad configurable por el usuario (0..1). */
    @Volatile
    var sensitivity: Float = 0.5f

    /** Zona muerta en m/s^2 para ignorar microaceleraciones. */
    @Volatile
    var deadZone: Float = 0.2f

    private var filtX = 0f
    private var filtY = 0f
    private var gyroZ = 0f

    private val alpha = 0.15f      // suavizado del filtro paso-bajo (menor = más suave)
    private val maxAccel = 6f      // m/s^2 que se mapean al máximo desplazamiento (1.0)

    /** Hay al menos el sensor imprescindible (acelerómetro lineal). */
    val hasRequiredSensors: Boolean get() = linearAccel != null

    fun start() {
        linearAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        motionX = 0f
        motionY = 0f
        filtX = 0f
        filtY = 0f
        gyroZ = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                filtX += alpha * (event.values[0] - filtX)
                filtY += alpha * (event.values[1] - filtY)
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Velocidad angular alrededor del eje vertical del teléfono (viraje del vehículo).
                gyroZ += alpha * (event.values[2] - gyroZ)
            }
            else -> return
        }

        val lateral = filtX + gyroZ * 1.5f
        motionX = mapAxis(lateral)
        motionY = -mapAxis(filtY)
    }

    /** Aplica zona muerta, escalado por sensibilidad y normaliza a [-1, 1]. */
    private fun mapAxis(value: Float): Float {
        val sign = if (value >= 0f) 1f else -1f
        val magnitude = abs(value)
        if (magnitude < deadZone) return 0f
        val scaled = (magnitude - deadZone) / (maxAccel - deadZone)
        val gain = 0.4f + sensitivity * 1.2f
        return (sign * scaled.coerceIn(0f, 1f) * gain).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}
