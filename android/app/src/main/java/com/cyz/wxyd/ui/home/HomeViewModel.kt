package com.cyz.wxyd.ui.home

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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 首页卡片（`GET /api/menu/list?platform=h5` 的一行）。
 *
 * 字段名是 **`key`**（服务端 `MenuServiceImpl.list` 把 DB 列 `menu_key` 映射成 `key`），
 * 不是 `menuKey` / `menu_key`（Review Focus #3）。
 *
 * @param hashUrl 非空才出卡片；为空的行（message/group）在 H5 里是底部 tab
 * @param adminOnly 1 = 卡片右上角显示"管理员"角标
 */
data class MenuItem(
    val key: String,
    val name: String,
    val icon: String,
    val hashUrl: String,
    val sortNo: Int,
    val adminOnly: Int,
)

data class HomeUiState(
    val loading: Boolean = true,
    val menus: List<MenuItem> = emptyList(),
    val error: String? = null,
    /** 是否已成功拿到过一次列表（区分"还在加载"与"确实没有卡片"）。 */
    val loaded: Boolean = false,
)

/**
 * 工作台（计划 Task 7 Step 1）。
 *
 * 菜单数据驱动：服务端已按身份过滤（管理员全可见 / 四态 menu_config）并按 sortNo 升序返回，
 * 客户端只做 `hashUrl` 非空过滤（与 H5 `renderHome` 完全一致），不重新排序。
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_ui.value.loading && _ui.value.loaded) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val menus = MenuApi(container.apiClient).list()
                _ui.update { it.copy(loading = false, loaded = true, menus = menus) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun consumeError() = _ui.update { it.copy(error = null) }
}

/**
 * `GET /menu/list?platform=h5` 适配。
 *
 * 统一响应里 `body` 是**数组**（[ApiClient.getResult] 只返回对象型 body，不适用），
 * 所以走 `getRaw` + [ApiResult.parseArray]：请求仍带全局拦截器（`X-Requested-With`、401、Base URL）。
 */
class MenuApi(private val client: ApiClient) {

    suspend fun list(): List<MenuItem> {
        val raw = client.getRaw("/menu/list", mapOf("platform" to "h5")).use { it.body?.string().orEmpty() }
        return parse(ApiResult.parseArray(raw))
    }

    companion object {
        /** 解析数组为卡片列表：`key` 与 `hashUrl` 任一为空的行丢弃（H5 `renderHome` 同条件）。 */
        fun parse(array: JsonArray): List<MenuItem> = array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val key = obj["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val hashUrl = obj["hashUrl"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (key.isEmpty() || hashUrl.isEmpty()) return@mapNotNull null
            MenuItem(
                key = key,
                name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { key },
                icon = obj["icon"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { "•" },
                hashUrl = hashUrl,
                sortNo = obj["sortNo"]?.jsonPrimitive?.intOrNull ?: 0,
                adminOnly = if (obj["adminOnly"]?.jsonPrimitive?.booleanOrNull == true) 1
                else obj["adminOnly"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
    }
}
