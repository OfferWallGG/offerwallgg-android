package gg.offerwall.sdk

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * The wall as a view, for apps that want it inside their own screen (a tab,
 * a bottom sheet). OfferwallActivity is this plus a bar and a close button.
 *
 *     val wall = OfferwallView(context)
 *     wall.load(OfferwallOptions(publicKey, userId))
 *
 * Call [onBackPressedInWall] from your back handling so the user steps back
 * through the wall before leaving it, and [release] when the view goes away.
 */
class OfferwallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    var listener: OfferwallListener? = null

    /** 0–100 while a page loads; the activity draws its bar from this. */
    var onProgress: ((Int) -> Unit)? = null

    private var wallHost: String = ""

    init {
        configure()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        settings.javaScriptEnabled = true          // live search, image fallbacks, Traffic Task timers
        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(false)  // a target=_blank link comes through shouldOverrideUrlLoading instead
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = settings.userAgentString + " OfferwallGG-Android/" + OfferwallGG.VERSION

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                route(request.url.toString())

            @Deprecated("Below API 24 only")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = route(url)

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                onProgress?.invoke(5)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    listener?.onError(error.description?.toString() ?: "The offerwall could not be loaded.")
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress?.invoke(newProgress)
            }
        }
    }

    /** Open the wall for one user. */
    fun load(options: OfferwallOptions) {
        wallHost = options.wallHost()
        loadUrl(options.wallUrl())
    }

    /** True when the back press was used to step back inside the wall. */
    fun onBackPressedInWall(): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        return false
    }

    /** Free the WebView. Call from onDestroy. */
    fun release() {
        stopLoading()
        listener = null
        onProgress = null
        destroy()
    }

    /** True when the SDK handled the link (opened it elsewhere, or refused it). */
    private fun route(url: String): Boolean =
        when (LinkPolicy.decide(url, wallHost)) {
            LinkPolicy.Decision.STAY -> false
            LinkPolicy.Decision.BLOCK -> true
            LinkPolicy.Decision.EXTERNAL -> {
                openExternally(url)
                true
            }
        }

    private fun openExternally(url: String) {
        val intent = if (url.startsWith("intent:", ignoreCase = true)) {
            safeIntent(url)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } ?: return

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            listener?.onOfferStarted(url)
        } catch (e: ActivityNotFoundException) {
            // An intent: link to an app that is not installed carries the
            // store or web address to use instead.
            val fallback = intentFallback(url)
            if (fallback != null) {
                openExternally(fallback)
            } else {
                listener?.onError("No app can open this offer.")
            }
        }
    }

    /**
     * An `intent:` link from a web page may only VIEW something. Whatever
     * component, selector or flags the page wrote are removed — a page must
     * not be able to start an arbitrary activity inside the host app.
     */
    private fun safeIntent(url: String): Intent? =
        try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                if (action != Intent.ACTION_VIEW) {
                    return null
                }
                component = null
                selector = null
                flags = 0
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        } catch (e: Exception) {
            null
        }

    private fun intentFallback(url: String): String? =
        try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                .getStringExtra("browser_fallback_url")
                ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
        } catch (e: Exception) {
            null
        }
}
