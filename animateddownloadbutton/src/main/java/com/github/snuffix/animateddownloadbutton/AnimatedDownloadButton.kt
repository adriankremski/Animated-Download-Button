package com.github.snuffix.animateddownloadbutton

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable
import kotlinx.android.synthetic.main.animated_download_button.view.*
import kotlinx.coroutines.*

class AnimatedDownloadButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var animationDuration = 500L
    private var frameStrokeWidth = 3.dp
    private var downloadIconSize = 16.dp
    private var tickIconSize = 16.dp
    private var tintColor = -1

    private var frameRectangleRadius = 8.dp.toFloat()

    private val frameCircleCornerRadius: Float
        get() = (buttonBackgroundFrame.width / 2).toFloat()

    private val frameDrawable by lazy {
        GradientDrawable().apply {
            setStroke(frameStrokeWidth, tintColor)
            shape = GradientDrawable.RECTANGLE
        }
    }

    var onClick: ((ButtonState) -> Unit)? = null

    var scope: CoroutineScope? = null
    private var animationJob: Job? = null

    private var progressDrawable: CircularProgressDrawable? = null

    var buttonState: ButtonState = ButtonState.Download(animated = false)
        set(newValue) {
            val oldValue = field
            field = newValue

            if (oldValue != newValue) {
                when (newValue) {
                    is ButtonState.Download -> resetState(oldValue, animated = newValue.animated)
                    is ButtonState.Progress -> showProgress(oldValue, animated = newValue.animated)
                    is ButtonState.Success -> showSuccess(oldValue, animated = newValue.animated)
                }
            }
        }

    private fun resetState(previousState: ButtonState, animated: Boolean) {
        animationJob = scope?.launch {
            // Other animation might still be going
            animationJob?.join()

            if (animated) {
                resetStateAnimated(previousState)
            } else {
                frameDrawable.cornerRadius = frameRectangleRadius
                buttonTickIcon.alpha = 0f
                buttonProgressBar.setGone()
                buttonDownloadIcon.alpha = 1f
                buttonBackgroundFrame.alpha = 0.1f
            }
        }
    }

    private suspend fun resetStateAnimated(previousState: ButtonState) {
        if (previousState is ButtonState.Progress) {
            val progressAnimation = createAlphaAnimationAsync(view = buttonProgressBar, alpha = 0f)
            val frameAlphaAnimation = createAlphaAnimationAsync(view = buttonBackgroundFrame, alpha = 0.1f)

            progressAnimation?.await()
            frameAlphaAnimation?.await()
        } else if (previousState is ButtonState.Success) {
            val tickAnimation = createAlphaAnimationAsync(view = buttonTickIcon, alpha = 0f)
            val frameAlphaAnimation = createAlphaAnimationAsync(view = buttonBackgroundFrame, alpha = 0.1f)

            tickAnimation?.await()
            frameAlphaAnimation?.await()
        }

        val backgroundCornersAnimation = createFrameCornersAnimationAsync(startCornerRadius = frameCircleCornerRadius, finalCornerRadius = frameRectangleRadius)
        val downloadIconAnimation = createAlphaAnimationAsync(view = buttonDownloadIcon, alpha = 1f)

        // Wait for both animations to end
        backgroundCornersAnimation?.await()
        downloadIconAnimation?.await()
    }

    private suspend fun createAlphaAnimationAsync(view: View, alpha: Float, alphaDuration: Long = animationDuration) = scope?.async {
        view.animate().alpha(alpha).run {
            duration = alphaDuration
            start()
            awaitEnd()
        }
    }

    private suspend fun createFrameCornersAnimationAsync(startCornerRadius: Float, finalCornerRadius: Float) = scope?.async {
        // Animate corners
        val backgroundCornersAnimator = ValueAnimator.ofFloat(startCornerRadius, finalCornerRadius)

        backgroundCornersAnimator.setDuration(animationDuration).addUpdateListener { animation ->
            frameDrawable.cornerRadius = animation.animatedValue as Float
        }

        backgroundCornersAnimator.start()
        backgroundCornersAnimator.awaitEnd()
    }

    private fun showProgress(previousState: ButtonState, animated: Boolean) {
        animationJob = scope?.launch {
            animationJob?.join()

            if (animated) {
                showProgressAnimated(previousState)
            } else {
                frameDrawable.cornerRadius = frameCircleCornerRadius
                buttonProgressBar.alpha = 1f
                buttonBackgroundFrame.alpha = 0.1f
                buttonTickIcon.alpha = 0f
                buttonDownloadIcon.alpha = 0f
            }
        }
    }

    private suspend fun showProgressAnimated(previousState: ButtonState) {
        if (previousState is ButtonState.Download) {
            val backgroundCornersAnimation = createFrameCornersAnimationAsync(startCornerRadius = frameRectangleRadius, finalCornerRadius = frameCircleCornerRadius)
            val downloadIconAnimation = createAlphaAnimationAsync(view = buttonDownloadIcon, alpha = 0f)

            backgroundCornersAnimation?.await()
            downloadIconAnimation?.await()
        } else if (previousState is ButtonState.Success) {
            val tickAnimation = createAlphaAnimationAsync(view = buttonTickIcon, alpha = 0f)
            val frameAlphaAnimation = createAlphaAnimationAsync(view = buttonBackgroundFrame, alpha = 0.1f)

            tickAnimation?.await()
            frameAlphaAnimation?.await()
        }


        // Show progress bar
        progressDrawable?.stop()
        createAlphaAnimationAsync(view = buttonProgressBar, alpha = 1f, alphaDuration = 1L)?.await()
        progressDrawable?.start()

        // Small delay to make the progress bar a little bit longer
        delay(500)
    }

    private fun showSuccess(previousState: ButtonState, animated: Boolean) {
        animationJob = scope?.launch {
            animationJob?.join()

            if (animated) {
                showSuccessAnimated(previousState)
            } else {
                frameDrawable.cornerRadius = frameCircleCornerRadius
                buttonTickIcon.alpha = 1f
                buttonBackgroundFrame.alpha = 1f
                buttonDownloadIcon.alpha = 0f
                buttonProgressBar.alpha = 0f
                buttonTickIcon.setImageDrawable(context.getDrawableCompat(R.drawable.ic_tick_final))
            }
        }
    }

    private suspend fun showSuccessAnimated(previousState: ButtonState) {
        if (previousState is ButtonState.Progress) {
            val progressBarAnimation = createAlphaAnimationAsync(view = buttonProgressBar, alpha = 0f)
            val backgroundFrameAnimation = createAlphaAnimationAsync(view = buttonBackgroundFrame, alpha = 1f)

            progressBarAnimation?.await()
            backgroundFrameAnimation?.await()
        } else if (previousState is ButtonState.Download) {
            val backgroundCornersAnimation = createFrameCornersAnimationAsync(startCornerRadius = frameRectangleRadius, finalCornerRadius = frameCircleCornerRadius)
            val downloadIconAnimation = createAlphaAnimationAsync(view = buttonDownloadIcon, alpha = 0f)

            backgroundCornersAnimation?.await()
            downloadIconAnimation?.await()

            // Launch background alpha animation
            createAlphaAnimationAsync(view = buttonBackgroundFrame, alpha = 1f)?.await()
        }

        // After animation background, show tick button
        buttonTickIcon.setBackgroundResource(0)
        buttonTickIcon.setImageDrawable(context.getDrawableCompat(R.drawable.animated_tick))
        buttonTickIcon.alpha = 1f
        (buttonTickIcon.drawable as Animatable).start()
    }

    init {
        View.inflate(context, R.layout.animated_download_button, this)

        context.getStyledAttributes(attrs, R.styleable.AnimatedDownloadButton) {
            frameRectangleRadius = this.getDimensionPixelSize(R.styleable.AnimatedDownloadButton_frameCornerRadius, frameRectangleRadius.toInt()).toFloat()
            frameStrokeWidth = this.getDimensionPixelSize(R.styleable.AnimatedDownloadButton_frameStrokeWidth, frameStrokeWidth)
            animationDuration = this.getInteger(R.styleable.AnimatedDownloadButton_animationDuration, animationDuration.toInt()).toLong()
            downloadIconSize = this.getDimensionPixelSize(R.styleable.AnimatedDownloadButton_downloadIconSize, downloadIconSize)
            tickIconSize = this.getDimensionPixelSize(R.styleable.AnimatedDownloadButton_tickIconSize, tickIconSize)
            tintColor = this.getColor(R.styleable.AnimatedDownloadButton_tintColor, context.getColorCompat(R.color.colorAccent))
        }

        buttonDownloadIcon.layoutParams = LayoutParams(downloadIconSize, downloadIconSize, Gravity.CENTER)
        buttonDownloadIcon.setColorFilter(tintColor)

        buttonTickIcon.layoutParams = LayoutParams(tickIconSize, tickIconSize, Gravity.CENTER)
        buttonTickIcon.setColorFilter(tintColor)

        buttonBackgroundFrame.setColorFilter(tintColor)
        buttonBackgroundFrame.background = frameDrawable

        progressDrawable = CircularProgressDrawable.Builder(context)
            .color(tintColor)
            .strokeWidth(frameStrokeWidth.toFloat())
            .rotationSpeed(0.5f)
            .build()

        buttonProgressBar.indeterminateDrawable = progressDrawable

        setOnClickListener { onClick?.invoke(buttonState) }

        frameDrawable.cornerRadius = frameRectangleRadius
        buttonTickIcon.alpha = 0f
        buttonProgressBar.alpha = 0f
        buttonDownloadIcon.alpha = 1f
        buttonBackgroundFrame.alpha = 0.1f
    }

    sealed class ButtonState(val animated: Boolean) {
        class Download(animated: Boolean = false) : ButtonState(animated) {
            override fun equals(other: Any?) = other is Download
            override fun hashCode() = 0
        }

        class Progress(animated: Boolean = false) : ButtonState(animated) {
            override fun equals(other: Any?) = other is Progress
            override fun hashCode() = 1
        }

        class Success(animated: Boolean = false) : ButtonState(animated) {
            override fun equals(other: Any?) = other is Success
            override fun hashCode() = 2
        }
    }
}
