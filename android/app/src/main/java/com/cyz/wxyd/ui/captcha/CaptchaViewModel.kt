package com.cyz.wxyd.ui.captcha

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiException
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 滑块验证码状态（spec 约束 13）。
 *
 * 后端契约（`SliderCaptchaService` / `CaptchaController` 源码核对）：
 * - `GET /captcha/slider` → `Result.body = {captchaId, bg(base64 PNG), piece(base64 PNG), y}`；
 *   `bg` 固定 300×150；`y` 为拼图块在**原图坐标系**的 top（= targetY - 4）；
 * - `POST /captcha/verify {body:{captchaId, x}}` → `Result.body = {ticket}`；
 *   `x` 为拼图块在原图坐标系的**左边缘**（H5：屏幕 left / scale + 4；拖到最右为 260）；
 * - 校验失败**即销毁本题**（防爆破），必须重新 `GET captcha/slider`；
 * - `ticket` 一次性、2 分钟有效，`register/email/send` 或 `password/reset/send` 消费后作废。
 */
sealed interface CaptchaState {
    data object Loading : CaptchaState

    /**
     * 题目就绪。
     * @param notice 上一次尝试的失败提示（"滑块位置不正确，请重试"等），无则 null
     */
    data class Ready(
        val captchaId: String,
        val bg: ImageBitmap,
        val piece: ImageBitmap,
        val pieceY: Int,
        val notice: String? = null,
    ) : CaptchaState

    data object Verifying : CaptchaState

    data class Success(val ticket: String) : CaptchaState

    /** 题目本身加载失败（网络/服务端生成失败），需用户点"重试"。 */
    data class LoadFailed(val message: String) : CaptchaState
}

class CaptchaViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow<CaptchaState>(CaptchaState.Loading)
    val state: StateFlow<CaptchaState> = _state.asStateFlow()

    init {
        load()
    }

    /** 拉取新题（换一张 / 重试）。 */
    fun load(notice: String? = null) {
        _state.value = CaptchaState.Loading
        viewModelScope.launch {
            try {
                val body = container.apiClient.getResult("/captcha/slider")
                val captchaId = body["captchaId"]?.jsonPrimitive?.contentOrNull
                    ?: error("缺少 captchaId")
                _state.value = CaptchaState.Ready(
                    captchaId = captchaId,
                    bg = decodeBase64Png(body["bg"]?.jsonPrimitive?.contentOrNull),
                    piece = decodeBase64Png(body["piece"]?.jsonPrimitive?.contentOrNull),
                    pieceY = body["y"]?.jsonPrimitive?.intOrNull ?: 0,
                    notice = notice,
                )
            } catch (e: Exception) {
                _state.value = CaptchaState.LoadFailed(e.userMessage())
            }
        }
    }

    /** 松手：提交原图坐标的拼图块左边缘。失败自动换新题（服务端已毁题，不能复用）。 */
    fun submit(originalX: Int) {
        val ready = _state.value as? CaptchaState.Ready ?: return
        _state.value = CaptchaState.Verifying
        viewModelScope.launch {
            try {
                val body = container.apiClient.postBody(
                    "/captcha/verify",
                    buildJsonObject {
                        put("captchaId", JsonPrimitive(ready.captchaId))
                        put("x", JsonPrimitive(originalX))
                    },
                )
                val ticket = body["ticket"]?.jsonPrimitive?.contentOrNull
                if (ticket.isNullOrEmpty()) {
                    load(notice = "验证失败，请重试")
                } else {
                    _state.value = CaptchaState.Success(ticket)
                }
            } catch (e: ApiException) {
                load(notice = e.errorMsg.ifBlank { "验证未通过，请重试" })
            } catch (e: Exception) {
                load(notice = e.userMessage())
            }
        }
    }

    private fun decodeBase64Png(b64: String?): ImageBitmap {
        requireNotNull(b64) { "缺少图片数据" }
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("图片解码失败")
        return bitmap.asImageBitmap()
    }
}
