package com.cyz.wxyd.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.captcha.SliderCaptcha
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.session.UserSession

/**
 * 注册 / 找回密码三步向导（计划 Task 6 Step 2；spec 约束 14）。
 *
 * 两步界面共用本组件，仅 [AuthMode] 不同：
 * ```
 * 第 1 步 验证邮箱：邮箱输入 + 滑块（通过后启用"发送验证码"，与 H5 顺序一致）
 * 第 2 步 输入验证码：6 位码 + 60s 重发倒计时（重发需重新滑块）
 * 第 3 步 设置账号 / 新密码：密码×2（注册可填用户名，留空自动生成）
 * ```
 * 注册完成 → 服务端已种会话 → [onLoggedIn]（导航随之切到主界面）；
 * 找回完成 → [onResetDone]（回登录页，需用新密码登录）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthWizardScreen(
    app: WxydApp,
    mode: AuthMode,
    onBack: () -> Unit,
    onLoggedIn: (UserSession) -> Unit,
    onResetDone: () -> Unit,
) {
    val vm: AuthWizardViewModel = viewModel(
        key = mode.name,
        factory = appFactory(app) { AuthWizardViewModel(it.container, mode) },
    )
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mode.title) },
                navigationIcon = {
                    IconButton(onClick = { vm.back(onBack) }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .widthIn(max = 480.dp),
        ) {
            StepIndicator(state.step)
            Spacer(Modifier.height(20.dp))
            // 错误直接显示在表单内（与登录页一致），修改输入后由 ViewModel 清除
            state.error?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
            }

            when (state.step) {
                AuthStep.CAPTCHA -> CaptchaStep(vm, state, app)
                AuthStep.EMAIL_CODE -> EmailCodeStep(vm, state)
                AuthStep.FINISH -> FinishStep(vm = vm, state = state, onLoggedIn = onLoggedIn, onResetDone = onResetDone)
            }
        }
    }

}

@Composable
private fun StepIndicator(step: AuthStep) {
    val labels = listOf("验证邮箱", "输入验证码", "设置账号")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.forEachIndexed { i, label ->
            val idx = when (step) {
                AuthStep.CAPTCHA -> 1
                AuthStep.EMAIL_CODE -> 2
                AuthStep.FINISH -> 3
            }
            val active = i + 1 <= idx
            Text(
                text = "${i + 1}. $label",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CaptchaStep(
    vm: AuthWizardViewModel,
    state: AuthWizardState,
    app: WxydApp,
) {
    Text("第 1 步：输入邮箱，拖动滑块完成验证", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.email,
        onValueChange = vm::onEmailChange,
        label = { Text("邮箱") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    SliderCaptcha(
        app = app,
        onVerified = vm::onCaptchaVerified,
        onFailed = { /* 题目加载失败时组件内部显示重试 */ },
    )
    if (!state.needsCaptcha) {
        Spacer(Modifier.height(8.dp))
        Text("验证通过 ✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = vm::sendCode,
        enabled = !state.loading && !state.needsCaptcha,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("发送验证码")
        }
    }
    if (state.needsCaptcha) {
        Spacer(Modifier.height(8.dp))
        Text(
            "请先拖动上方滑块完成验证",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmailCodeStep(vm: AuthWizardViewModel, state: AuthWizardState) {
    Text("验证码已发送至 ${state.email}", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.code,
        onValueChange = vm::onCodeChange,
        label = { Text("6 位验证码") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = vm::verifyCode,
        enabled = !state.loading && state.code.length == 6,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("验证邮箱")
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = vm::resend,
            enabled = state.resendLeft == 0 && !state.loading,
        ) {
            Text(
                if (state.resendLeft > 0) "重新发送 (${state.resendLeft}s)" else "重新发送",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FinishStep(
    vm: AuthWizardViewModel,
    state: AuthWizardState,
    onLoggedIn: (UserSession) -> Unit,
    onResetDone: () -> Unit,
) {
    Text(
        if (state.mode == AuthMode.REGISTER) "第 3 步：设置密码与用户名" else "第 3 步：设置新密码",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = vm::onPasswordChange,
        label = { Text(if (state.mode == AuthMode.REGISTER) "密码（至少 6 位）" else "新密码（至少 6 位）") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.password2,
        onValueChange = vm::onPassword2Change,
        label = { Text("确认密码") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.mode == AuthMode.REGISTER) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = vm::onUsernameChange,
            label = { Text("用户名（选填，留空自动生成）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { vm.complete(onLoggedIn, onResetDone) },
        enabled = !state.loading && state.token != null,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(if (state.mode == AuthMode.REGISTER) "完成注册" else "重置密码")
        }
    }
    if (state.token == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            "请返回上一步重新验证验证码",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
