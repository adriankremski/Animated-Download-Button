package com.github.snuffix.animateddownloadbutton.sample

import android.graphics.Color
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.github.snuffix.animateddownloadbutton.*
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.drawable.Drawable



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadButton.scope = lifecycleScope
        downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Download()

        resetStateAnimated.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Download(animated = true) }
        showProgressAnimated.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Progress(animated = true) }
        showSuccessAnimated.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Success(animated = true) }

        resetState.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Download(animated = false) }
        showProgress.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Progress(animated = false) }
        showSuccess.setOnClickListener { downloadButton.buttonState = AnimatedDownloadButton.ButtonState.Success(animated = false) }

        quickActions.setOnClickListener {

        }

        animator.updateGlyphs(R.string.quick_actions, R.drawable.ic_add_black_24dp)
        animator.animationEndListener = {isExtended ->
            if (!isExtended) {
                expandable_fab.post {
                    val frameLayout = RoundNavigationButtonsLayout(arcRadius = 230f, context = this)
                    frameLayout.minimumWidth = screenWidth
                    frameLayout.minimumHeight = screenHeight / 3
                    frameLayout.popOver(this@MainActivity, expandable_fab,
                        adjuster = {
                            Point(-expandable_fab.x.toInt(), -(frameLayout.height))
                        },
                        options = {
                            contentView.alpha = 0f
                            contentView.setOnTouchListener { _, _ ->
                                contentView.animate().alpha(0f).apply {
                                    duration = 300L
                                    start()
                                }

                                frameLayout.hideButtons(lifecycleScope) {
                                    animator.isExtended = true
                                    dismiss()
                                }

                                true
                            }

                            frameLayout.afterMeasured {
                                contentView.animate().alpha(1f).apply {
                                    duration = 300L
                                    start()
                                }
                                frameLayout.animateButtons(lifecycleScope)
                            }
                        })
                }
            }
        }
        expandable_fab.setOnClickListener { animator.isExtended = !animator.isExtended }
    }

    private val animator: FabExtensionAnimator by lazy {
        FabExtensionAnimator(expandable_fab)
    }
}


fun View.popOver(
    activity: AppCompatActivity,
    anchor: View,
    adjuster: () -> Point = { Point(0, 0) },
    options: PopupWindow.() -> Unit = {}
) {
    require(!this.isAttachedToWindow) { "The View being attached must be an orphan" }
    PopupWindow(this.wrapAtAnchor(activity, anchor, adjuster), MATCH_PARENT, MATCH_PARENT, true).run {
        isOutsideTouchable = true
        options(this)
        showAtLocation(anchor, Gravity.START, 0, 0)
    }
}

private fun View.wrapAtAnchor(
    activity: AppCompatActivity,
    anchor: View, adjuster: () -> Point): View? = BlurView(anchor.context).apply {
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
        .setOverlayColor(anchor.context.getColorCompat(R.color.overlay_background_color))
        .setBlurAlgorithm(RenderScriptBlur(anchor.context))
        .setBlurRadius(radius)
        .setHasFixedTransformationMatrix(true)

    this@wrapAtAnchor.alignToAnchor(anchor, adjuster)

    addView(this@wrapAtAnchor, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
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