package com.thewolf1724.cinetosis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinetosis_settings")

/** Preferencias del usuario que afectan al aspecto y comportamiento de los indicadores. */
data class Settings(
    val sensitivity: Float = 0.5f,        // 0..1
    val dotsPerEdge: Int = 6,             // puntos por borde activo
    val dotSizeDp: Float = 5f,            // radio del punto en dp
    val colorArgb: Int = DEFAULT_COLOR,   // color ARGB de los puntos
    val edgeTop: Boolean = true,
    val edgeBottom: Boolean = true,
    val edgeLeft: Boolean = true,
    val edgeRight: Boolean = true,
    val onboardingDone: Boolean = false,  // si el usuario ya completó el tour inicial
) {
    companion object {
        const val DEFAULT_COLOR: Int = 0xCCFFFFFF.toInt() // blanco semitransparente
    }
}

/** Persistencia de [Settings] mediante Jetpack DataStore (Preferences). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val DOTS = intPreferencesKey("dots_per_edge")
        val SIZE = floatPreferencesKey("dot_size_dp")
        val COLOR = intPreferencesKey("color_argb")
        val TOP = booleanPreferencesKey("edge_top")
        val BOTTOM = booleanPreferencesKey("edge_bottom")
        val LEFT = booleanPreferencesKey("edge_left")
        val RIGHT = booleanPreferencesKey("edge_right")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }

    private fun Preferences.toSettings(): Settings = Settings(
        sensitivity = this[Keys.SENSITIVITY] ?: 0.5f,
        dotsPerEdge = this[Keys.DOTS] ?: 6,
        dotSizeDp = this[Keys.SIZE] ?: 5f,
        colorArgb = this[Keys.COLOR] ?: Settings.DEFAULT_COLOR,
        edgeTop = this[Keys.TOP] ?: true,
        edgeBottom = this[Keys.BOTTOM] ?: true,
        edgeLeft = this[Keys.LEFT] ?: true,
        edgeRight = this[Keys.RIGHT] ?: true,
        onboardingDone = this[Keys.ONBOARDING] ?: false,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.SENSITIVITY] = updated.sensitivity
            prefs[Keys.DOTS] = updated.dotsPerEdge
            prefs[Keys.SIZE] = updated.dotSizeDp
            prefs[Keys.COLOR] = updated.colorArgb
            prefs[Keys.TOP] = updated.edgeTop
            prefs[Keys.BOTTOM] = updated.edgeBottom
            prefs[Keys.LEFT] = updated.edgeLeft
            prefs[Keys.RIGHT] = updated.edgeRight
            prefs[Keys.ONBOARDING] = updated.onboardingDone
        }
    }
}
