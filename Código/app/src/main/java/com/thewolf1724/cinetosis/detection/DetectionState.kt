package com.thewolf1724.cinetosis.detection

import android.content.Context

/**
 * Estado ligero de la detección guardado en SharedPreferences (accesible sin corrutinas desde el
 * receiver de alarma, el tile y la actividad).
 */
object DetectionState {

    private const val PREFS = "cinetosis_detection"
    private const val KEY_MANUAL_OFF = "manual_off_at"

    /** Marca el instante en que el usuario apagó los indicadores a mano. */
    fun recordManualOff(context: Context) {
        prefs(context).edit().putLong(KEY_MANUAL_OFF, System.currentTimeMillis()).apply()
    }

    /** Instante (ms) del último apagado manual, para no reactivar al instante. */
    fun manualOffAt(context: Context): Long =
        prefs(context).getLong(KEY_MANUAL_OFF, 0L)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
