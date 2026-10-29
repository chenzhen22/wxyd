package com.cyz.wxyd.ui.login

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.session.UserSession

/**
 * 登录页（计划 Task 4 Step 2）。
 *
 * 与 H5 `#h5LoginMask` 对齐：账号 + 密码 + 登录 + 表单内错误文案；
 * 另加两条文字链接：「没有账号？去注册」「忘记密码？」（Task 6 接路由，先占位回调）。
 * 服务器地址为空时同一表单内要求先填写（spec 第 7 节。
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

    val submit = { vm.login(onLoggedIn) }

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
                            value = ui.baseUrl,
                            onValueChange = vm::onBaseUrlChange,
                            label = { Text("服务器地址") },
                            placeholder = { Text("http://10.0.0.1:8080/api") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
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
