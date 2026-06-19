package com.thewolf1724.cinetosis.motion

import kotlin.math.abs
import kotlin.math.hypot

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

    /**
     * Descompone la aceleración del dispositivo en componentes de **pantalla** (derecha, arriba)
     * usando la matriz de rotación device→world [r] (la de `SensorManager.getRotationMatrixFromVector`).
     *
     * Toma la parte **horizontal** de la aceleración en el mundo y la proyecta sobre los ejes de la
     * pantalla (también proyectados al plano horizontal), de modo que el resultado es independiente
     * de cómo se sostenga el teléfono. Cuando el móvil está casi vertical, el eje "arriba" se vuelve
     * casi vertical y su componente horizontal tiende a 0 (limitación física inevitable).
     *
     * @param ax,ay,az  aceleración lineal filtrada en el sistema del dispositivo.
     * @param r         matriz de rotación 3x3 (row-major) device→world.
     * @param rightDx..rightDz  eje "derecha" de la pantalla en coordenadas del dispositivo.
     * @param upDx..upDz        eje "arriba" de la pantalla en coordenadas del dispositivo.
     * @return par (screenX, screenY) en m/s^2.
     */
    fun screenComponents(
        ax: Float, ay: Float, az: Float,
        r: FloatArray,
        rightDx: Float, rightDy: Float, rightDz: Float,
        upDx: Float, upDy: Float, upDz: Float,
    ): Pair<Float, Float> {
        // Aceleración en el mundo (solo nos interesa la parte horizontal x, y).
        val awX = r[0] * ax + r[1] * ay + r[2] * az
        val awY = r[3] * ax + r[4] * ay + r[5] * az
        // Ejes de la pantalla expresados en el mundo.
        var rwx = r[0] * rightDx + r[1] * rightDy + r[2] * rightDz
        var rwy = r[3] * rightDx + r[4] * rightDy + r[5] * rightDz
        var uwx = r[0] * upDx + r[1] * upDy + r[2] * upDz
        var uwy = r[3] * upDx + r[4] * upDy + r[5] * upDz
        val rlen = hypot(rwx, rwy)
        val ulen = hypot(uwx, uwy)
        if (rlen > 1e-3f) { rwx /= rlen; rwy /= rlen } else { rwx = 0f; rwy = 0f }
        if (ulen > 1e-3f) { uwx /= ulen; uwy /= ulen } else { uwx = 0f; uwy = 0f }
        val screenX = awX * rwx + awY * rwy
        val screenY = awX * uwx + awY * uwy
        return screenX to screenY
    }
}
