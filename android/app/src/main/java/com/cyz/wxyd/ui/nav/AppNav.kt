package com.cyz.wxyd.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.auth.ForgotPasswordScreen
import com.cyz.wxyd.ui.auth.RegisterScreen
import com.cyz.wxyd.ui.login.LoginScreen
import com.cyz.wxyd.ui.welfare.WelfareDetailScreen
import com.cyz.wxyd.ui.welfare.WelfareScreen
import com.cyz.wxyd.ui.dayinfo.DayInfoScreen
import com.cyz.wxyd.ui.session.SessionViewModel

/**
 * 应用导航骨架（计划 Task 4 Step 3；界面范式：底部 2 Tab + 单页面栈）。
 *
 * 依据会话状态挂两套互斥的路由图（`key(session != null)` 让会话切换时重建导航栈，
 * startDestination 随之改变，401 清会话后必然落回登录页）：
 * - 无会话 → `login` / `register` / `forgot`，起始 `login`；
 * - 有会话 → `main`（工作台/我的，Task 7 实装）+ 模块路由，起始 `main`。
 *
 * 401（`SessionExpiredException`）由 [SessionViewModel.expiredEvents] 集中上报：
 * 这里弹 Snackbar；会话被清空后路由图重建回登录页（spec 约束 1、Review Focus #2）。
 */
@Composable
fun AppNav(app: WxydApp, sessionViewModel: SessionViewModel) {
    val session by sessionViewModel.session.collectAsState()
    val restored by sessionViewModel.restored.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        sessionViewModel.expiredEvents.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // edge-to-edge：内容避让状态栏/导航栏/输入法
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !restored -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                session == null -> key(false) {
                    AuthNav(app, sessionViewModel, snackbarHostState)
                }

                else -> key(true) {
                    MainNav(app, sessionViewModel)
                }
            }
        }
    }
}

/**
 * 未登录路由图：登录 → 注册 / 找回（Task 6 向导）。
 *
 * - 注册完成：`loginSucceeded` 置会话 → 外层 `key(session != null)` 重建为 [MainNav]，无需 navigate；
 * - 找回完成：弹提示后回 `login`（需用新密码登录）。
 */
@Composable
private fun AuthNav(
    app: WxydApp,
    sessionViewModel: SessionViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                app = app,
                onLoggedIn = sessionViewModel::loginSucceeded,
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateForgot = { navController.navigate(Routes.FORGOT) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onLoggedIn = sessionViewModel::loginSucceeded,
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onResetDone = {
                    navController.popBackStack()
                    snackbarHostState.showSnackbar("密码已重置，请用新密码登录")
                },
            )
        }
    }
}

/**
 * 已登录路由图：`main` 双 Tab + 模块页。
 *
 * 模块页由 [ModuleRoute] 按 menu/list 的 `key` 分发；各模块在 Task 8–13 逐个实装，
 * 未实装的 key 落到"建设中"占位（保证无死链）。
 */
@Composable
private fun MainNav(app: WxydApp, sessionViewModel: SessionViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                app = app,
                sessionViewModel = sessionViewModel,
                onOpenModule = { key -> navController.navigate(Routes.module(key)) },
            )
        }
        composable(
            route = Routes.MODULE,
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
        ) { entry ->
            val key = entry.arguments?.getString("key").orEmpty()
            ModuleRoute(
                key = key,
                app = app,
                onBack = { navController.popBackStack() },
                onOpenWelfareDetail = { id -> navController.navigate(Routes.welfareDetail(id)) },
            )
        }
        composable(
            route = Routes.WELFARE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            WelfareDetailScreen(
                app = app,
                id = entry.arguments?.getLong("id") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * 模块分发（计划 Task 7 Step 4）。`key` 来自 `menu/list?platform=h5`，首批范围：
 * welfare / dayinfo / gateway / robot / notes / share / shell / javaapi
 * （留言板/群聊/巡检/证据/归档、admin 模块首版不做，落占位）。
 */
@Composable
private fun ModuleRoute(
    key: String,
    app: WxydApp,
    onBack: () -> Unit,
    onOpenWelfareDetail: (Long) -> Unit,
) {
    when (key) {
        "welfare" -> WelfareScreen(app = app, onBack = onBack, onOpenDetail = onOpenWelfareDetail)
        "dayinfo" -> DayInfoScreen(app = app, onBack = onBack)
        else -> PlaceholderScreen(title = "建设中", subtitle = "$key 即将上线", onBack = onBack)
    }
}
