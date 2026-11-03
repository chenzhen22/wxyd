package com.cyz.wxyd.data.repo

import android.content.Context
import androidx.room.Room
import com.cyz.wxyd.data.local.BoheBillDao
import com.cyz.wxyd.data.local.BoheBillEntity
import com.cyz.wxyd.data.local.BoheLocalDb
import com.cyz.wxyd.data.local.BoheSyncOpEntity
import com.cyz.wxyd.data.remote.ApiClient
import com.cyz.wxyd.data.remote.ApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * 薄荷账仓库（需求 6 离线优先 + 一致性）。
 *
 * 读：始终走本地 Room（UI 是单一数据源）；联网刷新时服务端全量覆盖本地"已同步"行，
 *     pending 行不动（本地未同步操作是最新意图，不允许被旧服务器数据覆盖）。
 * 写：本地立即落库并记操作日志（op 队列），随后尝试同步；失败静默留队列，联网后重放。
 *
 * 一致性策略（保守）：
 * - add：重放成功用服务端 id 替换负数本地 id；
 * - edit：重放即服务端覆盖（编辑意图是最新值，后写赢）；
 * - delete：**只删本机新建过且尚未同步的行直接删；已同步过的行重放服务端删除接口**
 *   （删除意图明确，不存在误删：操作是用户在看到该行的情况下发起的）；
 * - 重放按 seq 顺序执行，同一行先 add 后 edit 能正确归并。
 */
class BoheRepository(private val dao: BoheBillDao, private val api: ApiClient) {

    private val syncMutex = Mutex()
    private val _syncing = MutableStateFlow(false)
    val syncing: kotlinx.coroutines.flow.StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: kotlinx.coroutines.flow.StateFlow<String?> = _syncError.asStateFlow()

    /** UI 观察：不含 pending delete 行。 */
    fun observeBills(): Flow<List<BoheBillEntity>> = dao.observeAll()

    companion object {
        fun create(context: Context, api: ApiClient): BoheRepository {
            val db = Room.databaseBuilder(context, BoheLocalDb::class.java, "bohe_local.db")
                .fallbackToDestructiveMigration()
                .build()
            return BoheRepository(db.billDao(), api)
        }
    }

    /** 本月（yyyy-MM 前缀）统计：total + 按分类合计，纯本地计算。 */
    suspend fun stats(month: String): Pair<Long, List<Pair<String, Long>>> {
        val bills = dao.all().filter { it.pendingOp != "delete" && it.billDate.startsWith(month) }
        val total = bills.sumOf { it.amount }
        val byCat = bills.groupBy { it.category }
            .map { (c, list) -> c to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
        return total to byCat
    }

    // ---------- 写操作（离线可用） ----------

    /** 记一笔：本地立即落库（负数 id + pending add），入队并尝试同步。 */
    suspend fun addBill(amount: Long, category: String, note: String?, date: String) {
        val localId = (dao.maxId() ?: 0L).let { if (it >= 0) -1L else it - 1 }
        val createTime = System.currentTimeMillis()
        dao.upsert(
            BoheBillEntity(
                id = localId, amount = amount, category = category, note = note,
                billDate = date, createTime = createTime, pendingOp = "add",
            ),
        )
        dao.insertOp(BoheSyncOpEntity(op = "add", localId = localId, amount = amount, category = category, note = note, billDate = date, createTime = createTime))
        sync()
    }

    /** 编辑：本地覆盖 + pending edit 入队（服务器记录的编辑重放后覆盖服务端）。 */
    suspend fun editBill(id: Long, amount: Long, category: String, note: String?, date: String) {
        val old = dao.byId(id) ?: return
        dao.upsert(old.copy(amount = amount, category = category, note = note, billDate = date, pendingOp = if (old.pendingOp == "add") "add" else "edit"))
        // add 未同步过的行：仍是一条 add（带上最新值），无需先 edit
        if (old.pendingOp != "add") {
            dao.insertOp(BoheSyncOpEntity(op = "edit", localId = id, amount = amount, category = category, note = note, billDate = date))
        } else {
            // 更新队列中该 add 的字段：找到最后一条该 localId 的 add 重写
            rewritePendingAdd(id, amount, category, note, date)
        }
        sync()
    }

    private suspend fun rewritePendingAdd(id: Long, amount: Long, category: String, note: String?, date: String) {
        val op = dao.pendingOps().lastOrNull { it.localId == id && it.op == "add" } ?: return
        dao.deleteOp(op.seq)
        dao.insertOp(op.copy(amount = amount, category = category, note = note, billDate = date))
    }

    /** 删除（需求 6：不误删服务器数据——重放服务端删除接口，服务端校验属主）。 */
    suspend fun deleteBill(id: Long) {
        val old = dao.byId(id) ?: return
        if (old.pendingOp == "add") {
            // 本机新建还没同步过：直接本地删 + 撤销 add 操作即可，服务器无此行
            dao.deleteById(id)
            val op = dao.pendingOps().lastOrNull { it.localId == id && it.op == "add" }
            op?.let { dao.deleteOp(it.seq) }
        } else {
            dao.upsert(old.copy(pendingOp = "delete"))
            dao.insertOp(BoheSyncOpEntity(op = "delete", localId = id))
            sync()
        }
    }

    // ---------- 同步 ----------

    /**
     * 刷新服务端 → 覆盖本地已同步行（pending 行保留本地版），然后重放操作队列。
     * 联网失败静默（保持本地数据可用），错误落 [syncError]。
     */
    suspend fun refreshAndSync(month: String) {
        // 先拉服务端全量（按月拉省流量：列表接口自带 month 过滤）
        try {
            val body = api.getResult("/bohe/bill/list", mapOf("month" to month))
            val arr = body["bills"]?.let {
                (it as? kotlinx.serialization.json.JsonArray)
            } ?: emptyList()
            val pendingIds = dao.pendingOps().map { it.localId }.toSet()
            val server = arr.mapNotNull { el ->
                val o = (el as? kotlinx.serialization.json.JsonObject) ?: return@mapNotNull null
                BoheBillEntity(
                    id = o["id"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull } ?: return@mapNotNull null,
                    amount = o["amount"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull } ?: 0L,
                    category = o["category"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull } ?: "other",
                    note = o["note"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull },
                    billDate = o["billDate"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull } ?: "",
                    createTime = o["createTime"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull },
                    pendingOp = null,
                )
            }.filter { it.id !in pendingIds }
            // 服务端行覆盖本地同 id 行；本机新增的负数 id 行不在服务端列表中，保留
            server.forEach { dao.upsert(it) }
            _syncError.value = null
        } catch (e: Exception) {
            // 离线：不影响本地读写
        }
        sync()
    }

    /** 重放操作队列到服务端；成功一条删一条。 */
    suspend fun sync() {
        syncMutex.withLock {
            val ops = dao.pendingOps()
            if (ops.isEmpty()) return
            _syncing.value = true
            try {
                for (op in ops) {
                    try {
                        when (op.op) {
                            "add" -> {
                                val body = api.postBody(
                                    "/bohe/bill/save",
                                    buildJsonObject {
                                        put("amount", JsonPrimitive(op.amount ?: 0L))
                                        put("category", JsonPrimitive(op.category ?: "other"))
                                        put("note", JsonPrimitive(op.note ?: ""))
                                        put("date", JsonPrimitive(op.billDate ?: ""))
                                    },
                                )
                                // 服务端返回新 id（body.id）；解析不到就按"保存成功"清 pending
                                val serverId = body["id"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull }
                                if (serverId != null && serverId != op.localId) {
                                    dao.commitServerId(op.localId, serverId, op.createTime)
                                } else {
                                    dao.markSynced(op.localId)
                                }
                            }
                            "edit" -> {
                                api.postBody(
                                    "/bohe/bill/save",
                                    buildJsonObject {
                                        put("id", JsonPrimitive(op.localId))
                                        put("amount", JsonPrimitive(op.amount ?: 0L))
                                        put("category", JsonPrimitive(op.category ?: "other"))
                                        put("note", JsonPrimitive(op.note ?: ""))
                                        put("date", JsonPrimitive(op.billDate ?: ""))
                                    },
                                )
                                dao.markSynced(op.localId)
                            }
                            "delete" -> {
                                api.postBody(
                                    "/bohe/bill/delete",
                                    buildJsonObject { put("id", JsonPrimitive(op.localId)) },
                                )
                                dao.deleteById(op.localId)
                            }
                        }
                        dao.deleteOp(op.seq)
                    } catch (e: ApiException) {
                        // 业务错误（如权限/记录已不存在）：丢弃该操作避免永久阻塞队列
                        if (op.op == "delete") dao.deleteById(op.localId)
                        dao.deleteOp(op.seq)
                    } catch (e: Exception) {
                        // 网络失败：停下，下次再试（保持队列顺序）
                        _syncError.value = null // 离线是正常态，不打扰
                        return
                    }
                }
            } finally {
                _syncing.value = false
            }
        }
    }

    /** 全量替换本地库（登录用户切换时调用，防串数据）。 */
    suspend fun clearAll() {
        dao.clearAll()
        dao.clearOps()
    }
}
