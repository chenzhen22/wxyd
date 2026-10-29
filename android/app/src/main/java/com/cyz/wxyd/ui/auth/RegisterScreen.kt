package com.cyz.wxyd.ui.auth

import androidx.compose.runtime.Composable
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.session.UserSession

/**
 * 邮箱注册（计划 Task 6）：共用 [AuthWizardScreen] 的 REGISTER 模式。
 * 完成后服务端已种会话，[onLoggedIn] 交给 `SessionViewModel.loginSucceeded` → 导航切到主界面。
 */
@Composable
fun RegisterScreen(
    app: WxydApp,
    onBack: () -> Unit,
    onLoggedIn: (UserSession) -> Unit,
) {
    AuthWizardScreen(
        app = app,
        mode = AuthMode.REGISTER,
        onBack = onBack,
        onLoggedIn = onLoggedIn,
        onResetDone = onBack,
    )
}
