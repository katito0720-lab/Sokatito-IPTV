package com.streamvault.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.doOnPreDraw
import kotlin.math.min
import kotlin.math.roundToInt

/** A decorative overlay. The activity can load and accept input underneath it. */
internal class SokatitoOpeningEffect(context: Context) : View(context) {
    private val logo = requireNotNull(context.getDrawable(R.drawable.sokatito_opening_logo)).mutate()
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = Color.rgb(244, 188, 98)
    }
    private var progress = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 850L
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                dismiss()
            }
        })
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isFocusable = false
        isClickable = false
    }

    fun showIn(host: ViewGroup) {
        val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
        if (!enabled) return
        host.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        doOnPreDraw { animator.start() }
    }

    override fun onDraw(canvas: Canvas) {
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val radius = min(min(width, height) * 0.14f, 76f * resources.displayMetrics.density)
        val entrance = (progress / 0.50f).coerceAtMost(1f)
        val eased = 1f - (1f - entrance) * (1f - entrance)
        val opacity = ((1f - progress) / 0.32f).coerceIn(0f, 1f)
        canvas.drawARGB((255f * opacity).roundToInt(), 16, 12, 23)
        ring.alpha = (170f * (1f - progress) * opacity).roundToInt()
        canvas.drawCircle(centerX, centerY, radius * (1.02f + 0.55f * progress), ring)
        val extent = radius * (0.82f + 0.18f * eased)
        logo.setBounds(
            (centerX - extent).roundToInt(),
            (centerY - extent).roundToInt(),
            (centerX + extent).roundToInt(),
            (centerY + extent).roundToInt()
        )
        logo.alpha = (255f * opacity * (0.55f + 0.45f * eased)).roundToInt()
        logo.draw(canvas)
    }

    fun dismiss() {
        (parent as? ViewGroup)?.removeView(this)
    }

    override fun onDetachedFromWindow() {
        animator.removeAllListeners()
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
