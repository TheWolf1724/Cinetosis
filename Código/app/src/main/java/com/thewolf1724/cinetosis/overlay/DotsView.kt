package com.thewolf1724.cinetosis.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Vista que dibuja los puntos en los bordes de la pantalla y los desplaza según el vector de
 * movimiento que entrega [motionProvider] (par x, y en ~[-1, 1]). Se anima a ~60 fps.
 *
 * Cada punto se dibuja con un **halo de contorno** de color contrastado para que sea visible sobre
 * cualquier fondo (no se puede leer el fondo real de un overlay sin permiso de captura de pantalla).
 * Respeta los *insets* de las barras del sistema para no quedar oculto tras la barra de estado/navegación.
 */
@SuppressLint("ViewConstructor")
class DotsView(
    context: Context,
    private val motionProvider: () -> Pair<Float, Float>,
) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    @Volatile var dotColor: Int = 0xF2FFFFFF.toInt()      // relleno (blanco casi opaco)
    @Volatile var haloColor: Int = 0x80000000.toInt()     // contorno (oscuro semitransparente)
    @Volatile var dotRadiusPx: Float = dp(5f)
    @Volatile var dotsPerEdge: Int = 6
    @Volatile var maxShiftPx: Float = dp(45f)
    @Volatile var edgeTop = true
    @Volatile var edgeBottom = true
    @Volatile var edgeLeft = true
    @Volatile var edgeRight = true

    // Insets de las barras del sistema (para no quedar tapado por la barra de estado/navegación).
    @Volatile var insetTop: Float = defaultStatusBarHeight()
    @Volatile var insetBottom: Float = 0f
    @Volatile var insetLeft: Float = 0f
    @Volatile var insetRight: Float = 0f

    private val edgeMargin = dp(14f)
    private val haloExtra = dp(1.8f) // grosor del contorno

    private var curShiftX = 0f
    private var curShiftY = 0f

    private val frame = object : Runnable {
        override fun run() {
            val (mx, my) = motionProvider()
            val targetX = mx * maxShiftPx
            val targetY = my * maxShiftPx
            curShiftX += (targetX - curShiftX) * 0.2f
            curShiftY += (targetY - curShiftY) * 0.2f
            invalidate()
            postOnAnimationDelayed(this, FRAME_MS)
        }
    }

    fun setInsets(left: Int, top: Int, right: Int, bottom: Int) {
        insetLeft = left.toFloat()
        insetTop = top.toFloat()
        insetRight = right.toFloat()
        insetBottom = bottom.toFloat()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        fillPaint.color = dotColor
        haloPaint.color = haloColor
        val w = width.toFloat()
        val h = height.toFloat()
        val top = insetTop + edgeMargin
        val bottom = h - insetBottom - edgeMargin
        val left = insetLeft + edgeMargin
        val right = w - insetRight - edgeMargin

        if (edgeTop) drawEdge(canvas, horizontal = true, fixed = top, w = w, h = h)
        if (edgeBottom) drawEdge(canvas, horizontal = true, fixed = bottom, w = w, h = h)
        if (edgeLeft) drawEdge(canvas, horizontal = false, fixed = left, w = w, h = h)
        if (edgeRight) drawEdge(canvas, horizontal = false, fixed = right, w = w, h = h)
    }

    private fun drawEdge(canvas: Canvas, horizontal: Boolean, fixed: Float, w: Float, h: Float) {
        val count = dotsPerEdge.coerceAtLeast(1)
        val spanStart: Float
        val spanLength: Float
        if (horizontal) {
            spanStart = insetLeft
            spanLength = w - insetLeft - insetRight
        } else {
            spanStart = insetTop
            spanLength = h - insetTop - insetBottom
        }
        for (i in 0 until count) {
            val t = (i + 1f) / (count + 1f)
            val pos = spanStart + t * spanLength
            val cx: Float
            val cy: Float
            if (horizontal) {
                cx = pos + curShiftX
                cy = fixed + curShiftY
            } else {
                cx = fixed + curShiftX
                cy = pos + curShiftY
            }
            canvas.drawCircle(cx, cy, dotRadiusPx + haloExtra, haloPaint)
            canvas.drawCircle(cx, cy, dotRadiusPx, fillPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    /** Altura de la barra de estado por recurso del sistema (fallback si aún no hay insets). */
    private fun defaultStatusBarHeight(): Float {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id).toFloat() else dp(24f)
    }

    companion object {
        private const val FRAME_MS = 16L
    }
}
