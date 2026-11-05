package com.cyz.wxyd

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * 安卓客户端外壳：直接加载服务端 H5 页面（h5.html），原生界面全面退役。
 *
 * - 入口 `/api/login.html`：未登录进登录页，已登录 H5 内部跳 h5.html；
 * - Cookie 由 [CookieManager] 持久化（跨进程保留 JSESSIONID），
 *   服务端会话有效期 30 天（application.yml `server.servlet.session`）；
 * - 返回键优先让 WebView 回退页面历史，无历史才退出；
 * - H5 里的文件上传（`<input type=file>`）经 [onShowFileChooser] 桥接到系统选择器。
 */
class MainActivity : Activity() {

    companion object {
        /** 服务端入口：与 SettingsStore.DEFAULT_BASE_URL 同源的登录页。 */
        const val START_URL = "https://wxyd.us.ci/api/login.html"
    }

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        swipeRefresh = SwipeRefreshLayout(this).apply {
            addView(webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setColorSchemeColors(0xFF3EB575.toInt())   // 薄荷绿转圈
            setOnRefreshListener { reloadFresh() }
        }
        setContentView(swipeRefresh)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // H5 群聊消息存 localStorage
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        // 会话 Cookie 持久化：WebView CookieManager 落盘，重启 App 不掉登录态
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // 站外链接（如 GitHub OAuth 授权页）交给系统浏览器
                val url = request.url
                if (url.host == Uri.parse(START_URL).host) return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url)); true
                } catch (e: Exception) { true }
            }

            override fun onPageFinished(view: WebView, url: String) {
                swipeRefresh.isRefreshing = false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams,
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    startActivityForResult(params.createIntent(), REQ_FILE); true
                } catch (e: Exception) {
                    filePathCallback = null; false
                }
            }
        }
        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else loadUrlFresh(START_URL)
    }

    /** 强制绕过缓存加载（每次启动/下拉刷新都向服务器校验新鲜度，保证 H5 更新即时可见）。 */
    private fun loadUrlFresh(url: String) {
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.loadUrl(url, mapOf("Cache-Control" to "no-cache"))
    }

    /** 下拉刷新：重载当前页并绕过缓存。 */
    private fun reloadFresh() {
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.reload()
        // onPageFinished 恢复默认缓存模式（页面内跳转走缓存更快）
        webView.postDelayed({
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }, 1500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE) {
            val uris = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()   // 立即落盘会话 Cookie
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 兼容部分机型的返回键分发路径
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack(); return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private object Keys { const val REQ_FILE = 1001 }
    private val REQ_FILE get() = Keys.REQ_FILE
}