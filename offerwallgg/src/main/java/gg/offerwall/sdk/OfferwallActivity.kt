package gg.offerwall.sdk

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/**
 * The full-screen wall: a bar with a title and a close button, a thin
 * progress line, and [OfferwallView] under it. Built in code rather than XML
 * so the SDK ships no resources that could clash with the host app's.
 *
 * Start it with [OfferwallGG.show]; it is not exported.
 */
class OfferwallActivity : Activity() {

    private lateinit var wall: OfferwallView
    private var backCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val options = intent.getSerializableExtra(EXTRA_OPTIONS) as? OfferwallOptions
        if (options == null) {
            finish()
            return
        }

        val ink = Color.parseColor("#0E100F")
        window.statusBarColor = ink

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(4), 0)
            setBackgroundColor(ink)
        }
        val title = TextView(this).apply {
            text = options.title ?: "Offers"
            setTextColor(Color.parseColor("#EEF1EE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            paint.isFakeBoldText = true
            maxLines = 1
        }
        val close = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Close"
            setOnClickListener { finish() }
        }
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        wall = OfferwallView(this).apply {
            listener = OfferwallGG.listener
            onProgress = { value ->
                progress.progress = value
                progress.visibility = if (value in 1..99) View.VISIBLE else View.GONE
            }
        }

        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        root.addView(
            FrameLayout(this).apply { addView(wall, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        setContentView(root)

        if (savedInstanceState != null) {
            wall.restoreState(savedInstanceState)
        } else {
            wall.load(options)
        }

        // Android 13+: predictive back steps through the wall first.
        if (Build.VERSION.SDK_INT >= 33) {
            val callback = OnBackInvokedCallback {
                if (!wall.onBackPressedInWall()) {
                    finish()
                }
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            backCallback = callback
        }

        OfferwallGG.listener?.onOpened()
    }

    @Deprecated("Below Android 13; newer versions use the OnBackInvokedCallback above")
    override fun onBackPressed() {
        if (!wall.onBackPressedInWall()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::wall.isInitialized) {
            wall.saveState(outState)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 33) {
            (backCallback as? OnBackInvokedCallback)?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        if (::wall.isInitialized) {
            wall.release()
            OfferwallGG.listener?.onClosed()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    companion object {
        const val EXTRA_OPTIONS = "gg.offerwall.sdk.OPTIONS"
    }
}
