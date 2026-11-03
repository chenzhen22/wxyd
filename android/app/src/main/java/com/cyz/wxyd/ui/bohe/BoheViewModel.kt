package com.cyz.wxyd.ui.bohe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.repo.BoheRepository
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** 账单行（本地 Room 或服务端，金额为分；id 负数 = 本机离线新建未同步）。 */
data class BoheBillItem(
    val id: Long,
    val amount: Long,
    val category: String,
    val note: String?,
    val billDate: String,
    val cloudId: String? = null,
) {
    val isLocalOnly: Boolean get() = id < 0
}

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

/** 搜索到的用户（好友 Tab 顶部搜索）。 */
data class BoheSearchUser(
    val userId: Long,
    val username: String?,
    val displayName: String?,
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

/** 统计粒度（需求 2：按年/月/日统计）。 */
enum class StatsGranularity(val key: String, val label: String) {
    DAY("day", "日"),
    MONTH("month", "月"),
    YEAR("year", "年");

    /** 当前选中期的服务端参数值：day=yyyy-MM-dd / month=yyyy-MM / year=yyyy。 */
    fun periodValue(month: String): String = when (this) {
        DAY -> month.take(10)
        MONTH -> month.take(7)
        YEAR -> month.take(4)
    }
}

data class BoheUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,

    /** 当前选中期（yyyy-MM-dd 锚点，按粒度截取展示）。 */
    val month: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + "-01",
    val granularity: StatsGranularity = StatsGranularity.MONTH,

    /** 好友查看模式：null=我；非 null=对方 userId */
    val viewingFriendId: Long? = null,
    val viewingFriendName: String? = null,

    val bills: List<BoheBillItem> = emptyList(),
    val monthTotal: Long = 0,
    val byCategory: List<CategorySum> = emptyList(),

    val friends: List<BoheFriendItem> = emptyList(),
    /** 好友 Tab 搜索。 */
    val searchKeyword: String = "",
    val searchResults: List<BoheSearchUser> = emptyList(),
    val searching: Boolean = false,

    /** 新增/编辑弹层。editBill 非 null = 编辑模式。 */
    val showAddSheet: Boolean = false,
    val editBill: BoheBillItem? = null,
    val saving: Boolean = false,
)

/**
 * 薄荷账（离线优先，需求 6）：读写走 [BoheRepository]（本地 Room），
 * 联网时刷新同步。好友/搜索仍走服务端。
 */
class BoheViewModel(private val container: AppContainer) : ViewModel() {

    private val repo: BoheRepository = container.boheRepository
    private val api get() = container.apiClient

    private val _ui = MutableStateFlow(BoheUiState())
    val ui: StateFlow<BoheUiState> = _ui.asStateFlow()

    fun consumeMessage() = _ui.update { it.copy(message = null) }

    fun importDone(msg: String) {
        _ui.update { it.copy(message = msg) }
    }

    // ---------- 期切换 ----------

    private fun fmt(p: StatsGranularity): DateTimeFormatter = when (p) {
        StatsGranularity.DAY -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
        StatsGranularity.MONTH -> DateTimeFormatter.ofPattern("yyyy-MM")
        StatsGranularity.YEAR -> DateTimeFormatter.ofPattern("yyyy")
    }

    fun setGranularity(g: StatsGranularity) {
        val anchor = LocalDate.parse(_ui.value.month.take(10))
        _ui.update { it.copy(granularity = g, month = anchor.format(fmt(g))) }
        load()
    }

    fun setMonth(m: String) {
        _ui.update { it.copy(month = m) }
        load()
    }

    fun prevPeriod() = shift(-1)
    fun nextPeriod() = shift(1)

    private fun shift(delta: Long) {
        val s = _ui.value
        val anchor = LocalDate.parse(s.month.take(10))
        val moved = when (s.granularity) {
            StatsGranularity.DAY -> anchor.plusDays(delta)
            StatsGranularity.MONTH -> anchor.plusMonths(delta)
            StatsGranularity.YEAR -> anchor.plusYears(delta)
        }
        setMonth(moved.format(fmt(s.granularity)))
    }

    // ---------- 数据加载 ----------

    /** 联网刷新 + 重放同步；本地 Room 为单一 UI 数据源。 */
    fun load() {
        val s = _ui.value
        _ui.update { it.copy(loading = it.bills.isEmpty()) }
        viewModelScope.launch {
            try {
                // 服务器数据拉取与同步（离线静默）
                launch { repo.refreshAndSync(s.granularity.periodValue(s.month)) }
                // 好友列表（联网才拉得到；失败静默保持旧值）
                if (s.viewingFriendId == null) {
                    launch { runCatching { loadFriends() } }
                }
            } finally {
                _ui.update { it.copy(loading = false) }
            }
        }
        // 好友模式：直接走服务端（好友账单不进本地库）
        if (s.viewingFriendId != null) {
            viewModelScope.launch { loadFriendBills(s.viewingFriendId, s.granularity, s.month) }
        }
    }

    private suspend fun loadFriendBills(friendId: Long, g: StatsGranularity, month: String) {
        try {
            val query = mapOf("month" to g.periodValue(month), "period" to g.key, "friendId" to friendId.toString())
            val billBody = api.getResult("/bohe/bill/list", query)
            val statsBody = api.getResult("/bohe/stats", query)
            val bills = parseBills(billBody)
            val total = statsBody["total"]?.jsonPrimitive?.longOrNull ?: 0L
            val byCategory = parseCategorySums(statsBody)
            _ui.update {
                it.copy(loading = false, bills = bills, monthTotal = total, byCategory = byCategory, error = null)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _ui.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    private suspend fun loadFriends() {
        val body = api.getResult("/bohe/friend/list")
        val arr = body["friends"]?.jsonArray ?: return
        val friends = arr.map { el ->
            val o = el.jsonObject
            BoheFriendItem(
                userId = o["userId"]?.jsonPrimitive?.longOrNull ?: 0L,
                username = o["username"]?.jsonPrimitive?.contentOrNull,
                displayName = o["displayName"]?.jsonPrimitive?.contentOrNull,
                status = o["status"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0,
                outgoing = o["outgoing"]?.jsonPrimitive?.contentOrNull == "true",
            )
        }
        _ui.update { it.copy(friends = friends) }
    }

    private fun parseBills(body: JsonObject): List<BoheBillItem> {
        val arr = body["bills"]?.jsonArray ?: return emptyList()
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
        val arr = body["byCategory"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.jsonObject
            val c = o["category"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CategorySum(c, o["total"]?.jsonPrimitive?.longOrNull ?: 0L)
        }
    }

    // ---------- 写（离线可用） ----------

    fun showAddSheet() = _ui.update { it.copy(showAddSheet = true, editBill = null) }

    fun showEditSheet(bill: BoheBillItem) = _ui.update { it.copy(showAddSheet = true, editBill = bill) }

    fun dismissAddSheet() = _ui.update { it.copy(showAddSheet = false, editBill = null, saving = false) }

    /** 记一笔/编辑（本地立即生效，联网自动同步）。 */
    fun saveBill(amountYuan: String, category: String, note: String, date: LocalDate) {
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
                val editing = _ui.value.editBill
                if (editing != null) {
                    repo.editBill(editing.id, cents, category, note.takeIf { it.isNotBlank() }, date.toString())
                } else {
                    repo.addBill(cents, category, note.takeIf { it.isNotBlank() }, date.toString())
                }
                _ui.update { it.copy(saving = false, showAddSheet = false, editBill = null, message = "已保存") }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(saving = false, message = e.userMessage()) }
            }
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteBill(id)
                _ui.update { it.copy(message = "已删除") }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    // ---------- 好友 Tab ----------

    fun onSearchChange(v: String) {
        _ui.update { it.copy(searchKeyword = v) }
        if (v.isBlank()) _ui.update { it.copy(searchResults = emptyList()) }
    }

    /** 模糊搜索用户（需求 3：上方搜索框加好友）。 */
    fun search() {
        val kw = _ui.value.searchKeyword.trim()
        if (kw.isEmpty()) return
        _ui.update { it.copy(searching = true) }
        viewModelScope.launch {
            try {
                val body = api.getResult("/bohe/user/search", mapOf("keyword" to kw))
                val arr = body["users"]?.jsonArray ?: emptyList()
                val users = arr.mapNotNull { el ->
                    val o = el.jsonObject
                    val id = o["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    BoheSearchUser(
                        userId = id,
                        username = o["username"]?.jsonPrimitive?.contentOrNull,
                        displayName = o["displayName"]?.jsonPrimitive?.contentOrNull,
                    )
                }
                _ui.update { it.copy(searching = false, searchResults = users) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(searching = false, message = e.userMessage()) }
            }
        }
    }

    /** 对搜索结果发好友申请（通过=互相关注）。 */
    fun addFriend(userId: Long) {
        viewModelScope.launch {
            try {
                val user = _ui.value.searchResults.firstOrNull { it.userId == userId }
                api.postBody(
                    "/bohe/friend/request",
                    kotlinx.serialization.json.buildJsonObject {
                        put("username", kotlinx.serialization.json.JsonPrimitive(user?.username ?: ""))
                    },
                )
                _ui.update { it.copy(message = "申请已发送") }
                runCatching { loadFriends() }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun handleFriend(userId: Long, accept: Boolean) {
        viewModelScope.launch {
            try {
                api.postBody(
                    "/bohe/friend/handle",
                    kotlinx.serialization.json.buildJsonObject {
                        put("userId", kotlinx.serialization.json.JsonPrimitive(userId))
                        put("accept", kotlinx.serialization.json.JsonPrimitive(accept))
                    },
                )
                _ui.update { it.copy(message = if (accept) "已添加好友" else "已拒绝") }
                runCatching { loadFriends() }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun removeFriend(userId: Long) {
        viewModelScope.launch {
            try {
                api.postBody(
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

    /** 进入好友只读视图（好友 Tab 点好友 → 薄荷账）。 */
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
