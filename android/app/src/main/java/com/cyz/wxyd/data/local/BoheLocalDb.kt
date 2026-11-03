package com.cyz.wxyd.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 薄荷账本地账单缓存（需求 6 离线记账）。
 *
 * - [id] = 服务端 bohe_bill.id；本机离线新建的记录用负数 id（-1, -2, …），
 *   同步成功后用服务端返回 id 重写（pending 状态清除）。
 * - [pendingOp] 同步状态：null=已同步；"add"/"edit"/"delete"=待重放到服务端的操作。
 *   delete 的行保留到同步完成（只留 id），避免断网删除后重启丢操作。
 */
@Entity(tableName = "bohe_bill")
data class BoheBillEntity(
    @PrimaryKey val id: Long,
    val amount: Long,
    val category: String,
    val note: String?,
    val billDate: String,
    val createTime: Long?,
    val pendingOp: String? = null,
)

/** 待同步操作日志队列（重放顺序 = 入队顺序）。 */
@Entity(tableName = "bohe_sync_op")
data class BoheSyncOpEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val op: String,             // add / edit / delete
    val localId: Long,          // 对应 bohe_bill.id（负数 = 本机新建）
    val amount: Long? = null,
    val category: String? = null,
    val note: String? = null,
    val billDate: String? = null,
    val createTime: Long? = null,
)

@Dao
interface BoheBillDao {

    @Query("SELECT * FROM bohe_bill WHERE pendingOp != 'delete' ORDER BY billDate DESC, id DESC")
    fun observeAll(): Flow<List<BoheBillEntity>>

    @Query("SELECT * FROM bohe_bill WHERE pendingOp != 'delete'")
    suspend fun all(): List<BoheBillEntity>

    @Query("SELECT * FROM bohe_bill WHERE id = :id")
    suspend fun byId(id: Long): BoheBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bills: List<BoheBillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bill: BoheBillEntity)

    @Query("SELECT MAX(id) FROM bohe_bill")
    suspend fun maxId(): Long?

    @Query("SELECT COUNT(*) FROM bohe_bill")
    suspend fun count(): Int

    @Query("SELECT * FROM bohe_sync_op ORDER BY seq ASC")
    suspend fun pendingOps(): List<BoheSyncOpEntity>

    @Insert
    suspend fun insertOp(op: BoheSyncOpEntity): Long

    @Query("DELETE FROM bohe_sync_op WHERE seq = :seq")
    suspend fun deleteOp(seq: Long)

    /** 同步成功：用服务端 id 重写本机行并清 pending（[oldLocalId] 为负数）。 */
    @androidx.room.Transaction
    suspend fun commitServerId(oldLocalId: Long, serverId: Long, serverCreateTime: Long?) {
        val local = byId(oldLocalId) ?: return
        upsert(local.copy(id = serverId, pendingOp = null, createTime = serverCreateTime ?: local.createTime))
        if (serverId != oldLocalId) {
            deleteById(oldLocalId)
        }
    }

    @androidx.room.Transaction
    suspend fun markSynced(id: Long) {
        byId(id)?.let { upsert(it.copy(pendingOp = null)) }
    }

    @Query("DELETE FROM bohe_bill WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bohe_bill")
    suspend fun clearAll()

    @Query("DELETE FROM bohe_sync_op")
    suspend fun clearOps()
}

@Database(entities = [BoheBillEntity::class, BoheSyncOpEntity::class], version = 1, exportSchema = false)
abstract class BoheLocalDb : RoomDatabase() {
    abstract fun billDao(): BoheBillDao
}
