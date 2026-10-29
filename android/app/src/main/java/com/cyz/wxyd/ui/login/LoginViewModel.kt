package com.cyz.wxyd.ui.login

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
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * 登录（计划 Task 4；spec 约束 7）。
 *
 * 契约（与 H5 `doLogin` + `bootstrap` 对齐）：
 * 1. `POST /api/login {body:{username,password}}` → Result.body 为 User；
 * 2. `GET /api/user/info` → `{id, username, role, isAdmin}`，以它为准组装 [UserSession]；
 * 3. 服务器地址为空时先要求用户填写（写 DataStore，后续请求动态读取）。
 *
 * 密码错误/账号待审批等返回业务错误码 000003（[ApiException]）。
 */
class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(baseUrl = container.settingsStore.baseUrlFlow.first())
        }
    }

    fun onBaseUrlChange(v: String) = _ui.update { it.copy(baseUrl = v, error = null) }

    fun onUsernameChange(v: String) = _ui.update { it.copy(username = v, error = null) }

    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }

    fun consumeError() = _ui.update { it.copy(error = null) }

    /**
     * 提交登录。成功回调 [onSuccess]（由 Screen 转交给 SessionViewModel）。
     * 失败时错误文案留在 [LoginUiState.error] 里显示在表单下方（与 H5 一致，不弹 Snackbar）。
     */
    fun login(onSuccess: (UserSession) -> Unit) {
        val state = _ui.value
        if (state.loading) return
        val baseUrl = state.baseUrl.trim()
        if (baseUrl.isEmpty()) {
            _ui.update { it.copy(error = "请先填写服务器地址") }
            return
        }
        val username = state.username.trim()
        if (username.isEmpty() || state.password.isEmpty()) {
            _ui.update { it.copy(error = "请输入用户名/邮箱和密码") }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                container.settingsStore.setBaseUrl(baseUrl)
                container.apiClient.postBody(
                    "/login",
                    buildJsonObject {
                        put("username", JsonPrimitive(username))
                        put("password", JsonPrimitive(state.password))
                    },
                )
                val info = container.apiClient.getResult("/user/info")
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

