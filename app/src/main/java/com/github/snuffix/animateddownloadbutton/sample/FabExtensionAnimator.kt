package com.github.snuffix.animateddownloadbutton.sample

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import com.github.snuffix.animateddownloadbutton.R
import com.google.android.material.button.MaterialButton
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.util.*

@ColorInt
fun Context.colorAt(@ColorRes color: Int) = ContextCompat.getColor(this, color)

/**
 * Convenience method for [ContextCompat.getDrawable]
 */
fun Context.drawableAt(@DrawableRes drawable: Int) = ContextCompat.getDrawable(this, drawable)

@ColorInt
fun Context.themeColorAt(@AttrRes colorAttr: Int): Int = TypedValue().run typedValue@{
    this@themeColorAt.theme.resolveAttribute(colorAttr, this@typedValue, true).run { if (this) data else Color.BLACK }
}

infix fun Drawable?.sameAs(other: Drawable?): Boolean {
    if (this == null || other == null) return false
    if (constantState != null && constantState == other.constantState) return true
    return toBitmap().sameAs(other.toBitmap())
}

/**
 * An end listener that removes itself when the animation ends
 */
fun SpringAnimation.withOneShotEndListener(onEnd: () -> Unit): SpringAnimation = addEndListener(object : DynamicAnimation.OnAnimationEndListener {
    override fun onAnimationEnd(animation: DynamicAnimation<out DynamicAnimation<*>>?, canceled: Boolean, value: Float, velocity: Float) {
        removeEndListener(this)
        onEnd()
    }
})

open class FabExtensionAnimator(
    private val button: MaterialButton,
    private val collapsedFabSize: Int = button.resources.getDimensionPixelSize(R.dimen.collapsed_fab_size),
    expandedFabHeight: Int = button.resources.getDimensionPixelSize(R.dimen.extended_fab_height)
) {

    private val strokeWidth = button.context.resources.getDimensionPixelSize(R.dimen.fab_halo_width) * 100F

    private var glyphState: GlyphState = SimpleGlyphState(button.text, button.icon)

    private val sizeInterpolator = SpringSizeInterpolator(button, collapsedFabSize, expandedFabHeight)
    private val strokeAnimation = SpringAnimation(button, StrokeWidthProperty(button), strokeWidth)
    private val scaleAnimation = SpringAnimation(button, ScaleProperty(), 0.8F)

    var animationEndListener: ((Boolean) -> Unit)? = null

    @Suppress("unused")
    val isRunning: Boolean
        get() = sizeInterpolator.isRunning

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

    fun configureSpring(options: SpringAnimation.() -> Unit) {
        sizeInterpolator.attachToSpring(options)
        strokeAnimation.apply(options)
        scaleAnimation.apply(options)
    }

    @Suppress("unused")
    fun updateGlyphs(@StringRes stringRes: Int, @DrawableRes drawableRes: Int) = button.context.run {
        updateGlyphs(SimpleGlyphState(getText(stringRes), this.drawableAt(drawableRes)))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateGlyphs(text: CharSequence, @DrawableRes drawableRes: Int) = button.context.run {
        updateGlyphs(SimpleGlyphState(text, drawableAt(drawableRes)))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateGlyphs(text: CharSequence, drawable: Drawable?) = button.context.run {
        updateGlyphs(SimpleGlyphState(text, drawable))
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateGlyphs(newGlyphState: GlyphState) {
        val oldGlyphState = glyphState
        glyphState = newGlyphState
        this.button.text = newGlyphState.text
        this.button.icon = newGlyphState.icon
    }


    abstract class GlyphState {
        abstract val icon: Drawable?

        abstract val text: CharSequence
    }

    class SimpleGlyphState constructor(
        override val text: CharSequence,
        override val icon: Drawable?
    ) : GlyphState() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as SimpleGlyphState?
            return icon == that!!.icon && text == that.text
        }

        override fun hashCode(): Int {
            return Objects.hash(icon, text)
        }
    }

    inner class SpringSizeInterpolator(
        val button: MaterialButton,
        val collapsedFabSize: Int,
        val expandedFabHeight: Int
    ) : FloatPropertyCompat<View>("FabExtensionSpring") {

        val isRunning get() = spring.isRunning

        private val x1 = collapsedFabSize
        private val y1 = collapsedFabSize
        private var y2 = expandedFabHeight
        private var x2 = button.height

        private val slope get() = if (x2 != x1) (y2 - y1) / (x2 - x1) else 0

        private val intercept get() = y2 - (slope * y1)

        private val spring = SpringAnimation(button, this, x1.toFloat()).apply {
            addEndListener(object : DynamicAnimation.OnAnimationEndListener {
                override fun onAnimationEnd(animation: DynamicAnimation<out DynamicAnimation<*>>?, canceled: Boolean, value: Float, velocity: Float) {
                    animationEndListener?.invoke(isExtended)
                }
            })
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
            if (!isRunning) options?.invoke(spring)
        }

        private fun f(x: Float): Float = intercept + (slope * x)

        override fun getValue(button: View): Float = button.width.toFloat()

        override fun setValue(button: View, x: Float) = button.run {
            layoutParams.width = x.toInt()
            layoutParams.height = f(x).toInt()
            requestLayout()
            invalidate()
        }
    }

    private class StrokeWidthProperty(button: MaterialButton) : FloatPropertyCompat<MaterialButton>("MaterialButtonStroke") {
        init {
            button.strokeColor = ColorStateList.valueOf(button.context.themeColorAt(R.attr.colorAccent))
        }

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
