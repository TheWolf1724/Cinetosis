package com.thewolf1724.cinetosis.detection

import kotlin.math.sqrt

/**
 * Clasificador heurístico "¿voy en coche?" a partir de la magnitud de la aceleración lineal y,
 * opcionalmente, la velocidad GPS. Lógica **pura** (sin Android) para poder testearla.
 *
 * Idea: andar produce picos fuertes y periódicos (~2 Hz); estar quieto produce muy poca señal; ir
 * en coche produce una vibración más suave y de menor amplitud, además de aceleraciones sostenidas.
 * Sin GPS la detección es aproximada (es el coste de hacerlo offline y con poca batería).
 */
object VehicleClassifier {

    const val MOVING_RMS = 0.35f        // m/s^2: por debajo se considera "sin movimiento real"
    const val WALK_MIN_HZ = 1.2f        // cadencia típica de caminar
    const val WALK_MAX_HZ = 3.0f
    const val WALK_PEAK = 1.8f          // m/s^2: andar produce picos marcados
    const val VEHICLE_SPEED_MS = 4.0f   // ~14.4 km/h: por encima, casi seguro vehículo

    /** Raíz cuadrática media de las magnitudes. */
    fun rms(magnitudes: FloatArray): Float {
        if (magnitudes.isEmpty()) return 0f
        var sum = 0f
        for (m in magnitudes) sum += m * m
        return sqrt(sum / magnitudes.size)
    }

    fun maxMagnitude(magnitudes: FloatArray): Float = magnitudes.maxOrNull() ?: 0f

    /**
     * Estima los picos por segundo (cadencia): cuenta máximos locales que superan claramente la
     * media. Sirve para distinguir el patrón rítmico de caminar.
     */
    fun peaksPerSecond(magnitudes: FloatArray, durationSec: Float): Float {
        if (magnitudes.size < 3 || durationSec <= 0f) return 0f
        val mean = magnitudes.average().toFloat()
        val threshold = mean * 1.3f
        var peaks = 0
        for (i in 1 until magnitudes.size - 1) {
            val v = magnitudes[i]
            if (v > threshold && v >= magnitudes[i - 1] && v > magnitudes[i + 1]) peaks++
        }
        return peaks / durationSec
    }

    fun isMoving(rms: Float): Boolean = rms > MOVING_RMS

    fun isWalking(peaksHz: Float, maxMagnitude: Float): Boolean =
        peaksHz in WALK_MIN_HZ..WALK_MAX_HZ && maxMagnitude > WALK_PEAK

    /** Decide si el patrón de aceleración parece de vehículo (hay movimiento y no es andar). */
    fun isVehicleByMotion(magnitudes: FloatArray, durationSec: Float): Boolean {
        val r = rms(magnitudes)
        if (!isMoving(r)) return false
        val hz = peaksPerSecond(magnitudes, durationSec)
        val mx = maxMagnitude(magnitudes)
        return !isWalking(hz, mx)
    }

    /** Confirmación por velocidad GPS (m/s). */
    fun isVehicleBySpeed(speedMetersPerSecond: Float): Boolean =
        speedMetersPerSecond > VEHICLE_SPEED_MS
}
