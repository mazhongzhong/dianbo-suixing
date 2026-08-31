package com.example.worldservicelive

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import kotlin.math.sin

class PlaybackIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#A9A8B6".toColorInt()
    }
    private var phase = 0f
    private var animating = false
    private val animator = ValueAnimator.ofFloat(0f, FULL_CYCLE).apply {
        duration = 850L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setIndicatorState(animate: Boolean, color: Int) {
        paint.color = color
        if (animating != animate) {
            animating = animate
            phase = 0f
            syncAnimation()
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncAnimation()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = width * 0.10f
        val barWidth = (width - gap * (BAR_COUNT - 1)) / BAR_COUNT
        val maximumHeight = height * 0.90f
        val minimumHeight = height * 0.24f
        val centerY = height / 2f

        repeat(BAR_COUNT) { index ->
            val heightFraction = if (animating) {
                val wave = (sin(phase + index * PHASE_OFFSET) + 1f) / 2f
                0.28f + wave * 0.72f
            } else {
                STATIC_HEIGHTS[index]
            }
            val barHeight = minimumHeight + (maximumHeight - minimumHeight) * heightFraction
            val left = index * (barWidth + gap)
            canvas.drawRoundRect(
                left,
                centerY - barHeight / 2f,
                left + barWidth,
                centerY + barHeight / 2f,
                barWidth / 2f,
                barWidth / 2f,
                paint,
            )
        }
    }

    private fun syncAnimation() {
        if (animating && isAttachedToWindow) {
            if (!animator.isStarted) animator.start()
        } else {
            animator.cancel()
        }
    }

    private companion object {
        const val BAR_COUNT = 3
        const val FULL_CYCLE = 6.2831855f
        const val PHASE_OFFSET = 2.1f
        val STATIC_HEIGHTS = floatArrayOf(0.35f, 0.72f, 0.48f)
    }
}
