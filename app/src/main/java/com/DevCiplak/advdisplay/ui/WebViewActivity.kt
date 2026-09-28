package com.DevCiplak.advdisplay.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.DevCiplak.advdisplay.R
import com.DevCiplak.advdisplay.constant.Constant
import com.DevCiplak.advdisplay.security.WebNavigationPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WebViewActivity : PlaybackActivity() {
    private lateinit var web: WebView
    private lateinit var displayUrl: String
    private var failed = false
    private val policy = WebNavigationPolicy(Constant.BASE_URL)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!session.isValid()) return
        setContentView(R.layout.activity_web_view)
        displayUrl = "${Constant.BASE_URL}display".toUri().buildUpon()
            .appendQueryParameter("c", session.code).appendQueryParameter("u", session.deviceId).build().toString()
        web = findViewById(R.id.webView)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, false)
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !policy.allows(request.url.toString())

            @Deprecated("Required for Android 5 and 6")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !policy.allows(url)

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!policy.allows(url)) {
                    view.stopLoading()
                    showError(getString(R.string.web_blocked))
                    return
                }
                failed = false
                findViewById<View>(R.id.loadingBack).visibility = View.GONE
                findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                if (!failed) findViewById<View>(R.id.loadingBack).visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError(getString(R.string.web_load_error))
            }

            @Deprecated("Required for Android 5")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) showError(getString(R.string.web_load_error))
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) showError(getString(R.string.web_load_error))
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                showError(getString(R.string.web_certificate_error))
            }
        }
        findViewById<Button>(R.id.okBtn2).apply {
            setText(R.string.retry)
            setOnClickListener { loadDisplay() }
        }
        loadDisplay()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(30_000)
                    if (failed && isOnline()) loadDisplay()
                }
            }
        }
    }

    private fun loadDisplay() {
        failed = false
        findViewById<View>(R.id.loadingBack).visibility = View.GONE
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
        web.loadUrl(displayUrl)
    }

    private fun showError(message: String) {
        failed = true
        findViewById<View>(R.id.progressBar).visibility = View.GONE
        findViewById<View>(R.id.loadingBack).visibility = View.VISIBLE
        findViewById<TextView>(R.id.messageTitle).text = message
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
    }

    override fun onPause() {
        if (::web.isInitialized) web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
        }
        super.onDestroy()
    }
}
