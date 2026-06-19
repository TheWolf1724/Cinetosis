package com.thewolf1724.cinetosis.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

/**
 * Motor de movimiento. Combina el acelerómetro lineal con el vector de rotación para proyectar la
 * aceleración real del vehículo sobre los ejes de la **pantalla**, de forma independiente a cómo se
 * sostenga el teléfono. Produce un vector 2D normalizado (~[-1, 1]) que indica hacia dónde deben
 * desplazarse los puntos (en el sentido de la fuerza inercial que siente el cuerpo).
 *
 * Convención (coincide con docs/CIENCIA.md):
 *  - Acelerar → puntos hacia atrás (abajo);  frenar → hacia delante (arriba).
 *  - Curva a la derecha → puntos a la izquierda;  curva a la izquierda → a la derecha.
 *
 * Si no hay sensor de rotación, cae a un modo simple en coordenadas del dispositivo.
 */
class MotionEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    // GAME_ROTATION_VECTOR no usa magnetómetro (mejor en coches, sin interferencias); si no está,
    // se usa ROTATION_VECTOR. Solo necesitamos la referencia de "arriba" (gravedad), no el norte.
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val display: Display? get() = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

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
    private var filtZ = 0f

    private val rotationMatrix = FloatArray(9)
    private var hasRotation = false

    private val alpha = 0.15f      // suavizado del filtro paso-bajo (menor = más suave)
    private val maxAccel = 6f      // m/s^2 que se mapean al máximo desplazamiento (1.0)

    /** Hay al menos el sensor imprescindible (acelerómetro lineal). */
    val hasRequiredSensors: Boolean get() = linearAccel != null

    fun start() {
        linearAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        motionX = 0f
        motionY = 0f
        filtX = 0f
        filtY = 0f
        filtZ = 0f
        hasRotation = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                filtX = MotionMath.lowPass(filtX, event.values[0], alpha)
                filtY = MotionMath.lowPass(filtY, event.values[1], alpha)
                filtZ = MotionMath.lowPass(filtZ, event.values[2], alpha)
                computeMotion()
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                hasRotation = true
            }
            else -> Unit
        }
    }

    private fun computeMotion() {
        val screenX: Float
        val screenY: Float
        if (hasRotation) {
            val (right, up) = screenAxesForRotation(display?.rotation ?: Surface.ROTATION_0)
            val (sx, sy) = MotionMath.screenComponents(
                filtX, filtY, filtZ,
                rotationMatrix,
                right[0], right[1], right[2],
                up[0], up[1], up[2],
            )
            screenX = sx
            screenY = sy
        } else {
            // Fallback sin sensor de rotación: ejes del dispositivo (válido con el móvil plano).
            screenX = filtX
            screenY = filtY
        }
        // Los puntos se mueven en sentido contrario a la aceleración (fuerza inercial sentida).
        motionX = MotionMath.mapAxis(-screenX, deadZone, maxAccel, sensitivity)
        motionY = MotionMath.mapAxis(screenY, deadZone, maxAccel, sensitivity)
    }

    /**
     * Ejes "derecha" y "arriba" de la pantalla expresados en coordenadas del dispositivo, según la
     * rotación del display (para que el mapeo sea correcto en vertical y en horizontal).
     */
    private fun screenAxesForRotation(rotation: Int): Pair<FloatArray, FloatArray> = when (rotation) {
        Surface.ROTATION_90 -> floatArrayOf(0f, 1f, 0f) to floatArrayOf(-1f, 0f, 0f)
        Surface.ROTATION_180 -> floatArrayOf(-1f, 0f, 0f) to floatArrayOf(0f, -1f, 0f)
        Surface.ROTATION_270 -> floatArrayOf(0f, -1f, 0f) to floatArrayOf(1f, 0f, 0f)
        else -> floatArrayOf(1f, 0f, 0f) to floatArrayOf(0f, 1f, 0f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}
