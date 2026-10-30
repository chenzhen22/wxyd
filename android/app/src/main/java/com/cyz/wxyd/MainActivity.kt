package com.cyz.wxyd

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyz.wxyd.ui.nav.AppNav
import com.cyz.wxyd.ui.session.SessionViewModel
import com.cyz.wxyd.ui.theme.AppTheme

/**
 * 唯一 Activity（spec：纯 Compose 单 Activity）。
 *
 * 组装：进程级 [AppContainer] → [SessionViewModel]（Activity 级，跨 Tab/模块页共享）
 * → [AppTheme]（7 套主题跟随 DataStore）→ [AppNav]（会话状态驱动路由）。
 */
class MainActivity : ComponentActivity() {

    private val app: WxydApp get() = application as WxydApp

    private val sessionViewModel: SessionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SessionViewModel(app.container) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 任何请求 401 → 会话层清会话并回登录页（Review Focus #2 的集中通道）
        app.container.apiClient.onUnauthorized = {
            runOnUiThread { if (!isFinishing && !isDestroyed) sessionViewModel.onSessionExpired() }
        }
        // 透明系统栏，背景色由主题决定
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        setContent {
            AppTheme(app.container) {
                SystemBarIcons()
                AppNav(app = app, sessionViewModel = sessionViewModel)
            }
        }
    }

    /** 系统栏图标明暗跟随当前生效配色（深底 → 浅色图标）。 */
    @Composable
    private fun SystemBarIcons() {
        val darkBackground = MaterialTheme.colorScheme.isDarkBackground()
        val view = LocalView.current
        val window = window
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkBackground
                isAppearanceLightNavigationBars = !darkBackground
            }
        }
    }
}

/** 深底判断：背景亮度低于 0.5 即用浅色系统栏图标。 */
private fun androidx.compose.material3.ColorScheme.isDarkBackground(): Boolean =
    this.background.luminance() < 0.5f
