package com.deeprows.footbolive

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splash: View

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private var adDialog: Dialog? = null
    private var adWebView: WebView? = null

    private val homeUrl =
        "https://deeprows.github.io/Footbolive/index.html"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splash = findViewById(R.id.splash)

        findViewById<ImageView>(R.id.splashLogo)
            .setImageResource(R.drawable.splash_logo)

        configureMainWebView()

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun configureMainWebView() {
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)

        settings.allowFileAccess = false
        settings.allowContentAccess = true

        // Better screen fitting.
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // Avoid forcing the desktop version of the site.
        settings.userAgentString = settings.userAgentString
            .replace("; wv", "")

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return false
            }

            override fun onPageFinished(
                view: WebView,
                url: String
            ) {
                super.onPageFinished(view, url)

                splash.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction {
                        splash.visibility = View.GONE
                    }
                    .start()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                // Never stack advertisement popups.
                if (adDialog?.isShowing == true) {
                    return false
                }

                val popup = WebView(this@MainActivity)

                configurePopupWebView(popup)

                val transport =
                    resultMsg.obj as WebView.WebViewTransport

                transport.webView = popup
                resultMsg.sendToTarget()

                return true
            }

            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback
            ) {
                if (fullscreenView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                fullscreenView = view
                fullscreenCallback = callback

                val decor =
                    window.decorView as android.view.ViewGroup

                decor.addView(
                    view,
                    android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                hideSystemBars()
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }
    }

    private fun configurePopupWebView(
        popupWebView: WebView
    ) {
        adWebView = popupWebView

        val settings = popupWebView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        settings.mediaPlaybackRequiresUserGesture = false

        // Critical: advertisements cannot create another popup.
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        settings.allowFileAccess = false
        settings.allowContentAccess = true

        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(popupWebView, true)

        popupWebView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Keep every ad navigation inside the temporary popup.
                return false
            }
        }

        popupWebView.webChromeClient = object : WebChromeClient() {

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                // Block nested ad popups.
                return false
            }
        }

        val dialog = Dialog(this)
        adDialog = dialog

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_webview)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.black
        )

        val oldWebView =
            dialog.findViewById<WebView>(R.id.popupWebView)

        val parent =
            oldWebView.parent as android.view.ViewGroup

        val index = parent.indexOfChild(oldWebView)

        parent.removeView(oldWebView)

        parent.addView(
            popupWebView,
            index,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        dialog.findViewById<TextView>(R.id.closeButton)
            .setOnClickListener {
                closeAdPopup()
            }

        dialog.setOnKeyListener { _, keyCode, event ->
            if (
                keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_UP
            ) {
                closeAdPopup()
                true
            } else {
                false
            }
        }

        dialog.setOnDismissListener {
            cleanupPopupWebView()
        }

        dialog.show()

        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    private fun closeAdPopup() {
        val dialog = adDialog

        if (dialog?.isShowing == true) {
            dialog.dismiss()
        } else {
            cleanupPopupWebView()
        }
    }

    private fun cleanupPopupWebView() {
        val popup = adWebView

        adWebView = null
        adDialog = null

        popup?.let {
            it.stopLoading()
            it.loadUrl("about:blank")
            it.clearHistory()
            it.clearCache(false)
            it.removeAllViews()
            it.destroy()
        }

        // Return focus to the actual Footbolive page.
        webView.requestFocus()
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {

            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())

                it.systemBarsBehavior =
                    WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun showSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {

            window.insetsController?.show(
                WindowInsets.Type.systemBars()
            )

        } else {

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun exitFullscreen() {
        fullscreenView?.let {
            (it.parent as? android.view.ViewGroup)
                ?.removeView(it)
        }

        fullscreenView = null

        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        showSystemBars()
    }

    @Deprecated("Use OnBackInvokedDispatcher on newer Android versions")
    override fun onBackPressed() {

        // 1. Close ad immediately.
        if (adDialog?.isShowing == true) {
            closeAdPopup()
            return
        }

        // 2. Exit fullscreen video.
        if (fullscreenView != null) {
            exitFullscreen()
            return
        }

        // 3. Go back through the actual website history.
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }

        // 4. Exit only when the website has no history.
        super.onBackPressed()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        if (adDialog?.isShowing == true) {
            adDialog?.dismiss()
        }

        cleanupPopupWebView()

        if (fullscreenView != null) {
            exitFullscreen()
        }

        webView.destroy()

        super.onDestroy()
    }
}
