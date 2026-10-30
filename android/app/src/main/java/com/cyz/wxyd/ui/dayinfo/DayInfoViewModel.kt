package com.cyz.wxyd.ui.dayinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiResult
import com.cyz.wxyd.ui.common.userMessage
import com.cyz.wxyd.ui.robot.RobotItem
import com.cyz.wxyd.ui.robot.parseRobots
import kotlinx.coroutines.Job
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

/** 日报列表项（`GET /dayinfo/list` → `body.list[{id, infoDate, title, brief, ...}]`，date-desc）。 */
data class DayInfoItem(
    val id: Long,
    val infoDate: String,
    val title: String,
    val brief: String,
)

/** 日报详情（`GET /dayinfo/detail?id` → body；Markdown 正文）。 */
data class DayInfoDetail(
    val id: Long,
    val infoDate: String,
    val title: String,
    /** 📌 今日要点（H5 同名字段），可空，空串不展示 */
    val brief: String,
    /** Markdown 正文，经 [MarkdownView] 渲染 */
    val content: String,
)

/** 订阅提醒（`GET /dayinfo/subscribe/my` → body 数组）。 */
data class DayInfoSub(
    val id: Long,
    /** `dingtalk` | `email` */
    val channel: String,
    val robotId: Long?,
    val email: String,
    /** 服务端存 Integer（1/0），true=启用 */
    val enabled: Boolean,
    val createTime: String,
)

data class DayInfoUiState(
    val loading: Boolean = true,
    /** 列表（date-desc，首条即最新一期） */
    val items: List<DayInfoItem> = emptyList(),
    val error: String? = null,

    /** 当前选中的期次（默认最新一期 = items[0]） */
    val selectedId: Long? = null,
    val detail: DayInfoDetail? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,

    /** 订阅管理区块（ModalBottomSheet 内） */
    val subSheetOpen: Boolean = false,
    val subsLoading: Boolean = false,
    val subs: List<DayInfoSub> = emptyList(),
    val robots: List<RobotItem> = emptyList(),
    val subMessage: String? = null,
    val subError: String? = null,
)

/**
 * 金融日报模块（Task 9）。
 *
 * - 列表：`GET /dayinfo/list?pageNum=1&pageSize=20`，body `{list:[...], total}`（date-desc）；
 *   首条即"最新一期"，作为默认展示（H5 同样取 `list[0]`）。
 * - 详情：`GET /dayinfo/detail?id`，Markdown 正文；可选 `brief` 渲染为"📌 今日要点"callout。
 * - 订阅管理：`/dayinfo/subscribe/my`(GET 数组) + `/dayinfo/subscribe/{add,delete,toggle,test}`
 *   （全部 postBody 双层 `{body:{...}}`，与 H5 `postJSON` 对齐）。
 *
 * 注：`/dayinfo/latest` 服务端存在但 H5 从不调用（首条即最新），本模块不单独请求 latest，
 * 直接用 list 首条，避免凭空造响应结构。
 */
class DayInfoViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(DayInfoUiState(loading = true))
    val ui: StateFlow<DayInfoUiState> = _ui.asStateFlow()

    private var listJob: Job? = null

    init {
        refresh()
    }

    // ---- 列表 + 详情 ----

    fun refresh() {
        listJob?.cancel()
        _ui.update { it.copy(loading = true, error = null) }
        listJob = viewModelScope.launch {
            try {
                val query = mapOf("pageNum" to "1", "pageSize" to PAGE_SIZE.toString())
                val raw = container.apiClient.getRaw("/dayinfo/list", query).use { it.body?.string().orEmpty() }
                val body = ApiResult.parse(raw).body
                    ?: throw com.cyz.wxyd.data.remote.ApiException("-1", "响应缺少 body")
                val list = parseItems(body["list"] as? JsonArray ?: JsonArray(emptyList()))
                val firstId = list.firstOrNull()?.id
                _ui.update {
                    it.copy(
                        loading = false,
                        items = list,
                        error = null,
                        selectedId = firstId,
                    )
                }
                firstId?.let { id -> loadDetail(id) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun selectIssue(id: Long) {
        if (_ui.value.selectedId == id) return
        _ui.update { it.copy(selectedId = id, detail = null, detailError = null) }
        loadDetail(id)
    }

    private fun loadDetail(id: Long) {
        _ui.update { it.copy(detailLoading = true, detailError = null, detail = null) }
        viewModelScope.launch {
            try {
                val body = container.apiClient.getResult("/dayinfo/detail", mapOf("id" to id.toString()))
                _ui.update { it.copy(detailLoading = false, detail = parseDetail(body)) }
            } catch (e: Exception) {
                _ui.update { it.copy(detailLoading = false, detailError = e.userMessage()) }
            }
        }
    }

    fun consumeError() = _ui.update { it.copy(error = null) }

    // ---- 订阅管理 ----

    fun openSubSheet() {
        _ui.update { it.copy(subSheetOpen = true, subError = null) }
        loadSubs()
    }

    fun closeSubSheet() = _ui.update { it.copy(subSheetOpen = false) }

    fun loadSubs() {
        _ui.update { it.copy(subsLoading = true, subError = null) }
        viewModelScope.launch {
            try {
                val raw = container.apiClient.getRaw("/dayinfo/subscribe/my").use { it.body?.string().orEmpty() }
                val subs = parseSubs(ApiResult.parseArray(raw))
                // 钉钉通道要选机器人：顺带拉 /robot/list（失败不阻塞订阅列表）
                val robots = runCatching {
                    val raw = container.apiClient.getRaw("/robot/list").use { it.body?.string().orEmpty() }
                    parseRobots(ApiResult.parseArray(raw))
                }.getOrDefault(emptyList())
                _ui.update { it.copy(subsLoading = false, subs = subs, robots = robots) }
            } catch (e: Exception) {
                _ui.update { it.copy(subsLoading = false, subError = e.userMessage()) }
            }
        }
    }

    /**
     * 新增订阅：dingtalk 传 `robotId`，email 传 `email`。
     * 空机器人 + dingtalk 调用方先拦截提示（"请先添加钉钉机器人"）。
     */
    fun addSub(channel: String, robotId: Long?, email: String) {
        val payload = buildJsonObject {
            put("channel", channel)
            if (channel == "dingtalk") robotId?.let { put("robotId", it) }
            else put("email", email.trim())
        }
        subAction("/dayinfo/subscribe/add", payload, reload = true, successMessage = "已添加")
    }

    fun deleteSub(id: Long) =
        subAction("/dayinfo/subscribe/delete", buildJsonObject { put("id", id) }, reload = true, successMessage = "已删除")

    fun toggleSub(id: Long, enabled: Boolean) =
        subAction(
            "/dayinfo/subscribe/toggle",
            buildJsonObject { put("id", id); put("enabled", enabled) },
            reload = true,
            successMessage = if (enabled) "已启用" else "已暂停",
        )

    /**
     * 测试推送：服务端成功时 body 是一段文案串（如"推送成功，请查收"）。
     * 注：[com.cyz.wxyd.data.remote.ApiClient.postBody] 的 `parseResultBody` 仅保留
     * 对象型 body（`as? JsonObject`），字符串 body 会落到空对象，故此处以静态成功文案兜底，
     * 失败仍由 [com.cyz.wxyd.data.remote.ApiException] 透出（如 "暂无日报可测试"）。
     */
    fun testSub(id: Long) =
        subAction("/dayinfo/subscribe/test", buildJsonObject { put("id", id) }, reload = false, useBodyAsMessage = true, successMessage = "已推送")

    private fun subAction(
        path: String,
        payload: JsonObject,
        reload: Boolean,
        successMessage: String,
        useBodyAsMessage: Boolean = false,
    ) {
        viewModelScope.launch {
            try {
                val res = container.apiClient.postBody(path, payload)
                // body 非字符串（对象/缺失）时 jsonPrimitive 会抛异常，用 as? 安全取
                val msg = if (useBodyAsMessage) {
                    (res["body"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: successMessage
                } else successMessage
                _ui.update { it.copy(subError = null, subMessage = msg) }
                if (reload) loadSubs()
            } catch (e: Exception) {
                _ui.update { it.copy(subError = e.userMessage()) }
            }
        }
    }

    fun consumeSubMessage() = _ui.update { it.copy(subMessage = null) }
    fun consumeSubError() = _ui.update { it.copy(subError = null) }

    companion object {
        const val PAGE_SIZE = 20
    }
}

/** 解析 `dayinfo/list` 的 `body.list` 数组。 */
internal fun parseItems(array: JsonArray): List<DayInfoItem> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    DayInfoItem(
        id = id,
        infoDate = obj["infoDate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        brief = obj["brief"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}

/** 解析 `dayinfo/detail` 的 body。 */
internal fun parseDetail(body: JsonObject): DayInfoDetail = DayInfoDetail(
    id = body["id"]?.jsonPrimitive?.longOrNull ?: 0L,
    infoDate = body["infoDate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    title = body["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    brief = body["brief"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    content = body["content"]?.jsonPrimitive?.contentOrNull.orEmpty(),
)

/** 解析 `dayinfo/subscribe/my` 的 body 数组（enabled 为 Integer(1/0)）。 */
internal fun parseSubs(array: JsonArray): List<DayInfoSub> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    DayInfoSub(
        id = id,
        channel = obj["channel"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        robotId = obj["robotId"]?.jsonPrimitive?.longOrNull,
        email = obj["email"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        enabled = when {
            obj["enabled"]?.jsonPrimitive?.booleanOrNull == true -> true
            (obj["enabled"]?.jsonPrimitive?.intOrNull ?: 0) == 1 -> true
            else -> false
        },
        createTime = obj["createTime"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}
