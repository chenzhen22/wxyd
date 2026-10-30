package com.cyz.wxyd.ui.robot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiResult
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.io.File

/** 发送表单的类型。 */
enum class RobotSendType(val wire: String) { TEXT("text"), FILE("file") }

data class RobotUiState(
    val loading: Boolean = true,
    val robots: List<RobotItem> = emptyList(),
    val error: String? = null,
    val message: String? = null,

    /** 新增 / 编辑表单（null = 关闭） */
    val editing: RobotEditForm? = null,
    /** 发送表单（null = 关闭） */
    val sending: RobotSendForm? = null,
    val busy: Boolean = false,
)

/** 新增 / 编辑表单；`id == null` 为新增。secret 编辑时留空表示不修改。 */
data class RobotEditForm(
    val id: Long? = null,
    val name: String = "",
    val accessToken: String = "",
    val secret: String = "",
) {
    val isEdit: Boolean get() = id != null
    val canSave: Boolean get() = name.isNotBlank() && accessToken.isNotBlank()
}

data class RobotSendForm(
    val robotId: Long,
    val robotName: String,
    val type: RobotSendType = RobotSendType.TEXT,
    val text: String = "",
    val file: File? = null,
)

/**
 * 钉钉机器人（计划 Task 11）。
 *
 * 请求体格式是本模块最容易错的地方（spec 约束 4，Review Focus #1），逐接口对照 H5 `h5.js`：
 * - `robot/list` —— **GET**，`body` 是数组 → `getRaw` + [ApiResult.parseArray]
 *   （注意不是 `getResult`：后者只保留对象型 body，数组会变 `{}`）；
 * - `robot/add` / `robot/update` / `robot/delete` —— **直接字段 JSON**（`postDirect`），
 *   `{"name","accessToken","secret"[,"id"]}`，**不包** `{body:{}}`；
 * - `robot/send` —— **multipart**（`postMultipart`），字段 `robotId`、`type`(text|file)、
 *   `text` 或 `file` 二选一。
 *
 * 服务端在"HTTP 成功"时也可能没真正发出：`body.error` 非空即实际失败（H5 同样判断）。
 */
class RobotViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(RobotUiState())
    val ui: StateFlow<RobotUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val raw = container.apiClient.getRaw("/robot/list").use { it.body?.string().orEmpty() }
                val robots = parseRobots(ApiResult.parseArray(raw))
                _ui.update { it.copy(loading = false, robots = robots, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    // ---- 新增 / 编辑 ----

    fun openCreate() = _ui.update { it.copy(editing = RobotEditForm()) }

    fun openEdit(robot: RobotItem) = _ui.update {
        it.copy(editing = RobotEditForm(id = robot.id, name = robot.name, accessToken = robot.accessToken, secret = ""))
    }

    fun closeEdit() = _ui.update { it.copy(editing = null) }

    fun updateEdit(transform: (RobotEditForm) -> RobotEditForm) =
        _ui.update { s -> s.editing?.let { s.copy(editing = transform(it)) } ?: s }

    /** 保存：add/update 走 postDirect（直接字段，不包 body），与 H5 `postJSON` 的 "body 展开"等价。 */
    fun save() {
        val form = _ui.value.editing ?: return
        if (!form.canSave || _ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        val payload = buildJsonObject {
            if (form.id != null) put("id", form.id)
            put("name", form.name.trim())
            put("accessToken", form.accessToken.trim())
            put("secret", form.secret.trim())
        }
        viewModelScope.launch {
            try {
                val path = if (form.isEdit) "/robot/update" else "/robot/add"
                container.apiClient.postDirect(path, payload)
                _ui.update { it.copy(busy = false, editing = null, message = "已保存") }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(busy = false, message = e.userMessage()) }
            }
        }
    }

    // ---- 删除 ----

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                container.apiClient.postDirect("/robot/delete", buildJsonObject { put("id", id) })
                _ui.update { it.copy(message = "已删除") }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    // ---- 发送 ----

    fun openSend(robot: RobotItem) = _ui.update {
        it.copy(sending = RobotSendForm(robotId = robot.id, robotName = robot.name))
    }

    fun closeSend() = _ui.update { it.copy(sending = null) }

    fun updateSend(transform: (RobotSendForm) -> RobotSendForm) =
        _ui.update { s -> s.sending?.let { s.copy(sending = transform(it)) } ?: s }

    /** 发送：multipart，`robotId` / `type` / `text` 或 `file`。 */
    fun send() {
        val form = _ui.value.sending ?: return
        if (_ui.value.busy) return
        if (form.type == RobotSendType.TEXT && form.text.isBlank()) {
            _ui.update { it.copy(message = "请输入消息内容") }
            return
        }
        if (form.type == RobotSendType.FILE && form.file == null) {
            _ui.update { it.copy(message = "请选择文件") }
            return
        }
        _ui.update { it.copy(busy = true) }
        val fields = buildMap {
            put("robotId", form.robotId.toString())
            put("type", form.type.wire)
            if (form.type == RobotSendType.TEXT) put("text", form.text)
        }
        val files = if (form.type == RobotSendType.FILE) mapOf("file" to form.file!!) else emptyMap()
        viewModelScope.launch {
            try {
                val body = container.apiClient.postMultipart("/robot/send", fields, files)
                // 服务端"成功"但实际未发出：body.error 非空（H5 同判断）
                val err = bodyStrError(body)
                if (err != null) {
                    _ui.update { it.copy(busy = false, message = err) }
                } else {
                    val ok = if (form.type == RobotSendType.FILE) "文件已发送" else "文本已发送"
                    _ui.update { it.copy(busy = false, sending = null, message = ok) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(busy = false, message = e.userMessage()) }
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }

    private val trueStr = JsonPrimitive(true)
    private val falseStr = JsonPrimitive(false)

    private fun bodyStrError(body: JsonObject): String? {
        // 只在 error 是非空、且不是布尔值时视为失败（避免把 error:false 误报成失败）
        val prim = body["error"] as? JsonPrimitive ?: return null
        if (prim == trueStr || prim == falseStr) return null
        return prim.contentOrNull?.takeIf { it.isNotBlank() }
    }
}
