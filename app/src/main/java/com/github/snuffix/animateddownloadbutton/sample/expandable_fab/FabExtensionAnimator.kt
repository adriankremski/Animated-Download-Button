package com.github.snuffix.animateddownloadbutton.sample.expandable_fab

import android.view.View
import androidx.core.view.doOnLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.github.snuffix.animateddownloadbutton.R
import com.google.android.material.button.MaterialButton

open class FabExtensionAnimator(
    private val button: MaterialButton,
    private val collapsedFabSize: Int = button.resources.getDimensionPixelSize(R.dimen.collapsed_fab_size),
    private val expandedFabHeight: Int = button.resources.getDimensionPixelSize(R.dimen.extended_fab_height)
) {

    private val strokeWidth = button.context.resources.getDimensionPixelSize(R.dimen.fab_halo_width) * 100F

    private val sizeInterpolator = SpringSizeInterpolator()
    private val strokeAnimation = SpringAnimation(button, StrokeWidthProperty(), strokeWidth)
    private val scaleAnimation = SpringAnimation(button, ScaleProperty(), 0.8F)

    var animationEndListener: ((Boolean) -> Unit)? = null

    var isExtended: Boolean
        get() = button.layoutParams.run { height != width || width != collapsedFabSize }
        set(extended) = sizeInterpolator.run(extended)

    init {
        button.cornerRadius = collapsedFabSize
        button.setSingleLine()
        configureSpring {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        }
    }

    private fun configureSpring(options: SpringAnimation.() -> Unit) {
        sizeInterpolator.attachToSpring(options)
        strokeAnimation.apply(options)
        scaleAnimation.apply(options)
    }

    inner class SpringSizeInterpolator : FloatPropertyCompat<View>("FabExtensionSpring") {

        private val x1 = collapsedFabSize
        private var y2 = expandedFabHeight
        private var x2 = button.height

        private val spring = SpringAnimation(button, this, x1.toFloat()).apply {
            addEndListener { _, _, _, _ ->
                animationEndListener?.invoke(isExtended)
            }
        }

        fun run(extended: Boolean) = button.doOnLayout {
            val widthMeasureSpec =
                if (extended) View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                else View.MeasureSpec.makeMeasureSpec(collapsedFabSize, View.MeasureSpec.EXACTLY)

            val heightMeasureSpec =
                if (extended) View.MeasureSpec.makeMeasureSpec(expandedFabHeight, View.MeasureSpec.EXACTLY)
                else View.MeasureSpec.makeMeasureSpec(collapsedFabSize, View.MeasureSpec.EXACTLY)

            button.measure(widthMeasureSpec, heightMeasureSpec)

            x2 = button.measuredWidth
            y2 = button.measuredHeight

            spring.animateToFinalPosition(x2.toFloat())
        }

        fun attachToSpring(options: (SpringAnimation.() -> Unit)?) {
            options?.invoke(spring)
        }

        override fun getValue(button: View): Float = button.width.toFloat()

        override fun setValue(button: View, x: Float) = button.run {
            layoutParams.width = x.toInt()
            layoutParams.height = y2
            requestLayout()
            invalidate()
        }
    }

    private class StrokeWidthProperty : FloatPropertyCompat<MaterialButton>("MaterialButtonStroke") {
        override fun setValue(`object`: MaterialButton, value: Float) {
            `object`.strokeWidth = (value / 100).toInt()
        }

        override fun getValue(`object`: MaterialButton): Float {
            return `object`.strokeWidth.toFloat() * 100
        }
    }

    private class ScaleProperty : FloatPropertyCompat<View>("MaterialButtonScale") {
        override fun setValue(`object`: View, value: Float) {
            `object`.scaleX = value / 1000
            `object`.scaleY = value / 1000
        }

        override fun getValue(`object`: View): Float {
            return `object`.scaleX * 1000
        }
    }
}
