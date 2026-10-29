package com.cyz.wxyd.ui.auth

import androidx.compose.runtime.Composable
import com.cyz.wxyd.WxydApp

/** 找回密码（计划 Task 6）：共用 [AuthWizardScreen] 的 RESET 模式。 */
@Composable
fun ForgotPasswordScreen(
    app: WxydApp,
    onBack: () -> Unit,
    onResetDone: () -> Unit,
) {
    AuthWizardScreen(
        app = app,
        mode = AuthMode.RESET,
        onBack = onBack,
        onLoggedIn = { /* 找回流程不会登录 */ },
        onResetDone = onResetDone,
    )
}
