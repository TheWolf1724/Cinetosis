package com.thewolf1724.cinetosis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinetosis_settings")

/**
 * Modo de detección automática de "voy en coche". Todos son **offline** (sin Google):
 * - [BATTERY]:  movimiento significativo + acelerómetro. Consumo mínimo, precisión moderada.
 * - [BALANCED]: añade GPS (LocationManager) para confirmar por velocidad. Más preciso, más batería.
 * - [EXTREME]:  GPS + acelerómetro de forma más agresiva. Máxima precisión offline, más batería.
 */
enum class DetectionMode { BATTERY, BALANCED, EXTREME }

/** Preferencias del usuario que afectan al aspecto y comportamiento de los indicadores. */
data class Settings(
    val sensitivity: Float = 0.5f,        // 0..1
    val amplitude: Float = 0.6f,          // 0..1: recorrido del desplazamiento de los puntos
    val dotsPerEdge: Int = 6,             // puntos por borde activo
    val dotSizeDp: Float = 5f,            // radio del punto en dp
    val colorArgb: Int = DEFAULT_COLOR,   // color ARGB de los puntos (si adaptiveColor = false)
    val adaptiveColor: Boolean = true,    // color claro/oscuro automático según el tema del sistema
    val edgeTop: Boolean = true,
    val edgeBottom: Boolean = true,
    val edgeLeft: Boolean = true,
    val edgeRight: Boolean = true,
    val onboardingDone: Boolean = false,  // si el usuario ya completó el tour inicial
    val autoDetect: Boolean = true,       // encender el overlay solo al detectar coche
    val detectionMode: DetectionMode = DetectionMode.BATTERY,
    val autoStartOnBoot: Boolean = true,  // iniciar la detección (invisible) al encender el móvil
) {
    companion object {
        const val DEFAULT_COLOR: Int = 0xCCFFFFFF.toInt() // blanco semitransparente
    }
}

/** Persistencia de [Settings] mediante Jetpack DataStore (Preferences). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val AMPLITUDE = floatPreferencesKey("amplitude")
        val DOTS = intPreferencesKey("dots_per_edge")
        val SIZE = floatPreferencesKey("dot_size_dp")
        val COLOR = intPreferencesKey("color_argb")
        val ADAPTIVE = booleanPreferencesKey("adaptive_color")
        val TOP = booleanPreferencesKey("edge_top")
        val BOTTOM = booleanPreferencesKey("edge_bottom")
        val LEFT = booleanPreferencesKey("edge_left")
        val RIGHT = booleanPreferencesKey("edge_right")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
        val DETECTION_MODE = stringPreferencesKey("detection_mode")
        val BOOT = booleanPreferencesKey("auto_start_boot")
    }

    private fun Preferences.toSettings(): Settings = Settings(
        sensitivity = this[Keys.SENSITIVITY] ?: 0.5f,
        amplitude = this[Keys.AMPLITUDE] ?: 0.6f,
        dotsPerEdge = this[Keys.DOTS] ?: 6,
        dotSizeDp = this[Keys.SIZE] ?: 5f,
        colorArgb = this[Keys.COLOR] ?: Settings.DEFAULT_COLOR,
        adaptiveColor = this[Keys.ADAPTIVE] ?: true,
        edgeTop = this[Keys.TOP] ?: true,
        edgeBottom = this[Keys.BOTTOM] ?: true,
        edgeLeft = this[Keys.LEFT] ?: true,
        edgeRight = this[Keys.RIGHT] ?: true,
        onboardingDone = this[Keys.ONBOARDING] ?: false,
        autoDetect = this[Keys.AUTO_DETECT] ?: true,
        detectionMode = runCatching { DetectionMode.valueOf(this[Keys.DETECTION_MODE] ?: "") }
            .getOrDefault(DetectionMode.BATTERY),
        autoStartOnBoot = this[Keys.BOOT] ?: true,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.SENSITIVITY] = updated.sensitivity
            prefs[Keys.AMPLITUDE] = updated.amplitude
            prefs[Keys.DOTS] = updated.dotsPerEdge
            prefs[Keys.SIZE] = updated.dotSizeDp
            prefs[Keys.COLOR] = updated.colorArgb
            prefs[Keys.ADAPTIVE] = updated.adaptiveColor
            prefs[Keys.TOP] = updated.edgeTop
            prefs[Keys.BOTTOM] = updated.edgeBottom
            prefs[Keys.LEFT] = updated.edgeLeft
            prefs[Keys.RIGHT] = updated.edgeRight
            prefs[Keys.ONBOARDING] = updated.onboardingDone
            prefs[Keys.AUTO_DETECT] = updated.autoDetect
            prefs[Keys.DETECTION_MODE] = updated.detectionMode.name
            prefs[Keys.BOOT] = updated.autoStartOnBoot
        }
    }
}
