package ai.arena.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar

/**
 * Arena AI — aplicativo wrapper (WebView) para https://arena.ai/
 */
class MainActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var errorView: View
    private lateinit var errorTitle: TextView
    private lateinit var errorMessage: TextView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            filePathCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
            filePathCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: a página ocupa a tela inteira
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        webView = findViewById(R.id.webview)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)
        errorView = findViewById(R.id.error_view)
        errorTitle = findViewById(R.id.error_title)
        errorMessage = findViewById(R.id.error_message)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupWebView()

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Estado restaurado após recriação da atividade
        } else {
            webView.loadUrl(HOME_URL)
        }

        findViewById<Button>(R.id.btn_retry).setOnClickListener {
            errorView.visibility = View.GONE
            if (webView.url.isNullOrEmpty()) webView.loadUrl(HOME_URL) else webView.reload()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // UA de Chrome estável — evita o bloqueio de login OAuth em WebViews
            userAgentString = CHROME_UA
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = ArenaWebViewClient()
        webView.webChromeClient = ArenaChromeClient()

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    setDescription(getString(R.string.downloading))
                    setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Snackbar.make(swipe, R.string.download_started, Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                openExternally(url)
            }
        }

        swipe.setOnRefreshListener { webView.reload() }
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            swipe.isEnabled = !webView.canScrollVertically(-1)
        }
    }

    private inner class ArenaWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme?.lowercase() ?: return false
            return if (scheme == "http" || scheme == "https") {
                false // continua dentro do app
            } else {
                openExternally(request.url.toString())
                true
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            errorView.visibility = View.GONE
            progress.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            swipe.isRefreshing = false
            progress.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (!request.isForMainFrame) return
            progress.visibility = View.GONE
            swipe.isRefreshing = false
            errorTitle.setText(R.string.error_title)
            errorMessage.setText(
                when (error.errorCode) {
                    ERROR_HOST_LOOKUP, ERROR_CONNECT, ERROR_TIMEOUT -> R.string.error_offline
                    else -> R.string.error_generic
                }
            )
            errorView.visibility = View.VISIBLE
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // Recuperação caso o renderer do WebView reinicie
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
            recreate()
            return true
        }
    }

    private inner class ArenaChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress.progress = newProgress
            progress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            this@MainActivity.filePathCallback?.onReceiveValue(null)
            this@MainActivity.filePathCallback = filePathCallback
            val intent = try {
                fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
            } catch (e: ActivityNotFoundException) {
                this@MainActivity.filePathCallback = null
                return false
            }
            return try {
                fileChooserLauncher.launch(intent)
                true
            } catch (e: ActivityNotFoundException) {
                this@MainActivity.filePathCallback = null
                false
            }
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            request ?: return
            val host = request.origin?.host ?: ""
            val wantsMedia = request.resources.any {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                    it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }
            if (wantsMedia && (host == "arena.ai" || host.endsWith(".arena.ai"))) {
                runOnUiThread { request.grant(request.resources) }
            } else {
                runOnUiThread { request.deny() }
            }
        }
    }

    private fun openExternally(url: String) {
        try {
            val intent = if (url.startsWith("intent://")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.link_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val HOME_URL = "https://arena.ai/"
        private const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
