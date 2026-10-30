package com.cyz.wxyd.ui.profile

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.login.hasBiometricCapability
import com.cyz.wxyd.ui.theme.ALL_THEMES
import kotlinx.coroutines.launch

/** 主题 7 值的展示名（值本身是接口/DataStore 中的英文标识）。 */
private val THEME_LABELS = linkedMapOf(
    "dark" to "暗色",
    "light" to "亮色",
    "eyecare" to "护眼",
    "techblue" to "科技蓝",
    "orange" to "暖橙",
    "pink" to "粉樱",
    "system" to "跟随系统",
)

/**
 * "我的"（计划 Task 7 Step 3）。
 *
 * - 账号信息：用户名 / 是否管理员；
 * - 主题选择（7 值单选），写 [ThemeController]：本地立即生效 + fire-and-forget 同步 `user/updateTheme`；
 * - 指纹登录开关：开启时弹 BiometricPrompt 验证 + 加密保存当前凭据（需本次会话内密码可还原，
 *   故由登录成功后的引导为主要入口；这里关闭即清除本机保存的凭据）；
 * - 退出登录：走 `SessionViewModel.logout()`，会话清空后导航图自动切回登录页；
 * - 关于：版本与首版范围（spec）。服务器地址固定云端部署，不再展示。
 */
@Composable
fun ProfileScreen(
    app: WxydApp,
    username: String,
    isAdmin: Boolean,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val theme by app.container.themeController.themeFlow.collectAsState(initial = "dark")
    val biometricEnabled by app.container.biometricStore.enabledFlow.collectAsState(initial = false)

    var themeDialog by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var biometricTip by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("账号", style = MaterialTheme.typography.titleMedium)
                SectionRule()
                SettingRow("用户名", username)
                SettingRow("身份", if (isAdmin) "管理员" else "普通用户")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("安全", style = MaterialTheme.typography.titleMedium)
                SectionRule()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("指纹登录", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "开启后登录页可用指纹快捷登录（凭据加密存本机）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { checked ->
                            val activity = context as? FragmentActivity
                            when {
                                checked -> biometricTip = "enable"
                                else -> scope.launch {
                                    app.container.biometricStore.disable()
                                }
                            }
                        },
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("外观", style = MaterialTheme.typography.titleMedium)
                SectionRule()
                SettingRow(
                    label = "主题",
                    value = THEME_LABELS[theme] ?: "暗色",
                    onClick = { themeDialog = true },
                )
            }
        }

        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("退出登录", color = MaterialTheme.colorScheme.error)
        }

    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    ALL_THEMES.forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = value == theme, onClick = {
                                    themeDialog = false
                                    scope.launch { app.container.themeController.setTheme(value) }
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = value == theme, onClick = null)
                            Text(THEME_LABELS[value] ?: value, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { themeDialog = false }) { Text("关闭") }
            },
        )
    }

    // 指纹登录开启流程：确认后弹 BiometricPrompt，验证通过即在 30 秒窗口内加密落盘
    if (biometricTip == "enable") {
        biometricTip = null
        val activity = context as? FragmentActivity
        if (activity == null || !hasBiometricCapability(activity)) {
            biometricTip = "no-capability"
        } else {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(code: Int, errString: CharSequence) {
                        if (code != BiometricPrompt.ERROR_USER_CANCELED &&
                            code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            biometricTip = "verify-failed"
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val credential = app.container.pendingLoginCredential
                        if (credential == null) {
                            // 会话是恢复的（非本次密码登录）：无凭据可存，请退出重登后再开启
                            biometricTip = "no-credential"
                            return
                        }
                        scope.launch {
                            app.container.biometricStore.enable(
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
            prompt.authenticate(info)
        }
    }

    biometricTip?.let { tip ->
        AlertDialog(
            onDismissRequest = { biometricTip = null },
            title = { Text("指纹登录") },
            text = {
                Text(
                    when (tip) {
                        "no-capability" -> "本机未录入指纹或不支持生物识别（需指纹硬件与已录入指纹）。"
                        "verify-failed" -> "指纹验证未完成，请重试。"
                        "no-credential" -> "本次会话没有可加密保存的登录凭据，请退出后重新登录再开启。"
                        else -> tip
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { biometricTip = null }) { Text("知道了") }
            },
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消") }
            },
        )
    }
}

/** 卡片内分隔线（MaterialTheme.outlineVariant 细线）。 */
@Composable
private fun SectionRule() {
    Surface(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxWidth().height(1.dp)) {}
}

@Composable
private fun SettingRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onClick != null) {
            Text(" ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
