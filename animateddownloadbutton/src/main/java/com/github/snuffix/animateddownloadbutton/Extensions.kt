package com.github.snuffix.animateddownloadbutton

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewPropertyAnimator
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import kotlinx.coroutines.suspendCancellableCoroutine

fun Context.getDrawableCompat(resourceId: Int) = ContextCompat.getDrawable(this, resourceId)
fun Context.getColorCompat(resourceId: Int) = ContextCompat.getColor(this, resourceId)

fun View.setGone() {
    visibility = View.GONE
}

@SuppressLint("Recycle")
fun Context.getStyledAttributes(
    attributeSet: AttributeSet?,
    styleArray: IntArray,
    block: TypedArray.() -> Unit
) =
    this.obtainStyledAttributes(attributeSet, styleArray, 0, 0).use(block)


fun View.setVisibleOrGone(isVisible: Boolean) {
    if (isVisible) {
        setVisible()
    } else {
        setGone()
    }
}

fun View.setVisible() {
    visibility = View.VISIBLE
}

fun View.setInvisible() {
    visibility = View.INVISIBLE
}

val screenDensity: Float
    get() = Resources.getSystem().displayMetrics.density

val Activity.screenWidth: Int
    get()  {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

val Activity.screenHeight: Int
    get()  {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

val Int.dp: Int
    get() = (this * screenDensity).toInt()


suspend fun ViewPropertyAnimator.awaitEnd() = suspendCancellableCoroutine<Unit> { cont ->
    cont.invokeOnCancellation { cancel() }

    this.setListener(object: AnimatorListenerAdapter() {
        private var endedSuccessfully = true

        override fun onAnimationCancel(animation: Animator) {
            endedSuccessfully = false
        }

        override fun onAnimationEnd(animation: Animator) {
            animation.removeListener(this)

            if (cont.isActive) {
                if (endedSuccessfully) {
                    cont.resume(Unit) { this@awaitEnd.cancel() }
                } else {
                    cont.cancel()
                }
            }
        }
    })
}

suspend fun Animator.awaitEnd() = suspendCancellableCoroutine<Unit> { cont ->
    // Add an invokeOnCancellation listener. If the coroutine is
    // cancelled, cancel the animation too that will notify
    // listener's onAnimationCancel() function
    cont.invokeOnCancellation { cancel() }

    addListener(object : AnimatorListenerAdapter() {
        private var endedSuccessfully = true

        override fun onAnimationCancel(animation: Animator) {
            // Animator has been cancelled, so flip the success flag
            endedSuccessfully = false
        }

        override fun onAnimationEnd(animation: Animator) {
            // Make sure we remove the listener so we don't keep
            // leak the coroutine continuation
            animation.removeListener(this)

            if (cont.isActive) {
                // If the coroutine is still active...
                if (endedSuccessfully) {
                    // ...and the Animator ended successfully, resume the coroutine
                    cont.resume(Unit) { this@awaitEnd.cancel() }
                } else {
                    // ...and the Animator was cancelled, cancel the coroutine too
                    cont.cancel()
                }
            }
        }
    })
}
