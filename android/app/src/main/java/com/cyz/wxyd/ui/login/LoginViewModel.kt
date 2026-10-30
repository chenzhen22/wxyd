package com.cyz.wxyd.ui.login

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiException
import com.cyz.wxyd.ui.common.userMessage
import com.cyz.wxyd.ui.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** 登录页 UI 状态。 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** 本机已启用指纹登录（显示"指纹登录"入口）。 */
    val biometricEnabled: Boolean = false,
)

/**
 * 登录（计划 Task 4；spec 约束 7）。
 *
 * 契约（与 H5 `doLogin` + `bootstrap` 对齐）：
 * 1. `POST /api/login {body:{username,password}}` → Result.body 为 User；
 * 2. `GET /api/user/info` → `{id, username, role, isAdmin}`，以它为准组装 [UserSession]；
 * 3. 服务器地址已固定为云端部署（[com.cyz.wxyd.data.local.SettingsStore.DEFAULT_BASE_URL]）。
 *
 * 指纹登录：本机启用过（[AppContainer.biometricStore]）时展示入口，
 * BiometricPrompt（CryptoObject 解密密钥）成功后用还原的凭据走同一登录契约。
 * 密码错误/账号待审批等返回业务错误码 000003（[ApiException]）。
 */
class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                biometricEnabled = container.biometricStore.enabledFlow.first(),
            )
        }
    }

    fun onUsernameChange(v: String) = _ui.update { it.copy(username = v, error = null) }

    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }

    fun consumeError() = _ui.update { it.copy(error = null) }

    /** 刷新指纹入口可见性（启用/关闭后调用）。 */
    fun refreshBiometric() = viewModelScope.launch {
        _ui.update { it.copy(biometricEnabled = container.biometricStore.enabledFlow.first()) }
    }

    /**
     * 弹出系统指纹对话框（凭据密钥为 30 秒时限窗口模式，无需 CryptoObject，
     * 兼容 Class 2/Class 3 指纹）。成功 → 解密还原凭据 → 走 [doLogin] 契约；
     * 失败/取消 → 文案留在表单错误位。
     */
    fun loginWithBiometric(activity: FragmentActivity, onSuccess: (UserSession) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            androidx.core.content.ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    _ui.update { it.copy(loading = false) }
                    if (code != BiometricPrompt.ERROR_USER_CANCELED &&
                        code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        _ui.update { it.copy(error = msg.toString()) }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // 指纹通过 → 30 秒窗口内解密凭据并登录
                    viewModelScope.launch {
                        try {
                            val payload = container.biometricStore.credentialPayload()
                                ?: throw IllegalStateException("指纹凭据不存在")
                            val credential =
                                container.biometricStore.restoreCredential(payload)
                                    ?: throw IllegalStateException("指纹凭据不存在")
                            val (username, password) = credential
                            doLogin(username, password, onSuccess)
                        } catch (e: Exception) {
                            // Keystore 密钥失效（如清除锁屏凭据）等：提示重新启用
                            _ui.update {
                                it.copy(
                                    loading = false,
                                    error = "指纹凭据不可用，请用密码登录后重新启用指纹",
                                    biometricEnabled = false,
                                )
                            }
                        }
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("指纹登录")
            .setSubtitle("验证已保存的指纹，用本机保存的凭据登录")
            .setNegativeButtonText("用密码登录")
            .build()
        _ui.update { it.copy(loading = true, error = null) }
        prompt.authenticate(info)
    }

    /**
     * 表单提交登录。成功回调 [onSuccess]（由 Screen 转交给 SessionViewModel）。
     * 失败时错误文案留在 [LoginUiState.error] 里显示在表单下方（与 H5 一致，不弹 Snackbar）。
     */
    fun login(onSuccess: (UserSession) -> Unit) {
        val state = _ui.value
        if (state.loading) return
        if (state.username.isBlank() || state.password.isEmpty()) {
            _ui.update { it.copy(error = "请输入用户名/邮箱和密码") }
            return
        }
        doLogin(state.username.trim(), state.password, onSuccess)
    }

    /** 登录契约本体：login → user/info → UserSession（表单与指纹两条路共用）。 */
    private fun doLogin(username: String, password: String, onSuccess: (UserSession) -> Unit) {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/login",
                    buildJsonObject {
                        put("username", JsonPrimitive(username))
                        put("password", JsonPrimitive(password))
                    },
                )
                val info = container.apiClient.getResult("/user/info")
                // 记录本次凭据（仅内存）：登录后引导/“我的”里开启指纹登录时加密落盘
                container.pendingLoginCredential = username to password
                _ui.update { it.copy(loading = false, password = "") }
                onSuccess(UserSession.fromUserInfo(info))
            } catch (e: ApiException) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }
}
