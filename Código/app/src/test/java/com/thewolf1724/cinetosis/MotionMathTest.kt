package com.thewolf1724.cinetosis

import com.thewolf1724.cinetosis.motion.MotionMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    private val deadZone = 0.2f
    private val maxAccel = 6f

    @Test
    fun `dentro de la zona muerta devuelve cero`() {
        assertEquals(0f, MotionMath.mapAxis(0.1f, deadZone, maxAccel, 0.5f), 0f)
        assertEquals(0f, MotionMath.mapAxis(-0.19f, deadZone, maxAccel, 1f), 0f)
    }

    @Test
    fun `aceleracion positiva produce salida positiva`() {
        val out = MotionMath.mapAxis(3f, deadZone, maxAccel, 0.5f)
        assertTrue("se esperaba salida positiva, fue $out", out > 0f)
    }

    @Test
    fun `aceleracion negativa produce salida negativa`() {
        val out = MotionMath.mapAxis(-3f, deadZone, maxAccel, 0.5f)
        assertTrue("se esperaba salida negativa, fue $out", out < 0f)
    }

    @Test
    fun `la salida nunca supera el rango -1 a 1`() {
        val high = MotionMath.mapAxis(100f, deadZone, maxAccel, 1f)
        val low = MotionMath.mapAxis(-100f, deadZone, maxAccel, 1f)
        assertTrue(high <= 1f && high >= -1f)
        assertTrue(low <= 1f && low >= -1f)
    }

    @Test
    fun `mayor sensibilidad produce mayor desplazamiento`() {
        val baja = MotionMath.mapAxis(2f, deadZone, maxAccel, 0.1f)
        val alta = MotionMath.mapAxis(2f, deadZone, maxAccel, 0.9f)
        assertTrue("alta ($alta) debería ser mayor que baja ($baja)", alta > baja)
    }

    @Test
    fun `el filtro paso-bajo se mueve hacia la muestra`() {
        // Partiendo de 0, una muestra de 10 con alpha 0.5 debe dar 5.
        assertEquals(5f, MotionMath.lowPass(0f, 10f, 0.5f), 1e-4f)
        // alpha 0 no cambia el valor previo.
        assertEquals(3f, MotionMath.lowPass(3f, 99f, 0f), 1e-4f)
    }

    @Test
    fun `con rotacion identidad las componentes de pantalla igualan x e y del dispositivo`() {
        // R = identidad (móvil plano, alineado con el mundo). z (vertical) se ignora.
        val r = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val (sx, sy) = MotionMath.screenComponents(
            ax = 2f, ay = 3f, az = 9f,
            r = r,
            rightDx = 1f, rightDy = 0f, rightDz = 0f,
            upDx = 0f, upDy = 1f, upDz = 0f,
        )
        assertEquals(2f, sx, 1e-4f)
        assertEquals(3f, sy, 1e-4f)
    }

    @Test
    fun `con el movil casi vertical la componente longitudinal de pantalla tiende a cero`() {
        // Rotación que lleva el eje Y del dispositivo a la vertical del mundo (móvil en vertical):
        // columnas = imágenes de los ejes del dispositivo -> X->(1,0,0), Y->(0,0,1), Z->(0,-1,0)
        val r = floatArrayOf(
            1f, 0f, 0f,
            0f, 0f, -1f,
            0f, 1f, 0f,
        )
        // Aceleración a lo largo del eje Y del dispositivo (vertical cuando el móvil está de pie).
        val (_, sy) = MotionMath.screenComponents(
            ax = 0f, ay = 5f, az = 0f,
            r = r,
            rightDx = 1f, rightDy = 0f, rightDz = 0f,
            upDx = 0f, upDy = 1f, upDz = 0f,
        )
        assertEquals(0f, sy, 1e-4f)
    }
}
