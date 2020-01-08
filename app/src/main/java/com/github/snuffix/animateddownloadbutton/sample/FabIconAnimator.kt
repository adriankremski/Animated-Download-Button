package com.github.snuffix.animateddownloadbutton.sample

import android.animation.ObjectAnimator
import androidx.annotation.NonNull
import android.animation.AnimatorSet
import android.transition.AutoTransition
import android.transition.TransitionManager
import androidx.constraintlayout.widget.ConstraintSet
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.core.os.HandlerCompat.postDelayed
import androidx.constraintlayout.widget.ConstraintLayout
import android.transition.Transition
import android.view.View
import com.github.snuffix.animateddownloadbutton.R
import com.google.android.material.button.MaterialButton
import java.util.concurrent.atomic.AtomicBoolean


class FabIconAnimator(private val container: ConstraintLayout) {

    @DrawableRes
    private var currentIcon: Int = 0
    @StringRes
    private var currentText: Int = 0
    private var isAnimating: Boolean = false

    private val button: MaterialButton
    private val listener = object : Transition.TransitionListener {
        override fun onTransitionStart(transition: Transition) {
            isAnimating = true
        }

        override fun onTransitionEnd(transition: Transition) {
            isAnimating = false
        }

        override fun onTransitionCancel(transition: Transition) {
            isAnimating = false
        }

        override fun onTransitionPause(transition: Transition) {}

        override fun onTransitionResume(transition: Transition) {}
    }

    private// R.dimen.triple_and_half_margin is 56 dp.
    var isExtended: Boolean
        get() = button.getLayoutParams().height !== button.getResources().getDimensionPixelSize(R.dimen.triple_and_half_margin)
        set(extended) = setExtended(extended, false)

    init {
        this.button = container.findViewById(R.id.fab)
    }

    fun update(@DrawableRes icon: Int, @StringRes text: Int) {
        val isSame = currentIcon == icon && currentText == text
        currentIcon = icon
        currentText = text
        animateChange(icon, text, isSame)
    }

    fun setOnClickListener(clickListener: View.OnClickListener?) {
        if (clickListener == null) {
            button.setOnClickListener(null)
            return
        }
        val flag = AtomicBoolean(true)
        button.setOnClickListener({ view ->
            if (!flag.getAndSet(false)) return@setOnClickListener
            clickListener!!.onClick(view)
            button.postDelayed({ flag.set(true) }, 2000)
        })
    }

    private fun animateChange(@DrawableRes icon: Int, @StringRes text: Int, isSame: Boolean) {
        val extended = isExtended
        button.setText(text)
        button.setIconResource(icon)
        setExtended(extended, !isSame)
        if (!extended) twitch()
    }

    private fun setExtended(extended: Boolean, force: Boolean) {
        if (isAnimating || extended && isExtended && !force) return

        val set = ConstraintSet()
        set.clone(container.context, if (extended) R.layout.fab_extended else R.layout.fab_collapsed)

        TransitionManager.beginDelayedTransition(
            container, AutoTransition()
                .addListener(listener).setDuration(150)
        )

        if (extended)
            button.setText(currentText)
        else
            button.setText("")

        set.applyTo(container)
    }

    private fun twitch() {
        val set = AnimatorSet()
        val twitchA = animateProperty(ROTATION_Y_PROPERTY, TWITCH_START, TWITCH_END)
        val twitchB = animateProperty(ROTATION_Y_PROPERTY, TWITCH_END, TWITCH_START)

        set.play(twitchB).after(twitchA)
        set.start()
    }

    private fun animateProperty(property: String, start: Float, end: Float): ObjectAnimator {
        return ObjectAnimator.ofFloat(container, property, start, end).setDuration(DURATION.toLong())
    }

    companion object {

        private val ROTATION_Y_PROPERTY = "rotationY"

        private val TWITCH_END = 20f
        private val TWITCH_START = 0f
        private val DURATION = 200
    }
}