package com.cyz.wxyd.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.data.local.BiometricCrypto
import com.cyz.wxyd.ui.home.HomeScreen
import com.cyz.wxyd.ui.login.hasBiometricCapability
import com.cyz.wxyd.ui.profile.ProfileScreen
import com.cyz.wxyd.ui.session.SessionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class TabSpec(val route: String, val label: String, val icon: String)

/** 底部 Tab：固定 2 个（留言板/群聊已砍，仅剩工作台/我的，spec 第 3 节）。 */
private val MAIN_TABS = listOf(
    TabSpec(route = "home", label = "工作台", icon = "🏠"),
    TabSpec(route = "profile", label = "我的", icon = "👤"),
)

/** Tab 路由：`home` / `profile` 固定可见（spec 决策：底部固定 2 Tab）。 */
object TabRoutes {
    const val HOME = "home"
    const val PROFILE = "profile"
}

/**
 * 已登录主界面（计划 Task 7 Step 4）：工作台 + 我的，双 Tab。
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
                HomeScreen(
                    app = app,
                    onOpenModule = onOpenModule,
                )
            }
            composable(TabRoutes.PROFILE) {
                ProfileScreen(
                    app = app,
                    username = session?.username.orEmpty(),
                    isAdmin = session?.isAdmin == true,
                    onLogout = { sessionViewModel.logout() },
                )
            }
        }
    }

    BiometricGuide(app)
}

/**
 * 指纹登录引导（spec 决策：登录进去后若未启用且设备支持，弹一次引导，可跳过）。
 *
 * 仅当本次会话由密码登录产生（有 [AppContainer.pendingLoginCredential]）时弹出；
 * "现在设置"直接弹 BiometricPrompt 验证并加密保存凭据；跳过/关闭即记入 guideSeen，
 * 之后只会在"我的"里手动开启。
 */
@Composable
private fun BiometricGuide(app: WxydApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val store = app.container.biometricStore
        if (app.container.pendingLoginCredential != null &&
            !store.enabledFlow.first() &&
            !store.guideSeenFlow.first() &&
            context is FragmentActivity &&
            hasBiometricCapability(context)
        ) {
            show = true
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = {
                show = false
                scope.launch { app.container.biometricStore.markGuideSeen() }
            },
            title = { Text("开启指纹登录？") },
            text = {
                Text(
                    "将验证指纹并加密保存本机登录凭据（仅存于这台设备）。\n" +
                        "之后可从登录页一键指纹登录；也可随时在\"我的\"里开启或关闭。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    scope.launch { app.container.biometricStore.markGuideSeen() }
                    val activity = context as? FragmentActivity ?: return@TextButton
                    val credential = app.container.pendingLoginCredential ?: return@TextButton
                    val prompt = BiometricPrompt(
                        activity,
                        androidx.core.content.ContextCompat.getMainExecutor(activity),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult,
                            ) {
                                val cipher = result.cryptoObject?.cipher ?: return
                                scope.launch {
                                    app.container.biometricStore.enable(
                                        cipher,
                                        credential.first,
                                        credential.second,
                                    )
                                }
                            }
                        },
                    )
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("开启指纹登录")
                        .setSubtitle("验证指纹后，下次登录可指纹快捷进入")
                        .setNegativeButtonText("取消")
                        .build()
                    prompt.authenticate(
                        info,
                        BiometricPrompt.CryptoObject(BiometricCrypto.encryptCipher()),
                    )
                }) { Text("现在设置") }
            },
            dismissButton = {
                TextButton(onClick = {
                    show = false
                    scope.launch { app.container.biometricStore.markGuideSeen() }
                }) { Text("暂不") }
            },
        )
    }
}
