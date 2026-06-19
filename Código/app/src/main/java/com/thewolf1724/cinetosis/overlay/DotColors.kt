package com.thewolf1724.cinetosis.overlay

import androidx.core.graphics.ColorUtils

/**
 * Calcula el color de **relleno** y el de **halo/contorno** de los puntos.
 *
 * - Modo automático: el relleno se adapta al tema claro/oscuro del sistema.
 * - Modo personalizado: el relleno es el color elegido por el usuario; el **halo** se calcula
 *   automáticamente (oscuro sobre colores claros, claro sobre colores oscuros) para que el punto
 *   siga viéndose sobre cualquier fondo.
 */
object DotColors {

    /** @return par (colorRelleno, colorHalo) en ARGB. */
    fun fillAndHalo(adaptive: Boolean, customArgb: Int, night: Boolean): Pair<Int, Int> =
        if (adaptive) {
            val fill = if (night) 0xF2FFFFFF.toInt() else 0xF21A1A1A.toInt()
            val halo = if (night) 0x80000000.toInt() else 0x80FFFFFF.toInt()
            fill to halo
        } else {
            val halo = if (ColorUtils.calculateLuminance(customArgb) > 0.5) {
                0x80000000.toInt()
            } else {
                0x80FFFFFF.toInt()
            }
            customArgb to halo
        }
}
