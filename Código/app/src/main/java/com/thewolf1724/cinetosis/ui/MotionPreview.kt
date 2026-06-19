package com.thewolf1724.cinetosis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.thewolf1724.cinetosis.R
import com.thewolf1724.cinetosis.data.Settings
import com.thewolf1724.cinetosis.overlay.DotsView
import kotlinx.coroutines.delay

/** Fuente de movimiento sintético para la vista previa (sin sensores). */
private class DemoMotion {
    @Volatile var x: Float = 0f
    @Volatile var y: Float = 0f
}

/**
 * Vista previa de los indicadores con maniobras simuladas, sobre un fondo mitad claro mitad oscuro
 * para comprobar que los puntos (con su contorno) se ven sobre cualquier fondo. Usa los ajustes
 * actuales para que se pueda afinar amplitud, tamaño, nº de puntos y bordes mientras se mira.
 */
@Composable
fun MotionPreview(settings: Settings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val motion = remember { DemoMotion() }
    var playing by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf(context.getString(R.string.demo_idle)) }

    LaunchedEffect(playing) {
        if (!playing) {
            motion.x = 0f
            motion.y = 0f
            return@LaunchedEffect
        }
        // (etiqueta, x, y).  y+ = abajo (acelerar), y- = arriba (frenar); x± = curvas.
        val steps = listOf(
            Triple(R.string.demo_idle, 0f, 0f),
            Triple(R.string.demo_accel, 0f, 0.9f),
            Triple(R.string.demo_idle, 0f, 0f),
            Triple(R.string.demo_brake, 0f, -0.9f),
            Triple(R.string.demo_idle, 0f, 0f),
            Triple(R.string.demo_left, 0.9f, 0f),
            Triple(R.string.demo_idle, 0f, 0f),
            Triple(R.string.demo_right, -0.9f, 0f),
        )
        var i = 0
        while (true) {
            val (res, x, y) = steps[i % steps.size]
            label = context.getString(res)
            motion.x = x
            motion.y = y
            delay(900)
            i++
        }
    }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFF101010)))),
        ) {
            AndroidView(
                factory = { ctx -> DotsView(ctx) { motion.x to motion.y } },
                update = { v ->
                    v.setInsets(0, 0, 0, 0)
                    // En la preview usamos blanco con halo oscuro para evidenciar el contorno.
                    v.dotColor = 0xF2FFFFFF.toInt()
                    v.haloColor = 0x99000000.toInt()
                    v.dotRadiusPx = settings.dotSizeDp * density
                    v.dotsPerEdge = settings.dotsPerEdge
                    v.maxShiftPx = (16f + settings.amplitude * 54f) * density
                    v.edgeTop = settings.edgeTop
                    v.edgeBottom = settings.edgeBottom
                    v.edgeLeft = settings.edgeLeft
                    v.edgeRight = settings.edgeRight
                },
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = label,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        TextButton(onClick = { playing = !playing }) {
            Text(stringResource(if (playing) R.string.demo_pause else R.string.demo_play))
        }
    }
}
