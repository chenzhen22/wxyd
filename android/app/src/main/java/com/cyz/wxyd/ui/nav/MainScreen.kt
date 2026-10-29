package com.cyz.wxyd.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.session.SessionViewModel

private data class TabSpec(val route: String, val label: String, val icon: String)

/** 底部 Tab：固定 2 个（留言板/群聊已砍，仅剩工作台/我的，spec 第 3 节）。 */
private val MAIN_TABS = listOf(
    TabSpec(route = "home", label = "工作台", icon = "🏠"),
    TabSpec(route = "profile", label = "我的", icon = "👤"),
)

/** Tab 路由（Task 7 的 HomeScreen/ProfileScreen 挂到这里）。 */
object TabRoutes {
    const val HOME = "home"
    const val PROFILE = "profile"
}

/**
 * 已登录主界面骨架（计划 Task 4；Task 7 实装两页内容）。
 *
 * 双 Tab 用内层 [NavHost]，切换时保存/恢复各 Tab 状态；
 * 模块页由外层 `MainNav` 压栈，打开模块时底部栏不显示（单页面栈，界面范式）。
 */
@Composable
fun MainScreen(
    app: WxydApp,
    sessionViewModel: SessionViewModel,
    onOpenModule: (String) -> Unit,
) {
    val session by sessionViewModel.session.collectAsState()
    val tabNav = rememberNavController()
    val backStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MAIN_TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNav.navigate(tab.route) {
                                popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = tabNav,
            startDestination = TabRoutes.HOME,
            modifier = Modifier.padding(inner),
        ) {
            composable(TabRoutes.HOME) {
                PlaceholderScreen(
                    title = "工作台",
                    subtitle = session?.let { "你好，${it.username} 👋" }.orEmpty(),
                    onBack = null,
                )
            }
            composable(TabRoutes.PROFILE) {
                PlaceholderScreen(
                    title = "我的",
                    subtitle = session?.let {
                        "${it.username}${if (it.isAdmin) "（管理员）" else ""}"
                    }.orEmpty(),
                    onBack = null,
                )
            }
        }
    }
}
