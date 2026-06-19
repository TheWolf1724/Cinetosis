package com.thewolf1724.cinetosis

import com.thewolf1724.cinetosis.detection.VehicleClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleClassifierTest {

    @Test
    fun `estar quieto no es vehiculo`() {
        val mags = FloatArray(50) { 0.05f } // ruido mínimo
        assertFalse(VehicleClassifier.isVehicleByMotion(mags, durationSec = 5f))
    }

    @Test
    fun `andar no es vehiculo`() {
        // Patrón rítmico ~2 Hz con picos fuertes, muestreado a 10 Hz durante 5 s.
        val sampleHz = 10
        val durationSec = 5f
        val mags = FloatArray((sampleHz * durationSec).toInt()) { i ->
            if (i % 5 == 0) 3.5f else 0.4f // un pico cada 5 muestras -> ~2 picos/seg
        }
        val hz = VehicleClassifier.peaksPerSecond(mags, durationSec)
        assertTrue("cadencia esperada ~2 Hz, fue $hz", hz in 1.5f..2.5f)
        assertFalse(VehicleClassifier.isVehicleByMotion(mags, durationSec))
    }

    @Test
    fun `vibracion suave y sostenida es vehiculo`() {
        // Movimiento presente (rms alto) pero sin picos marcados de caminar.
        val mags = FloatArray(50) { i -> 0.7f + 0.1f * (i % 2) } // ~0.7-0.8, picos bajos
        assertTrue(VehicleClassifier.rms(mags) > VehicleClassifier.MOVING_RMS)
        assertTrue(VehicleClassifier.isVehicleByMotion(mags, durationSec = 5f))
    }

    @Test
    fun `velocidad alta confirma vehiculo`() {
        assertTrue(VehicleClassifier.isVehicleBySpeed(6f))   // ~21 km/h
        assertFalse(VehicleClassifier.isVehicleBySpeed(1.5f)) // ~5 km/h (andar)
    }

    @Test
    fun `rms de array vacio es cero`() {
        assertEquals(0f, VehicleClassifier.rms(FloatArray(0)), 0f)
    }
}
