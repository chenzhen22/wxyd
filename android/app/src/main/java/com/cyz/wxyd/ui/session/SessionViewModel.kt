package com.cyz.wxyd.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.SessionExpiredException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 当前登录用户（spec 约束 7：登录后 `GET /api/user/info` 补全 isAdmin）。
 *
 * @param role 服务端角色号；`0` = 超管
 */
data class UserSession(
    val id: Long,
    val username: String,
    val isAdmin: Boolean,
    val role: Int = -1,
) {
    companion object {
        /** 把 `user/info` 的 body 解析为 [UserSession]；`isAdmin` 缺失时按 `role == 0` 推导。 */
        fun fromUserInfo(info: JsonObject): UserSession {
            val role = info["role"]?.jsonPrimitive?.intOrNull ?: -1
            return UserSession(
                id = info["id"]?.jsonPrimitive?.longOrNull ?: 0L,
                username = info["username"]?.jsonPrimitive?.content.orEmpty(),
                isAdmin = info["isAdmin"]?.jsonPrimitive?.booleanOrNull ?: (role == 0),
                role = role,
            )
        }
    }
}

/**
 * 全局会话状态（计划 Task 4）。
 *
 * - [session] 为 null 表示未登录：`AppNav` 据此在 `login` 与 `main` 之间切换。
 * - [restored] 为 true 前 UI 显示启动加载，避免会话探测期间闪现登录页。
 * - [expiredEvents] 集中收集 401（[SessionExpiredException]）：任何业务层 catch 后
 *   调 [onSessionExpired] 即可清会话并通知导航回登录页，不用各自处理跳转。
 *
 * 启动时用持久化的 JSESSIONID 调 `GET /api/user/info` 探测会话是否仍有效
 * （spec 第 7 节：仅持久化会话 Cookie，登录态跨进程保留）。
 */
class SessionViewModel(private val container: AppContainer) : ViewModel() {

    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _restored = MutableStateFlow(false)
    val restored: StateFlow<Boolean> = _restored.asStateFlow()

    private val _expiredEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val expiredEvents: SharedFlow<String> = _expiredEvents.asSharedFlow()

    init {
        restore()
    }

    private fun restore() = viewModelScope.launch {
        try {
            _session.value = UserSession.fromUserInfo(container.apiClient.getResult("/user/info"))
        } catch (e: SessionExpiredException) {
            // 无 Cookie / Cookie 已失效（401 已由 ApiClient 回调清过）：直接进登录页
            logoutLocal()
        } catch (e: Exception) {
            // 网络错误 / 服务器地址未配置：无法判定，按未登录处理，不打扰用户
            _session.value = null
        } finally {
            _restored.value = true
        }
    }

    /** 登录/注册成功后由 LoginViewModel 调用（已拿到 user/info 的完整信息）。 */
    fun loginSucceeded(session: UserSession) {
        _session.value = session
    }

    /**
     * 会话过期（401）：清会话并提示。由 `ApiClient.onUnauthorized` 全局回调触发
     * （MainActivity 经 runOnUiThread 转主线程），业务层无需各自处理。
     * 未登录状态下（启动探测 / 登录页）不提示，避免误报。
     */
    fun onSessionExpired(message: String = "登录已过期，请重新登录") {
        if (_session.value != null) _expiredEvents.tryEmit(message)
        logoutLocal()
    }

    /** 主动退出（"我的"页）：先调后端 `POST /logout`（失败不影响本地），再清本地会话。 */
    fun logout() = viewModelScope.launch {
        runCatching { container.apiClient.postBody("/logout", JsonObject(emptyMap())) }
        logoutLocal()
    }

    private fun logoutLocal() {
        container.cookieJar.clear()
        _session.value = null
    }
}
