package com.cyz.wxyd.ui.gateway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiClient
import com.cyz.wxyd.data.remote.ApiResult
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/** 我的一把 Key（`GET /gateway/keys` → `body.list[]`，api_key 只回掩码）。 */
data class GatewayKey(
    val id: Long,
    val name: String,
    /** 绑定的模型 id；`auto`/空 = 跟随系统 */
    val model: String,
    /** 服务端 enabled 原值（每日自动停用后为 false） */
    val enabled: Boolean,
    /** 手动启用的日期（yyyy-MM-dd）；与 enabled 一起决定 Key 今天是否真的可用 */
    val activeDate: String,
    val maskedKey: String,
    val createTime: String,
    val lastUsedTime: String,
)

data class GatewayUiState(
    val loading: Boolean = true,
    val keys: List<GatewayKey> = emptyList(),
    /** 每人最多可创建的 Key 数（服务端下发） */
    val max: Int = 0,
    val models: List<String> = emptyList(),
    val defaultModel: String = "",
    val error: String? = null,

    val sheetOpen: Boolean = false,
    /** 新增表单里选中的模型（`auto` 或目录内模型 id） */
    val formModel: String = AUTO_MODEL,
    val submitting: Boolean = false,

    /** reveal 结果弹窗：key 明文 */
    val revealed: String? = null,
    val message: String? = null,
) {
    companion object {
        const val AUTO_MODEL = "auto"
    }
}

/**
 * 模型网关（用户侧，计划 Task 10）。
 *
 * - 我的 Key：`GET /gateway/keys`（body `{list, max}`）；
 * - 新增 / 启停 / 删除 / 查看明文：`POST /gateway/keys/{add,toggle,delete,reveal}`，全部 postBody 双层；
 * - 模型目录：`GET /gateway/models`（body `{models, default}`）。
 *
 * **每日失效**（spec 约束 9）：Key 只在手动启用的当天有效，次日 00:00 自动停用。
 * 因此"今天可用" = `enabled && activeDate == 今天`（管理员 Key 豁免，`adminExempt` 由服务端标记，
 * 但首版按服务端 `enabled` 原值展示，不做额外推断）；失效后用户需再次点"启用"。
 *
 * 不含管理员诊断/用量/账号池接口（首版不做）。
 */
class GatewayViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(GatewayUiState())
    val ui: StateFlow<GatewayUiState> = _ui.asStateFlow()

    /** 服务端 `adminExempt`：管理员 Key 不受每日停用限制（用于"今天可用"判定）。 */
    @Volatile
    private var adminExempt = false

    init {
        refresh()
    }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val body = container.apiClient.getResult("/gateway/keys")
                // 服务端把 adminExempt 放在列表每行里（同属主的 Key 值相同），取第一行即可
                adminExempt = (body["list"] as? JsonArray)?.firstOrNull()
                    ?.let { (it as? JsonObject)?.get("adminExempt") }
                    ?.jsonPrimitive?.booleanOrNull
                    ?: false
                val keys = parseKeys(body["list"] as? JsonArray ?: JsonArray(emptyList()))
                val max = body["max"]?.jsonPrimitive?.intOrNull ?: 0
                _ui.update { it.copy(loading = false, keys = keys, max = max, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
        loadModels()
    }

    private fun loadModels() {
        viewModelScope.launch {
            runCatching {
                val body = container.apiClient.getResult("/gateway/models")
                _ui.update {
                    it.copy(
                        models = (body["models"] as? JsonArray).orEmpty()
                            .mapNotNull { el -> (el as? JsonPrimitive)?.contentOrNull },
                        defaultModel = body["default"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    )
                }
            }
            // 目录拉取失败不阻塞 Key 列表（Key 管理不依赖目录）
        }
    }

    /** Key 今天是否可用：启用了，并且是今天启用的（管理员豁免）。 */
    fun isActiveToday(k: GatewayKey): Boolean {
        if (!k.enabled) return false
        if (adminExempt) return true
        return k.activeDate == LocalDate.now().toString()
    }

    // ---- 新增 ----

    fun openSheet() = _ui.update { it.copy(sheetOpen = true, formModel = GatewayUiState.AUTO_MODEL) }
    fun closeSheet() = _ui.update { it.copy(sheetOpen = false) }
    fun onFormModelChange(model: String) = _ui.update { it.copy(formModel = model) }

    fun addKey(name: String) {
        val s = _ui.value
        if (s.submitting) return
        _ui.update { it.copy(submitting = true) }
        val payload = buildJsonObject {
            put("name", name.trim())
            put("model", s.formModel)
        }
        viewModelScope.launch {
            try {
                val body = container.apiClient.postBody("/gateway/keys/add", payload)
                // 明文只返回这一次 → 直接弹窗展示，方便复制
                val plain = body["apiKey"]?.jsonPrimitive?.contentOrNull
                _ui.update { it.copy(submitting = false, sheetOpen = false, revealed = plain) }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(submitting = false) }
                showError(e)
            }
        }
    }

    // ---- 启停 / 删除 / 查看明文 ----

    fun toggleKey(k: GatewayKey) {
        val target = !k.enabled
        action(
            path = "/gateway/keys/toggle",
            payload = buildJsonObject { put("id", k.id); put("enabled", target) },
            message = if (target) "已启用（仅今天有效）" else "已停用",
            reload = true,
        )
    }

    fun deleteKey(id: Long) = action(
        path = "/gateway/keys/delete",
        payload = buildJsonObject { put("id", id) },
        message = "已删除",
        reload = true,
    )

    /**
     * 查看明文：服务端 `result.setBody(raw)`，`body` 是**字符串**而非对象，
     * [com.cyz.wxyd.data.remote.ApiClient.postBody] 只保留对象型 body，故走 [ApiClient.postBodyRawString]
     * + [ApiResult.bodyStringOrNull] 取裸字符串。
     */
    fun revealKey(id: Long) {
        viewModelScope.launch {
            try {
                val raw = container.apiClient.postBodyRawString("/gateway/keys/reveal", buildJsonObject { put("id", id) })
                val plain = ApiResult.bodyStringOrNull(raw).orEmpty()
                _ui.update {
                    it.copy(revealed = plain.ifBlank { "（服务端未返回明文，请稍后重试）" })
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun dismissReveal() = _ui.update { it.copy(revealed = null) }
    fun consumeMessage() = _ui.update { it.copy(message = null) }
    fun consumeError() = _ui.update { it.copy(error = null) }

    private fun action(path: String, payload: JsonObject, message: String, reload: Boolean) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody(path, payload)
                _ui.update { it.copy(message = message) }
                if (reload) refresh()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private fun showError(e: Exception) = _ui.update { it.copy(message = e.userMessage()) }
}

/** 解析 `gateway/keys` 的 `body.list` 数组。 */
internal fun parseKeys(array: JsonArray): List<GatewayKey> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    GatewayKey(
        id = id,
        name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { "Key#$id" },
        model = obj["model"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull == true,
        activeDate = obj["activeDate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        maskedKey = obj["maskedKey"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        createTime = obj["createTime"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        lastUsedTime = obj["lastUsedTime"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}
