package com.cyz.wxyd.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.ui.common.userMessage
import com.cyz.wxyd.ui.session.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** 向导模式：注册 / 找回密码（两者接口同构，只是路径前缀不同）。 */
enum class AuthMode {
    REGISTER,
    RESET;

    /** 接口前缀：`register/email` 或 `password/reset`。 */
    val prefix: String get() = if (this == REGISTER) "register/email" else "password/reset"

    /** verify 返回的一次性令牌字段名。 */
    val tokenField: String get() = if (this == REGISTER) "registerToken" else "resetToken"

    val title: String get() = if (this == REGISTER) "邮箱注册" else "找回密码"
}

/** 向导步骤（spec 约束 14）。 */
enum class AuthStep { CAPTCHA, EMAIL_CODE, FINISH }

data class AuthWizardState(
    val mode: AuthMode,
    val step: AuthStep = AuthStep.CAPTCHA,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val password2: String = "",
    val username: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    /** 重发倒计时（秒），>0 时禁用重发。 */
    val resendLeft: Int = 0,
    /** verify 返回的一次性令牌（registerToken / resetToken），用于第 3 步提交。 */
    val token: String? = null,
    /**
     * 滑块通过的 ticket（一次性）。持有到 `send` 被调用为止：
     * 用户可能在滑块通过后才去填/改邮箱，那时 send 还没发出去，ticket 尚未被服务端消费。
     */
    val ticket: String? = null,
    /**
     * 一次性票据失效（邮箱在滑块后改过、或回到第 1 步）：必须重新滑动才能发送。
     * 与 H5 一致——`send` 前必须先走第一步的滑块。
     */
    val needsCaptcha: Boolean = true,
)

/**
 * 注册 / 找回密码三步向导（spec 约束 13–14；计划 Task 6）。
 *
 * 状态机：
 * ```
 * CAPTCHA（滑块通过 → ticket 暂存）
 *   → EMAIL_CODE（send {email,ticket}，60s 倒计时 → verify {email,code} → token）
 *   → FINISH（complete）
 * ```
 *
 * 关键约束（对照 H5 `register.html` / `forgot.html` 与服务端 `AuthController`）：
 * - **ticket 一次性**：`send` 成功后即作废；`send` 失败必须回 [AuthStep.CAPTCHA]
 *   重新滑（服务端 ticket 已被 consume），不能复用（Review Focus #4）；
 * - **重发**需要重新滑块验证（H5 `resend()` 回第 1 步）；
 * - 滑块通过后改邮箱 → 暂存的 ticket 不再代表这个邮箱，置 [AuthWizardState.needsCaptcha]；
 * - 注册 complete 成功即登录（服务端种会话），这里再拉 `user/info` 组装会话；
 *   找回 complete 只改密码，不登录，回登录页。
 */
class AuthWizardViewModel(
    private val container: AppContainer,
    val mode: AuthMode,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthWizardState(mode = mode))
    val state: StateFlow<AuthWizardState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    // ---- 表单 ----

    fun onEmailChange(v: String) = _state.update {
        // 邮箱变了，之前滑过的 ticket 作废（发给哪个邮箱的验证码要和服务端票据对应）
        it.copy(email = v, error = null, ticket = null, needsCaptcha = true)
    }

    fun onCodeChange(v: String) =
        _state.update { it.copy(code = v.filter(Char::isDigit).take(6), error = null) }

    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }

    fun onPassword2Change(v: String) = _state.update { it.copy(password2 = v, error = null) }

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }

    fun consumeError() = _state.update { it.copy(error = null) }

    // ---- Step 1：滑块 ----

    /**
     * 滑块通过（同时启用"发送验证码"按钮）。
     * H5 是"滑块通过即发"，这里需要邮箱，所以滑块通过只暂存 ticket，由用户点发送。
     */
    fun onCaptchaVerified(ticket: String) {
        _state.update { it.copy(ticket = ticket, needsCaptcha = false, error = null) }
    }

    /** 发送邮箱验证码：需已通过滑块（一次性 ticket）。 */
    fun sendCode() {
        val s = _state.value
        if (s.loading) return
        val email = s.email.trim()
        val ticket = s.ticket
        if (!EMAIL_REGEX.matches(email)) {
            _state.update { it.copy(error = "请输入正确的邮箱地址") }
            return
        }
        if (ticket.isNullOrEmpty()) {
            _state.update { it.copy(error = "请先拖动滑块完成验证") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/${mode.prefix}/send",
                    buildJsonObject {
                        put("email", JsonPrimitive(email))
                        put("ticket", JsonPrimitive(ticket))
                    },
                )
                // ticket 已在服务端消费
                _state.update {
                    it.copy(
                        loading = false,
                        step = AuthStep.EMAIL_CODE,
                        email = email,
                        ticket = null,
                        needsCaptcha = true,
                        code = "",
                    )
                }
                startCountdown()
            } catch (e: Exception) {
                // ticket 已被服务端消费：必须重新滑块（Review Focus #4）
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.userMessage(),
                        ticket = null,
                        needsCaptcha = true,
                    )
                }
            }
        }
    }

    /** 重发：回第 1 步重新滑块（ticket 一次性）。 */
    fun resend() {
        if (_state.value.resendLeft > 0) return
        _state.update {
            it.copy(step = AuthStep.CAPTCHA, code = "", error = null, ticket = null, needsCaptcha = true)
        }
    }

    /** 向导内"返回"：按步骤退一层；第 1 步返回由调用方关闭页面。 */
    fun back(onExit: () -> Unit) {
        val s = _state.value
        when (s.step) {
            AuthStep.CAPTCHA -> onExit()
            // 退回第 2 步保持倒计时；换邮箱需重新滑块
            AuthStep.EMAIL_CODE -> _state.update {
                it.copy(step = AuthStep.CAPTCHA, ticket = null, needsCaptcha = true, error = null)
            }
            // 同一 token 可重试 complete（失败分支服务端不消费），只清表单错误
            AuthStep.FINISH -> _state.update { it.copy(error = null) }
        }
    }

    // ---- Step 2：校验验证码 → 一次性 token ----

    fun verifyCode() {
        val s = _state.value
        if (s.loading) return
        if (s.code.length != 6) {
            _state.update { it.copy(error = "请输入 6 位验证码") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val body = container.apiClient.postBody(
                    "/${mode.prefix}/verify",
                    buildJsonObject {
                        put("email", JsonPrimitive(s.email))
                        put("code", JsonPrimitive(s.code))
                    },
                )
                val token = body[mode.tokenField]?.jsonPrimitive?.contentOrNull
                if (token.isNullOrEmpty()) {
                    _state.update { it.copy(loading = false, error = "验证失败，请重试", code = "") }
                    return@launch
                }
                _state.update { it.copy(loading = false, step = AuthStep.FINISH, token = token) }
            } catch (e: Exception) {
                // 与服务端一致：验证码错误时清空重填
                _state.update { it.copy(loading = false, error = e.userMessage(), code = "") }
            }
        }
    }

    // ---- Step 3：完成 ----

    /**
     * 完成注册 / 重置。
     * - REGISTER：`complete {registerToken,password,username}` → 已自动登录，拉 user/info 组装会话；
     * - RESET：`complete {resetToken,password}` → 不登录，回登录页。
     */
    fun complete(
        onLoggedIn: (UserSession) -> Unit,
        onResetDone: () -> Unit,
    ) {
        val s = _state.value
        if (s.loading) return
        val token = s.token
        if (token.isNullOrEmpty()) {
            _state.update { it.copy(error = "验证已失效，请返回上一步重新验证") }
            return
        }
        if (s.password.length < 6) {
            _state.update { it.copy(error = "密码长度至少 6 位") }
            return
        }
        if (s.password != s.password2) {
            _state.update { it.copy(error = "两次输入的密码不一致") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                if (mode == AuthMode.REGISTER) {
                    // H5 恒定发送 username（用户名为空串时服务端自动生成），保持一致
                    container.apiClient.postBody(
                        "/register/email/complete",
                        buildJsonObject {
                            put("registerToken", JsonPrimitive(token))
                            put("password", JsonPrimitive(s.password))
                            put("username", JsonPrimitive(s.username.trim()))
                        },
                    )
                    // 服务端已种会话：拉 user/info 补 isAdmin（与登录流程一致）
                    val info = container.apiClient.getResult("/user/info")
                    _state.update { it.copy(loading = false) }
                    onLoggedIn(UserSession.fromUserInfo(info))
                } else {
                    container.apiClient.postBody(
                        "/password/reset/complete",
                        buildJsonObject {
                            put("resetToken", JsonPrimitive(token))
                            put("password", JsonPrimitive(s.password))
                        },
                    )
                    _state.update { it.copy(loading = false) }
                    onResetDone()
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    /**
     * 60 → 1 的倒计时流，每秒发一个剩余秒数（与 H5 `startResendCountdown` 一致）。
     * 取消（回第 1 步重发 / 页面关闭）时经 `onCompletion` 把剩余秒数归零，按钮立即可用。
     */
    fun countDownFlow(seconds: Int = RESEND_SECONDS) = flow {
        for (left in seconds downTo 1) {
            emit(left)
            delay(1_000)
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            countDownFlow()
                .catch { /* 下游取消/异常不打断状态收尾 */ }
                .onCompletion { _state.update { it.copy(resendLeft = 0) } }
                .collect { left -> _state.update { it.copy(resendLeft = left) } }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
    }

    companion object {
        private const val RESEND_SECONDS = 60
        private val EMAIL_REGEX = Regex("^[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
