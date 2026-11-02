package com.cyz.wxyd.ui.bohe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 账单行（服务端 BoheBill，金额为分）。 */
data class BoheBillItem(
    val id: Long,
    val amount: Long,
    val category: String,
    val note: String?,
    val billDate: String,
    val cloudId: String?,
)

/** 分类合计行。 */
data class CategorySum(val category: String, val total: Long)

/** 好友行。status: 0=待通过 1=已通过 2=已拒绝/删除；outgoing=true 表示我发起的。 */
data class BoheFriendItem(
    val userId: Long,
    val username: String?,
    val displayName: String?,
    val status: Int,
    val outgoing: Boolean,
) {
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: username ?: "$userId"
}

/** 8 内置分类（与小程序 food/transport/shop/house/fun/medical/study/other 对齐）。 */
object BoheCategories {
    val LIST = listOf(
        "food" to "餐饮",
        "transport" to "交通",
        "shop" to "购物",
        "house" to "居住",
        "fun" to "娱乐",
        "medical" to "医疗",
        "study" to "教育",
        "other" to "其他",
    )

    private val MAP = LIST.toMap()

    /** 分类 key → 中文名；未知分类（导入的历史数据）兜底"其他"。 */
    fun label(key: String?): String = MAP[key] ?: "其他"
}

data class BoheUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,

    /** 当前查看的月份 yyyy-MM */
    val month: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),

    /** 好友查看模式：null=我；非 null=对方 userId */
    val viewingFriendId: Long? = null,
    val viewingFriendName: String? = null,

    val bills: List<BoheBillItem> = emptyList(),
    val monthTotal: Long = 0,
    val byCategory: List<CategorySum> = emptyList(),

    val friends: List<BoheFriendItem> = emptyList(),
    val friendKeyword: String = "",

    val showAddDialog: Boolean = false,
    val saving: Boolean = false,
)

/**
 * 薄荷账（spec docs/superpowers/specs/2026-11-02-bohe-android-integration-design.md）。
 *
 * 接口：bohe/bill/list、bohe/bill/save、bohe/bill/delete、bohe/stats、
 * bohe/friend/list、bohe/friend/request、bohe/friend/handle、bohe/friend/remove、bohe/user/search。
 * 金额传输全部为整数分；展示层才转元。
 */
class BoheViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(BoheUiState())
    val ui: StateFlow<BoheUiState> = _ui.asStateFlow()

    fun consumeMessage() = _ui.update { it.copy(message = null) }

    /** 导入卡片回调：展示结果文本（含 inserted/updated/skipped 时由调用方拼好）。 */
    fun importDone(msg: String) {
        _ui.update { it.copy(message = msg) }
    }

    fun setMonth(m: String) {
        _ui.update { it.copy(month = m) }
        load()
    }

    fun prevMonth() {
        val d = LocalDate.parse(_ui.value.month + "-01").minusMonths(1)
        setMonth(d.format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }

    fun nextMonth() {
        val d = LocalDate.parse(_ui.value.month + "-01").plusMonths(1)
        setMonth(d.format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }

    /** 拉账单 + 统计 + 好友列表（并发三个请求，全部完成后落状态）。 */
    fun load() {
        val s = _ui.value
        val friendId = s.viewingFriendId
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val query = buildMap {
                    put("month", s.month)
                    if (friendId != null) put("friendId", friendId.toString())
                }
                val billBody = container.apiClient.getResult("/bohe/bill/list", query)
                val statsBody = container.apiClient.getResult("/bohe/stats", query)
                val bills = parseBills(billBody)
                val total = statsBody["body"]?.jsonObject?.get("total")?.jsonPrimitive?.longOrNull ?: 0L
                val byCategory = parseCategorySums(statsBody)
                val friends = if (friendId == null) loadFriends() else _ui.value.friends
                _ui.update {
                    it.copy(
                        loading = false, bills = bills, monthTotal = total, byCategory = byCategory,
                        friends = friends, error = null,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    private suspend fun loadFriends(): List<BoheFriendItem> {
        val body = container.apiClient.getResult("/bohe/friend/list")
        val arr = body["body"]?.jsonObject?.get("friends")?.jsonArray ?: return emptyList()
        return arr.map { el ->
            val o = el.jsonObject
            BoheFriendItem(
                userId = o["userId"]?.jsonPrimitive?.longOrNull ?: 0L,
                username = o["username"]?.jsonPrimitive?.contentOrNull,
                displayName = o["displayName"]?.jsonPrimitive?.contentOrNull,
                status = o["status"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0,
                outgoing = o["outgoing"]?.jsonPrimitive?.contentOrNull == "true",
            )
        }
    }

    private fun parseBills(body: JsonObject): List<BoheBillItem> {
        val arr = body["body"]?.jsonObject?.get("bills")?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.jsonObject
            val id = o["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            BoheBillItem(
                id = id,
                amount = o["amount"]?.jsonPrimitive?.longOrNull ?: 0L,
                category = o["category"]?.jsonPrimitive?.contentOrNull ?: "other",
                note = o["note"]?.jsonPrimitive?.contentOrNull,
                billDate = o["billDate"]?.jsonPrimitive?.contentOrNull ?: "",
                cloudId = o["cloudId"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    private fun parseCategorySums(body: JsonObject): List<CategorySum> {
        val arr = body["body"]?.jsonObject?.get("byCategory")?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.jsonObject
            val c = o["category"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CategorySum(c, o["total"]?.jsonPrimitive?.longOrNull ?: 0L)
        }
    }

    fun showAddDialog() = _ui.update { it.copy(showAddDialog = true) }

    fun dismissAddDialog() = _ui.update { it.copy(showAddDialog = false, saving = false) }

    /** 记一笔：成功后刷新并收起弹窗（回到账单卡片）。 */
    fun addBill(amountYuan: String, category: String, note: String, date: LocalDate) {
        val cents = amountYuan.toBigDecimalOrNull()
            ?.multiply(java.math.BigDecimal(100))
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.longValueExact()
        if (cents == null || cents <= 0) {
            _ui.update { it.copy(message = "金额无效") }
            return
        }
        if (category.isBlank()) {
            _ui.update { it.copy(message = "请选择分类") }
            return
        }
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/bohe/bill/save",
                    kotlinx.serialization.json.buildJsonObject {
                        put("amount", kotlinx.serialization.json.JsonPrimitive(cents))
                        put("category", kotlinx.serialization.json.JsonPrimitive(category))
                        put("note", kotlinx.serialization.json.JsonPrimitive(note))
                        put("date", kotlinx.serialization.json.JsonPrimitive(date.toString()))
                    },
                )
                _ui.update {
                    it.copy(saving = false, showAddDialog = false, message = "记账成功")
                }
                load()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(saving = false, message = e.userMessage()) }
            }
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/bohe/bill/delete",
                    kotlinx.serialization.json.buildJsonObject {
                        put("id", kotlinx.serialization.json.JsonPrimitive(id))
                    },
                )
                _ui.update { it.copy(message = "已删除") }
                load()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun onFriendKeywordChange(v: String) = _ui.update { it.copy(friendKeyword = v) }

    fun requestFriend() {
        val username = _ui.value.friendKeyword.trim()
        if (username.isBlank()) {
            _ui.update { it.copy(message = "请输入用户名") }
            return
        }
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/bohe/friend/request",
                    kotlinx.serialization.json.buildJsonObject {
                        put("username", kotlinx.serialization.json.JsonPrimitive(username))
                    },
                )
                _ui.update { it.copy(friendKeyword = "", message = "申请已发送") }
                load()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun handleFriend(userId: Long, accept: Boolean) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/bohe/friend/handle",
                    kotlinx.serialization.json.buildJsonObject {
                        put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                        put("accept", kotlinx.serialization.json.JsonPrimitive(accept))
                    },
                )
                load()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun removeFriend(userId: Long) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody(
                    "/bohe/friend/remove",
                    kotlinx.serialization.json.buildJsonObject {
                        put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                    },
                )
                if (_ui.value.viewingFriendId == userId) backToMe()
                else load()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    /** 进入好友只读视图。 */
    fun openFriend(f: BoheFriendItem) {
        if (f.status != 1) return
        _ui.update { it.copy(viewingFriendId = f.userId, viewingFriendName = f.label) }
        load()
    }

    fun backToMe() {
        _ui.update { it.copy(viewingFriendId = null, viewingFriendName = null) }
        load()
    }
}
