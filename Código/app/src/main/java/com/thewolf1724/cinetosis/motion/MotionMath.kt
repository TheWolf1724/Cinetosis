package com.thewolf1724.cinetosis.motion

import kotlin.math.abs

/**
 * Funciones puras (sin dependencias de Android) del procesado de movimiento, para poder
 * probarlas con tests unitarios de JVM.
 */
object MotionMath {

    /**
     * Mapea una aceleración filtrada a un desplazamiento normalizado en [-1, 1].
     *
     * @param value       aceleración filtrada (m/s^2).
     * @param deadZone    umbral por debajo del cual se ignora (m/s^2).
     * @param maxAccel    aceleración que se mapea al máximo desplazamiento.
     * @param sensitivity sensibilidad del usuario en [0, 1].
     */
    fun mapAxis(value: Float, deadZone: Float, maxAccel: Float, sensitivity: Float): Float {
        val sign = if (value >= 0f) 1f else -1f
        val magnitude = abs(value)
        if (magnitude < deadZone) return 0f
        val scaled = ((magnitude - deadZone) / (maxAccel - deadZone)).coerceIn(0f, 1f)
        val gain = 0.4f + sensitivity * 1.2f
        return (sign * scaled * gain).coerceIn(-1f, 1f)
    }

    /** Suavizado exponencial (filtro paso-bajo) de un solo paso. */
    fun lowPass(previous: Float, sample: Float, alpha: Float): Float =
        previous + alpha * (sample - previous)
}
