package com.github.snuffix.animateddownloadbutton.sample.expandable_fab

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.snuffix.animateddownloadbutton.R
import com.github.snuffix.animateddownloadbutton.awaitEnd
import com.github.snuffix.animateddownloadbutton.getDrawableCompat
import com.github.snuffix.animateddownloadbutton.setVisible
import kotlinx.android.synthetic.main.layout_quick_actions.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val animationDuration = 200.toLong()

@SuppressLint("ViewConstructor")
class CollapsedFabButtonsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.layout_quick_actions, this)
    }

    fun setupButton(button: ButtonType, textResource: Int, iconResource: Int) {
        when (button) {
            ButtonType.LEFT -> {
                leftButtonText.text = context.getString(textResource)
                leftButtonIcon.setImageDrawable(context.getDrawableCompat(iconResource))
            }
            ButtonType.CENTER -> {
                centerButtonText.text = context.getString(textResource)
                centerButtonIcon.setImageDrawable(context.getDrawableCompat(iconResource))
            }
            ButtonType.RIGHT -> {
                rightButtonText.text = context.getString(textResource)
                rightButtonIcon.setImageDrawable(context.getDrawableCompat(iconResource))
            }
        }
    }

    fun setOnButtonClickListener(block: (ButtonType) -> Unit) {
        buttonLeft.setOnClickListener { block(ButtonType.LEFT) }
        buttonCenter.setOnClickListener { block(ButtonType.CENTER) }
        buttonRight.setOnClickListener { block(ButtonType.RIGHT) }
    }

    private val leftButtonStartXTranslation: Float
        get() = width / 2f - buttonLeft.width / 2f

    private val rightButtonStartXTranslation: Float
        get() = -(buttonRight.x - width / 2f + buttonRight.width / 2f)

    fun animateButtons(lifecycleScope: CoroutineScope, block: () -> Unit) {
        lifecycleScope.launch {
            fab.animate().rotationBy(45f).apply {
                duration = animationDuration * 2
                start()
            }

            startButtonAnimation(
                button = buttonLeft,
                startXTranslation = leftButtonStartXTranslation,
                endXTranslation = 0f,
                startYTranslation = height.toFloat(),
                endYTranslation = buttonLeft.y
            )

            startButtonAnimation(
                button = buttonCenter,
                startXTranslation = 0f,
                endXTranslation = 0f,
                startYTranslation = height.toFloat(),
                endYTranslation = 0f
            )

            startButtonAnimation(
                button = buttonRight,
                startXTranslation = rightButtonStartXTranslation,
                endXTranslation = 0f,
                startYTranslation = height.toFloat(),
                endYTranslation = buttonRight.y
            )


            block()
        }
    }

    private suspend fun startButtonAnimation(
        button: View,
        startXTranslation: Float, endXTranslation: Float,
        startYTranslation: Float, endYTranslation: Float,
        startScale: Float = 0f, endScale: Float = 1f
    ) {
        button.setVisible()
        button.translateX(startTranslation = startXTranslation, endTranslation = endXTranslation)
        button.translateY(startTranslation = startYTranslation, endTranslation = endYTranslation)
        button.animateScale(startScale = startScale, endScale = endScale)
    }

    fun hideButtons(lifecycleScope: CoroutineScope, block: () -> Unit) {
        lifecycleScope.launch {
            fab.animate().rotationBy(-45f).apply {
                duration = animationDuration * 2
                start()
            }

            startButtonAnimation(
                button = buttonRight,
                startXTranslation = 0f,
                endXTranslation = rightButtonStartXTranslation,
                startYTranslation = buttonRight.y,
                endYTranslation = height.toFloat(),
                startScale = 1f,
                endScale = 0f
            )

            startButtonAnimation(
                button = buttonCenter,
                startXTranslation = 0f,
                endXTranslation = 0f,
                startYTranslation = 0f,
                endYTranslation = height.toFloat(),
                startScale = 1f,
                endScale = 0f
            )

            startButtonAnimation(
                button = buttonLeft,
                startXTranslation = 0f,
                endXTranslation = leftButtonStartXTranslation,
                endYTranslation = height.toFloat(),
                startYTranslation = buttonLeft.y,
                startScale = 1f,
                endScale = 0f
            )

            block()
        }
    }
}

enum class ButtonType { LEFT, CENTER, RIGHT }

private fun View.translateX(startTranslation: Float, endTranslation: Float) {
    translationX = startTranslation
    animate().translationX(endTranslation).apply {
        if (endTranslation > startTranslation) {
            interpolator = AccelerateDecelerateInterpolator()
        } else {
            interpolator = DecelerateInterpolator()
        }
        duration = animationDuration
        start()
    }
}

private fun View.translateY(startTranslation: Float, endTranslation: Float) {
    translationY = startTranslation
    animate().translationY(endTranslation).apply {
        if (endTranslation > startTranslation) {
            interpolator = AccelerateDecelerateInterpolator()
        } else {
            interpolator = DecelerateInterpolator()
        }
        duration = animationDuration
        start()
    }
}

private suspend fun View.animateScale(startScale: Float = 0f, endScale: Float = 1f) {
    this.scaleX = startScale
    this.scaleY = startScale
    objectAnimator(this, "scaleX", endScale).start()
    val animator = objectAnimator(this, "scaleY", endScale)
    animator.start()
    animator.awaitEnd()
}

private fun objectAnimator(view: View, propertyName: String, finalValue: Float): ObjectAnimator {
    val animator = ObjectAnimator.ofFloat(view, propertyName, finalValue)
    animator.duration = animationDuration
    animator.interpolator = AccelerateDecelerateInterpolator()
    return animator
}


fun View.afterMeasured(block: View.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            block()
        }
    })
}
