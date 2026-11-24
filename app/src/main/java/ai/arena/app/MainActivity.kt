package ai.arena.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
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
import android.webkit.WebStorage
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
import java.util.Locale

/**
 * Arena AI — aplicativo wrapper (WebView) para https://arena.ai/
 *
 * Modos:
 *  - [MODE_GUEST]: anônimo (nada é salvo), com sessões limitadas de tempo
 *  - [MODE_LOGIN]: fluxo de login (persistência normal + confirmação)
 *  - [MODE_NORMAL]: uso normal/ilimitado (após login)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var errorView: View
    private lateinit var errorTitle: TextView
    private lateinit var errorMessage: TextView

    private lateinit var guestBanner: View
    private lateinit var timerText: TextView
    private lateinit var loginPendingBar: View
    private lateinit var loginPrompt: View
    private lateinit var loginPromptTitle: TextView
    private lateinit var loginPromptMessage: TextView
    private lateinit var btnAnotherSession: Button

    private lateinit var guest: GuestSessionManager
    private var mode: String = MODE_NORMAL
    private var pendingLogin = false
    private var countDownTimer: CountDownTimer? = null
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        guest = GuestSessionManager(this)

        mode = intent.getStringExtra(EXTRA_MODE) ?: when {
            guest.isLoggedIn -> MODE_NORMAL
            guest.canStartAnonymousSession -> MODE_GUEST
            else -> MODE_LOGIN
        }

        bindViews()
        setupInsets()
        setupWebView()

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // Estado restaurado após recriação da atividade
        } else {
            val target = intent?.dataString
                ?: if (mode == MODE_LOGIN) LOGIN_URL else HOME_URL
            webView.loadUrl(target)
        }

        when (mode) {
            MODE_GUEST -> startGuestSession()
            MODE_LOGIN -> showLoginPending()
        }

        findViewById<Button>(R.id.btn_retry).setOnClickListener {
            errorView.visibility = View.GONE
            if (webView.url.isNullOrEmpty()) webView.loadUrl(HOME_URL) else webView.reload()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (loginPrompt.visibility == View.VISIBLE) return // modal: escolha uma opção
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun bindViews() {
        root = findViewById(R.id.root)
        webView = findViewById(R.id.webview)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)
        errorView = findViewById(R.id.error_view)
        errorTitle = findViewById(R.id.error_title)
        errorMessage = findViewById(R.id.error_message)
        guestBanner = findViewById(R.id.guest_banner)
        timerText = findViewById(R.id.timer_text)
        loginPendingBar = findViewById(R.id.login_pending_bar)
        loginPrompt = findViewById(R.id.login_prompt)
        loginPromptTitle = findViewById(R.id.login_prompt_title)
        loginPromptMessage = findViewById(R.id.login_prompt_message)
        btnAnotherSession = findViewById(R.id.btn_another_session)

        findViewById<Button>(R.id.btn_logout_guest).setOnClickListener { exitToWelcome() }
        findViewById<Button>(R.id.btn_done_login).setOnClickListener { confirmLogin() }
        findViewById<Button>(R.id.btn_login_unlimited).setOnClickListener { switchToLoginFlow() }
        btnAnotherSession.setOnClickListener {
            loginPrompt.visibility = View.GONE
            startGuestSession()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    // ------------------------------------------------------------------
    // Modo convidado (anônimo) com limite
    // ------------------------------------------------------------------

    private fun startGuestSession() {
        if (!guest.startAnonymousSession()) {
            onSessionExpired()
            return
        }
        wipeGuestData() // cada sessão começa limpa
        guestBanner.visibility = View.VISIBLE
        loginPrompt.visibility = View.GONE
        startCountdown(GuestSessionManager.sessionDurationMs())
    }

    private fun startCountdown(durationMs: Long) {
        countDownTimer?.cancel()
        updateTimerText(durationMs) // estado inicial imediato
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(msLeft: Long) = updateTimerText(msLeft)

            override fun onFinish() = onSessionExpired()
        }.start()
    }

    private fun updateTimerText(msLeft: Long) {
        val s = msLeft / 1000
        timerText.text = getString(
            R.string.guest_time_left,
            String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60)
        )
    }

    internal fun onSessionExpired() {
        countDownTimer?.cancel()
        val remaining = guest.sessionsRemaining
        loginPromptTitle.setText(R.string.session_expired_title)
        loginPromptMessage.setText(
            if (remaining > 0) {
                getString(
                    R.string.session_expired_have_more,
                    remaining,
                    GuestSessionManager.ANON_SESSION_MINUTES
                )
            } else {
                getString(R.string.session_expired_none, GuestSessionManager.ANON_SESSION_LIMIT)
            }
        )
        btnAnotherSession.visibility = if (remaining > 0) View.VISIBLE else View.GONE
        if (remaining > 0) {
            btnAnotherSession.text = getString(R.string.btn_use_another_session, remaining)
        }
        loginPrompt.visibility = View.VISIBLE
    }

    private fun switchToLoginFlow() {
        countDownTimer?.cancel()
        wipeGuestData()
        guestBanner.visibility = View.GONE
        loginPrompt.visibility = View.GONE
        mode = MODE_LOGIN
        showLoginPending()
        webView.loadUrl(LOGIN_URL)
    }

    private fun showLoginPending() {
        pendingLogin = true
        loginPendingBar.visibility = View.VISIBLE
    }

    private fun confirmLogin() {
        guest.confirmLogin()
        pendingLogin = false
        mode = MODE_NORMAL
        loginPendingBar.visibility = View.GONE
        loginPrompt.visibility = View.GONE
        guestBanner.visibility = View.GONE
        countDownTimer?.cancel()
        Toast.makeText(this, R.string.login_confirmed, Toast.LENGTH_LONG).show()
    }

    private fun exitToWelcome() {
        countDownTimer?.cancel()
        if (mode == MODE_GUEST) wipeGuestData()
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    /** Aba anônima: apaga cookies, cache, formulários e armazenamento do site. */
    private fun wipeGuestData() {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
            webView.clearSslPreferences()
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {
        }
    }

    /** Heurística: se aparecer um cookie de sessão do site, considera logado. */
    private fun checkLoginCookieHeuristic() {
        if (mode != MODE_LOGIN || !pendingLogin) return
        val cookies = CookieManager.getInstance().getCookie("https://arena.ai/") ?: return
        val names = cookies.split(";").map { it.substringBefore("=").trim().lowercase() }
        val loggedIn = names.any { name ->
            (name.contains("session") || name.contains("auth") || name.contains("token") ||
                name.contains("uid") || name.contains("logged")) &&
                !name.contains("csrf") && !name.contains("xsrf") &&
                !name.contains("anon") && !name.contains("guest")
        }
        if (loggedIn) confirmLogin()
    }

    // ------------------------------------------------------------------
    // WebView
    // ------------------------------------------------------------------

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
            checkLoginCookieHeuristic()
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
        countDownTimer?.cancel()
        if (mode == MODE_GUEST) wipeGuestData()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_GUEST = "guest"
        const val MODE_LOGIN = "login"
        const val MODE_NORMAL = "normal"

        private const val HOME_URL = "https://arena.ai/"
        private const val LOGIN_URL = "https://arena.ai/login"

        private const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
