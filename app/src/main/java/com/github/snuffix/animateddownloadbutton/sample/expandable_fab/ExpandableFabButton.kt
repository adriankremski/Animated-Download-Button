package com.github.snuffix.animateddownloadbutton.sample.expandable_fab

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.view.doOnLayout
import com.github.snuffix.animateddownloadbutton.R
import com.github.snuffix.animateddownloadbutton.dp
import com.github.snuffix.animateddownloadbutton.getColorCompat
import com.github.snuffix.animateddownloadbutton.getStyledAttributes
import com.google.android.material.button.MaterialButton
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.layout_quick_actions.view.*
import kotlinx.coroutines.CoroutineScope

class ExpandableFabButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    private val animator: FabExtensionAnimator by lazy {
        FabExtensionAnimator(this)
    }

    private val screenWidth: Int
        get() {
            val displayMetrics = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }

    private val screenHeight: Int
        get() {
            val displayMetrics = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.heightPixels
        }

    var buttonClickListener: ((ButtonType) -> Unit)? = null
    lateinit var scope: CoroutineScope

    private var leftButtonText : Int = -1
    private var centerButtonText : Int = -1
    private var rightButtonText : Int = -1

    private var leftButtonIcon : Int = -1
    private var centerButtonIcon : Int = -1
    private var rightButtonIcon : Int = -1

    init {
        context.getStyledAttributes(attrs, R.styleable.ExpandableFabButton) {
            leftButtonText = this.getResourceId(R.styleable.ExpandableFabButton_leftButtonText, -1)
            centerButtonText = this.getResourceId(R.styleable.ExpandableFabButton_centerButtonText, -1)
            rightButtonText = this.getResourceId(R.styleable.ExpandableFabButton_rightButtonText, -1)

            leftButtonIcon = this.getResourceId(R.styleable.ExpandableFabButton_leftButtonIcon, -1)
            centerButtonIcon = this.getResourceId(R.styleable.ExpandableFabButton_centerButtonIcon, -1)
            rightButtonIcon = this.getResourceId(R.styleable.ExpandableFabButton_rightButtonIcon, -1)
        }

        animator.animationEndListener = { isExtended ->
            this.isClickable = isExtended

            if (!isExtended) {
                post {
                    val frameLayout = CollapsedFabButtonsLayout(context = context)
                    frameLayout.buttonsContainer.minimumWidth = screenWidth
                    frameLayout.buttonsContainer.minimumHeight = screenHeight / 3

                    frameLayout.setupButton(ButtonType.LEFT, leftButtonText, leftButtonIcon)
                    frameLayout.setupButton(ButtonType.CENTER, centerButtonText, centerButtonIcon)
                    frameLayout.setupButton(ButtonType.RIGHT, rightButtonText, rightButtonIcon)

                    frameLayout.setOnButtonClickListener {
                        buttonClickListener?.invoke(it)
                    }
                    frameLayout.popOver(
                        anchor = this,
                        viewLocationOptions = {
                            Point(-this.x.toInt(), -(screenHeight / 3 + 32.dp))
                        },
                        popupOptions = {
                            contentView.alpha = 0f
                            contentView.setOnTouchListener { _, _ ->
                                contentView.animate().alpha(0f).apply {
                                    duration = 300L
                                    start()
                                }

                                frameLayout.hideButtons(scope) {
                                    animator.isExtended = true
                                    dismiss()
                                }

                                true
                            }

                            contentView.animate().alpha(1f).apply {
                                duration = 300L
                                start()
                            }

                            frameLayout.afterMeasured {
                                frameLayout.animateButtons(scope) {
                                }
                            }

                            frameLayout.setOnButtonClickListener { button ->
                                frameLayout.hideButtons(scope) {
                                    animator.isExtended = true
                                    dismiss()
                                }

                                buttonClickListener?.invoke(button)
                            }
                        })
                }
            }
        }

        setOnClickListener {
            this.isClickable = false
            animator.isExtended = !animator.isExtended
        }
    }
}

fun View.popOver(
    anchor: View,
    viewLocationOptions: () -> Point = { Point(0, 0) },
    popupOptions: PopupWindow.() -> Unit = {}
) {
    require(!this.isAttachedToWindow) { "The View being attached must be an orphan" }

    this.alignToAnchor(anchor, viewLocationOptions)
    val wrappedView = this.wrapWithBlurView()

    PopupWindow(wrappedView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true).run {
        isOutsideTouchable = true
        popupOptions(this)
        showAtLocation(anchor, Gravity.START, 0, 0)
    }
}

private fun View.wrapWithBlurView(): View? {
    val activity = context as Activity
    val blurView = BlurView(context).apply {
        this.setBlurEnabled(true)

        val radius = 5f

        val decorView = activity.getWindow().getDecorView()
        //ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
        val rootView = decorView.findViewById(android.R.id.content) as ViewGroup
        //Set drawable to draw in the beginning of each blurred frame (Optional).
        //Can be used in case your layout has a lot of transparent space and your content
        //gets kinda lost after after blur is applied.
        val windowBackground = decorView.getBackground()

        setupWith(rootView)
            .setFrameClearDrawable(windowBackground)
            .setOverlayColor(context.getColorCompat(com.github.snuffix.animateddownloadbutton.R.color.overlay_background_color))
            .setBlurAlgorithm(RenderScriptBlur(context))
            .setBlurRadius(radius)
            .setHasFixedTransformationMatrix(true)

    }

    blurView.addView(this@wrapWithBlurView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    return blurView
}

private fun View.alignToAnchor(anchor: View, adjuster: () -> Point) = intArrayOf(0, 0).run {
    anchor.getLocationInWindow(this)
    doOnLayout {
        val (offsetX, offsetY) = adjuster()
        val x = this[0].toFloat() + offsetX
        val y = this[1].toFloat() + offsetY
        translationX = x; translationY = y
    }
}
