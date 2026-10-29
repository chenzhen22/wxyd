package com.cyz.wxyd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cyz.wxyd.AppContainer

/**
 * 根主题：跟随 DataStore 中的 7 值主题（由 ThemeController 写入；
 * `system` 态读取系统深浅）。spec 第 5 节「主题」、决策 D7。
 */
@Composable
fun AppTheme(container: AppContainer, content: @Composable () -> Unit) {
    val theme by container.themeController.themeFlow.collectAsState(initial = "dark")
    val scheme = colorSchemeFor(theme, isSystemInDarkTheme())
    MaterialTheme(colorScheme = scheme, content = content)
}
