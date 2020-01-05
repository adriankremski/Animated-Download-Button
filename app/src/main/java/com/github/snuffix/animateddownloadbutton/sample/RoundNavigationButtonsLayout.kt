package com.github.snuffix.animateddownloadbutton.sample

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.*
import android.view.animation.Animation.RELATIVE_TO_PARENT
import android.view.animation.Animation.RELATIVE_TO_SELF
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import android.animation.ObjectAnimator
import android.widget.LinearLayout
import com.github.snuffix.animateddownloadbutton.R
import com.github.snuffix.animateddownloadbutton.setVisible
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.delay


@SuppressLint("ViewConstructor")
class RoundNavigationButtonsLayout @JvmOverloads constructor(
    private var arcRadius: Float = 230f,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val arcStartAngles = listOf(180f, 230f, 280f)
    private var buttonSize = (arcRadius / 2).toInt()
    private val buttonPadding = 60
    private val buttonImageResources = listOf(
        R.drawable.ic_donut_large_black_24dp,
        R.drawable.ic_whatshot_24dp, R.drawable.ic_favorite_24dp
    )

    private var animator: ValueAnimator? = null
    private val animationDuration = 250.toLong()

    private var buttonClickListener: ((Int) -> Unit)? = null


    init {
        View.inflate(context, R.layout.layout_quick_actions, this)
    }

//    fun initButtons() {
//        afterMeasured {
//            buttonSize = (arcRadius * 7 / 12).toInt()
//
//            arcStartAngles.forEachIndexed { index, angle ->
//                addButtonImageView(index, angle)
//            }
//        }
//    }
//
//    fun setArcRadius(value: Float) {
//        arcRadius = value
//        removeAllViews()
//        initButtons()
//        invalidate()
//    }

//    private fun addButtonImageView(buttonNumber: Int, startAngle: Float) {
//        val layout = LinearLayout(context)
//        layout.orientation = LinearLayout.VERTICAL
//
//        val buttonImageView = ImageView(context).apply {
//            setOnClickListener { buttonClickListener?.invoke(buttonNumber) }
//            setImageResource(buttonImageResources[buttonNumber])
//
//            setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding)
//        }
//
//        layout.addView(buttonImageView)
//
//        val textView = TextView(context)
//        textView.text = "HEY THERE"
//        layout.addView(textView)
//
//        addView(layout, LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
//
//        layout.afterMeasured {
//            layout.layoutParams = createButtonParams(layout, startAngle)
//            layout.requestLayout()
//        }
//    }
//
//    private fun createButtonParams(view: View, startAngle: Float): LayoutParams {
//        val params = LayoutParams(view.width, view.height)
//        val center = calculateButtonCenter(startAngle)
//        params.leftMargin = center.x - view.width / 2
//        params.topMargin = center.y - view.height / 2
//        return params
//    }
//
//    private fun calculateButtonCenter(startAngle: Float): Point {
//        val angleInRadians = Math.toRadians((startAngle).toDouble())
//        val centerX = width / 2 + ((width / 2 - arcRadius / 2) * cos(angleInRadians))
//        val centerY = width / 2 + ((width / 2 - arcRadius / 2) * sin(angleInRadians))
//        return Point(centerX.toInt(), centerY.toInt())
//    }

    fun setOnButtonClickListener(block: (Int) -> Unit) {
    }

    fun animateButtons(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            translateFirstButton()
            delay(animationDuration / 2)
            translateSecondButton()
            delay(animationDuration / 2)
            translateThirdButton()
        }
    }

    private fun translateFirstButton() {
        val firstButton = getChildViews<LinearLayout>()[0]
        firstButton.setVisible()
        firstButton.translateX(startTranslation = (width / 2 - firstButton.width / 2).toFloat(), endTranslation = 0f)
        firstButton.translateY(startTranslation = height.toFloat(), endTranslation = firstButton.y)
        startScaleAnimation(firstButton)
    }

    private fun View.translateX(startTranslation: Float, endTranslation: Float) {
        translationX = startTranslation
        animate().translationX(endTranslation).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = animationDuration
            start()
        }
    }

    private fun View.translateY(startTranslation: Float, endTranslation: Float) {
        translationY = startTranslation
        animate().translationY(endTranslation).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = animationDuration
            start()
        }
    }

    private fun translateSecondButton() {
        val secondButton = getChildViews<LinearLayout>()[1]
        secondButton.setVisible()
        secondButton.translateY(startTranslation = height.toFloat(), endTranslation = secondButton.y)
        startScaleAnimation(secondButton)
    }

    private fun translateThirdButton() {
        val thirdButton = getChildViews<LinearLayout>()[2]
        thirdButton.setVisible()
        thirdButton.translateX(startTranslation = -(thirdButton.x - width/2 + thirdButton.width/2), endTranslation = 0f)
        thirdButton.translateY(startTranslation = height.toFloat(), endTranslation = thirdButton.y)
        startScaleAnimation(thirdButton)
    }

    private fun startScaleAnimation(view: View, startScale: Float = 0f, endScale: Float = 1f) {
        view.scaleX = startScale
        view.scaleY = startScale

        val scaleXAnimation = ObjectAnimator.ofFloat(view, "scaleX", endScale)
        scaleXAnimation.duration = animationDuration
        scaleXAnimation.interpolator = AccelerateDecelerateInterpolator()
        scaleXAnimation.start()

        val scaleYAnimation = ObjectAnimator.ofFloat(view, "scaleY", endScale)
        scaleYAnimation.duration = animationDuration
        scaleYAnimation.interpolator = AccelerateDecelerateInterpolator()
        scaleYAnimation.start()
    }

    fun hideButtons(lifecycleScope: LifecycleCoroutineScope, block: () -> Unit) {
        lifecycleScope.launch {

            val thirdButton = getChildViews<LinearLayout>()[2]
            thirdButton.setVisible()
            thirdButton.translateX(endTranslation = -(thirdButton.x - width/2 + thirdButton.width/2), startTranslation = 0f)
            thirdButton.translateY(endTranslation = height.toFloat(), startTranslation = thirdButton.y)

            startScaleAnimation(thirdButton, startScale = 1f, endScale = 0f)

            delay(animationDuration / 2)

            val secondButton = getChildViews<LinearLayout>()[1]
            secondButton.setVisible()
            secondButton.translateY(endTranslation = height.toFloat(), startTranslation = secondButton.y)
            startScaleAnimation(secondButton, startScale = 1f, endScale = 0f)

            delay(animationDuration / 2)

            val firstButton = getChildViews<LinearLayout>()[0]
            firstButton.setVisible()
            firstButton.translateX(endTranslation = (width / 2 - firstButton.width / 2).toFloat(), startTranslation = 0f)
            firstButton.translateY(endTranslation = height.toFloat(), startTranslation = firstButton.y)

            startScaleAnimation(firstButton, startScale = 1f, endScale = 0f)

            delay(animationDuration)

            block()
        }
    }
}

fun View.afterMeasured(block: View.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            block()
        }
    })
}

inline fun <reified T> ViewGroup.getChildViews(): List<T> {
    val views = mutableListOf<T>()

    for (i in 0 until childCount) {
        val view = getChildAt(i)

        if (view is T) {
            views.add(getChildAt(i) as T)
        }
    }

    return views
}
