package com.github.snuffix.animateddownloadbutton.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.snuffix.animateddownloadbutton.AnimatedDownloadButton
import com.github.snuffix.animateddownloadbutton.R
import com.github.snuffix.animateddownloadbutton.getDrawableCompat
import com.github.snuffix.animateddownloadbutton.sample.expandable_fab.ButtonType
import kotlinx.android.synthetic.main.activity_main.*


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

        expandable_fab.scope = lifecycleScope
        expandable_fab.text = getString(R.string.quick_actions)
        expandable_fab.icon = getDrawableCompat(R.drawable.ic_add_black_24dp)
    }
}
