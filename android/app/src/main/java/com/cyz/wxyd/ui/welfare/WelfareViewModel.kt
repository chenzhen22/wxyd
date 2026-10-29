package com.cyz.wxyd.ui.welfare

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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** 福利列表项（`GET /welfare/list` → `[{id,title,summary,firstImage,publishedAt}]`）。 */
data class WelfarePost(
    val id: Long,
    val title: String,
    val summary: String,
    val firstImage: String?,
    val publishedAt: String,
)

/** 福利详情（`GET /welfare/detail` → `{id,title,publishedAt,contentMd,images}`）。 */
data class WelfareDetail(
    val id: Long,
    val title: String,
    val publishedAt: String,
    val contentMd: String,
    /** `[{url,webUrl,alt,width,height}]`，只含已落地本地的图；[url] 为相对路径 */
    val images: List<WelfareImage>,
) {
    /** 正文里的图片引用换成服务端提供的本地图 URL（H5 `welfare-md.js` 同思路的简化版）。 */
    fun resolveMarkdown(): String {
        if (images.isEmpty()) return contentMd
        var md = contentMd
        images.forEach { img ->
            if (img.webUrl.isNotBlank()) md = md.replace(img.url, img.webUrl)
        }
        return md
    }
}

data class WelfareImage(val url: String, val webUrl: String, val alt: String)

/** 羊毛雷达订阅（`GET /radar/list` → `RadarSubscribe` 列表）。 */
data class RadarSub(
    val id: Long,
    val keyword: String,
    /** `dingtalk` | `email` */
    val channel: String,
    val robotId: Long?,
    val email: String,
    val enabled: Boolean,
    val createTime: String,
)

data class RadarUiState(
    val loading: Boolean = true,
    val subs: List<RadarSub> = emptyList(),
    val robots: List<RobotItem> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

data class WelfareUiState(
    /** 第一页是否在加载（首屏 / 搜索 / 刷新） */
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val posts: List<WelfarePost> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val keyword: String = "",
    val error: String? = null,
    val detail: WelfareDetail? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
    val radar: RadarUiState = RadarUiState(),
)

/**
 * 福利专区（计划 Task 8）。
 *
 * - 列表：`GET /welfare/list?page&size&keyword`，body 是数组；`list.size < size` 即到底（H5 同判据）
 * - 详情：`GET /welfare/detail?id`，Markdown 正文
 * - 羊毛雷达：`/radar/list`（GET，响应仍按统一 Result 解析）与 `/radar/{add,delete,toggle,test}`
 *   全部 `postBody` 双层 `{body:{...}}`（H5 `postJSON`），是 spec 三层请求体分类的第一类
 */
class WelfareViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(WelfareUiState(loading = true))
    val ui: StateFlow<WelfareUiState> = _ui.asStateFlow()

    private var listJob: Job? = null

    init {
        refresh()
        // 雷达是页内区块：进入福利页即预取，展开"管理"时数据已在
        loadRadar()
    }

    // ---- 列表 ----

    /** 下拉刷新 / 首次加载 / 搜索提交：清空回第一页。 */
    fun refresh(keyword: String = _ui.value.keyword) {
        listJob?.cancel()
        _ui.update { it.copy(keyword = keyword, loading = true, error = null) }
        listJob = viewModelScope.launch {
            try {
                val page = fetchPage(1, keyword)
                _ui.update {
                    it.copy(
                        loading = false,
                        posts = page,
                        page = 1,
                        hasMore = page.size >= PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun onKeywordChange(v: String) = _ui.update { it.copy(keyword = v) }

    /** 触底加载下一页。 */
    fun loadMore() {
        val s = _ui.value
        if (s.loading || s.loadingMore || !s.hasMore) return
        _ui.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            try {
                val nextPage = s.page + 1
                val page = fetchPage(nextPage, s.keyword)
                _ui.update {
                    it.copy(
                        loadingMore = false,
                        posts = it.posts + page,
                        page = nextPage,
                        hasMore = page.size >= PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loadingMore = false, error = e.userMessage()) }
            }
        }
    }

    fun consumeError() = _ui.update { it.copy(error = null) }

    private suspend fun fetchPage(page: Int, keyword: String): List<WelfarePost> {
        val query = buildMap {
            put("page", page.toString())
            put("size", PAGE_SIZE.toString())
            if (keyword.isNotBlank()) put("keyword", keyword.trim())
        }
        val raw = container.apiClient.getRaw("/welfare/list", query).use { it.body?.string().orEmpty() }
        return parsePosts(ApiResult.parseArray(raw))
    }

    // ---- 详情 ----

    fun openDetail(id: Long) {
        _ui.update { it.copy(detailLoading = true, detailError = null, detail = null) }
        viewModelScope.launch {
            try {
                val body = container.apiClient.getResult("/welfare/detail", mapOf("id" to id.toString()))
                _ui.update { it.copy(detailLoading = false, detail = parseDetail(body)) }
            } catch (e: Exception) {
                _ui.update { it.copy(detailLoading = false, detailError = e.userMessage()) }
            }
        }
    }

    fun closeDetail() = _ui.update { it.copy(detail = null, detailError = null) }

    // ---- 羊毛雷达 ----

    fun loadRadar() {
        _ui.update { it.copy(radar = it.radar.copy(loading = true, error = null)) }
        viewModelScope.launch {
            try {
                val raw = container.apiClient.getRaw("/radar/list").use { it.body?.string().orEmpty() }
                val subs = parseRadarSubs(ApiResult.parseArray(raw))
                // 钉钉通道要选机器人：与 H5 一样顺带拉 robot/list（失败不阻塞订阅列表）
                val robots = runCatching {
                    val raw = container.apiClient.getRaw("/robot/list").use { it.body?.string().orEmpty() }
                    parseRobots(ApiResult.parseArray(raw))
                }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(radar = it.radar.copy(loading = false, subs = subs, robots = robots))
                }
            } catch (e: Exception) {
                _ui.update { it.copy(radar = it.radar.copy(loading = false, error = e.userMessage())) }
            }
        }
    }

    /**
     * 新增订阅：钉钉通道传 `robotId`，邮件通道传 `email`（服务端二选一校验）。
     * 成功文案由调用方展示（"已添加"），失败进 [RadarUiState.error]。
     */
    fun addRadar(keyword: String, channel: String, robotId: Long?, email: String) {
        val payload = buildJsonObject {
            put("keyword", keyword.trim())
            put("channel", channel)
            if (channel == "dingtalk") robotId?.let { put("robotId", it) }
            else put("email", email.trim())
        }
        radarAction("/radar/add", payload, reload = true, successMessage = "已添加")
    }

    fun deleteRadar(id: Long) =
        radarAction("/radar/delete", buildJsonObject { put("id", id) }, reload = true, successMessage = "已删除")

    fun toggleRadar(id: Long, enabled: Boolean) =
        radarAction(
            "/radar/toggle",
            buildJsonObject { put("id", id); put("enabled", enabled) },
            reload = true,
            successMessage = if (enabled) "已启用" else "已暂停",
        )

    /** 测试推送：成功后 body 是一段文案（`推送成功，请查收`），作为提示展示。 */
    fun testRadar(id: Long) =
        radarAction("/radar/test", buildJsonObject { put("id", id) }, reload = false, successMessage = "已推送")

    private fun radarAction(
        path: String,
        payload: JsonObject,
        reload: Boolean,
        successMessage: String,
    ) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody(path, payload)
                _ui.update { it.copy(radar = it.radar.copy(error = null, message = successMessage)) }
                if (reload) loadRadar()
            } catch (e: Exception) {
                _ui.update { it.copy(radar = it.radar.copy(error = e.userMessage())) }
            }
        }
    }

    fun consumeRadarError() = _ui.update { it.copy(radar = it.radar.copy(error = null)) }

    fun consumeRadarMessage() = _ui.update { it.copy(radar = it.radar.copy(message = null)) }

    companion object {
        const val PAGE_SIZE = 20
    }
}

/** 解析 `welfare/list` 数组。 */
internal fun parsePosts(array: JsonArray): List<WelfarePost> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    WelfarePost(
        id = id,
        title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        summary = obj["summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        firstImage = obj["firstImage"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
        publishedAt = obj["publishedAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}

/** 解析 `welfare/detail` 的 body。 */
internal fun parseDetail(body: JsonObject): WelfareDetail {
    val images = (body["images"] as? JsonArray).orEmpty().mapNotNull { el ->
        val obj = el as? JsonObject ?: return@mapNotNull null
        WelfareImage(
            url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            webUrl = obj["webUrl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            alt = obj["alt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }
    return WelfareDetail(
        id = body["id"]?.jsonPrimitive?.longOrNull ?: 0L,
        title = body["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        publishedAt = body["publishedAt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        contentMd = body["contentMd"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        images = images,
    )
}

/**
 * 解析 `radar/list`：body 是 `RadarSubscribe` 数组。
 *
 * 走的是 `getResult`（返回 body 对象），数组型 body 会被 [ApiResult.parse] 丢成 null，
 * 所以这里改走 `getRaw` + [ApiResult.parseArray]，保证与统一响应格式对齐。
 */
internal fun parseRadarSubs(body: JsonArray): List<RadarSub> = body.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    RadarSub(
        id = id,
        keyword = obj["keyword"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        channel = obj["channel"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        robotId = obj["robotId"]?.jsonPrimitive?.longOrNull,
        email = obj["email"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        // 服务端 RadarSubscribe.enabled 是 Integer（1/0），不是 Boolean
        enabled = when {
            obj["enabled"]?.jsonPrimitive?.booleanOrNull == true -> true
            (obj["enabled"]?.jsonPrimitive?.intOrNull ?: 0) == 1 -> true
            else -> false
        },
        createTime = obj["createTime"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}
