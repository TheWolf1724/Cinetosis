package com.thewolf1724.cinetosis.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Vista que dibuja los puntos en los bordes de la pantalla y los desplaza según el vector de
 * movimiento que entrega [motionProvider] (par x, y en ~[-1, 1]). Se anima a ~60 fps.
 */
@SuppressLint("ViewConstructor")
class DotsView(
    context: Context,
    private val motionProvider: () -> Pair<Float, Float>,
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    @Volatile var dotColor: Int = 0xCCFFFFFF.toInt()
    @Volatile var dotRadiusPx: Float = dp(5f)
    @Volatile var dotsPerEdge: Int = 6
    @Volatile var edgeTop = true
    @Volatile var edgeBottom = true
    @Volatile var edgeLeft = true
    @Volatile var edgeRight = true

    private val maxShiftPx = dp(22f)
    private val edgeMargin = dp(16f)

    // Desplazamiento actual interpolado (para un movimiento fluido).
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
        paint.color = dotColor
        val w = width.toFloat()
        val h = height.toFloat()
        if (edgeTop) drawLine(canvas, horizontal = true, fixed = edgeMargin, w = w, h = h)
        if (edgeBottom) drawLine(canvas, horizontal = true, fixed = h - edgeMargin, w = w, h = h)
        if (edgeLeft) drawLine(canvas, horizontal = false, fixed = edgeMargin, w = w, h = h)
        if (edgeRight) drawLine(canvas, horizontal = false, fixed = w - edgeMargin, w = w, h = h)
    }

    private fun drawLine(canvas: Canvas, horizontal: Boolean, fixed: Float, w: Float, h: Float) {
        val count = dotsPerEdge.coerceAtLeast(1)
        for (i in 0 until count) {
            val t = (i + 1f) / (count + 1f)
            if (horizontal) {
                canvas.drawCircle(t * w + curShiftX, fixed + curShiftY, dotRadiusPx, paint)
            } else {
                canvas.drawCircle(fixed + curShiftX, t * h + curShiftY, dotRadiusPx, paint)
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val FRAME_MS = 16L
    }
}
