package com.cyz.wxyd.ui.login

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.session.UserSession
import kotlinx.coroutines.launch

/**
 * 登录页（计划 Task 4 Step 2）。
 *
 * 与 H5 `#h5LoginMask` 对齐：账号 + 密码 + 登录 + 表单内错误文案；
 * 另加两条文字链接：「没有账号？去注册」「忘记密码？」（Task 6 接路由，先占位回调）。
 * 服务器地址固定为云端部署（不展示、不可改）。
 *
 * 指纹登录：本机启用过时显示"指纹登录"按钮（[LoginViewModel.loginWithBiometric]）；
 * 登录成功后若未启用且设备支持，弹一次引导（跳过记入 guideSeen，不再重复弹）。
 */
@Composable
fun LoginScreen(
    app: WxydApp,
    onLoggedIn: (UserSession) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
) {
    val vm: LoginViewModel = viewModel(factory = appFactory(app) { LoginViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val submit = { vm.login(onLoggedIn) }
    val biometricLogin = {
        (context as? FragmentActivity)?.let { vm.loginWithBiometric(it, onLoggedIn) }
            ?: vm.consumeError()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "小泽助手",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = ui.username,
                            onValueChange = vm::onUsernameChange,
                            label = { Text("用户名 / 邮箱") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = ui.password,
                            onValueChange = vm::onPasswordChange,
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        ui.error?.let { msg ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = submit,
                            enabled = !ui.loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            if (ui.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("登 录")
                            }
                        }

                        if (ui.biometricEnabled) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = biometricLogin,
                                enabled = !ui.loading,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Text("🔑 指纹登录")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = onNavigateRegister, enabled = !ui.loading) {
                                Text("没有账号？去注册", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onNavigateForgot, enabled = !ui.loading) {
                                Text("忘记密码？", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "支持内网 HTTP 明文与自签证书部署",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 设备是否具备指纹/生物识别能力（Class 3，BiometricPrompt 加密通道要求）。 */
fun hasBiometricCapability(activity: ComponentActivity): Boolean =
    BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
