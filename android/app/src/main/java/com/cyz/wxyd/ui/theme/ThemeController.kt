package com.cyz.wxyd.ui.theme

import com.cyz.wxyd.data.local.SettingsStore
import com.cyz.wxyd.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 主题控制器（spec 第 5 节「主题」、决策 D7）。
 *
 * 职责：
 * 1. 暴露当前主题 [themeFlow]（来自 DataStore，本地即时生效）；
 * 2. [setTheme] 先写本地（DataStore 优先，本地立即生效），再 fire-and-forget
 *    同步 `POST /api/user/updateTheme {theme}`（与 H5 跨端一致）；
 * 3. 网络失败**不阻塞、不回滚**本地改动（失败仅吞掉，本地已生效）。
 *
 * @param settings 本地设置存储（DataStore），提供 [SettingsStore.themeFlow]/[SettingsStore.setTheme]
 * @param client   网络层，用于同步主题到服务端
 */
class ThemeController(
    settings: SettingsStore,
    client: ApiClient,
) {
    /** 当前主题（7 值之一，来自 DataStore；非法值已被 SettingsStore coerce 到 dark）。 */
    val themeFlow: Flow<String> = settings.themeFlow

    /** fire-and-forget 用的作用域：异常隔离（SupervisorJob），不阻塞调用方。 */
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 切换主题：先落本地，再后台同步服务端。
     *
     * 本地写入是 suspend（DataStore.edit）；成功后启动一个不阻塞的协程去调
     * `user/updateTheme`。网络异常仅记录日志、不回滚本地。
     */
    suspend fun setTheme(v: String) {
        // 1) 本地优先：立即生效（SettingsStore 内部已 coerce 到合法 7 值）
        settings.setTheme(v)
        // 2) fire-and-forget 同步服务端：失败不影响本地
        val local = v
        syncScope.launch {
            runCatching {
                client.postBody(
                    "/user/updateTheme",
                    buildJsonObject { put("theme", JsonPrimitive(local)) },
                )
            }
            // 网络/业务异常：吞掉，不回滚本地（spec 要求失败不阻塞本地生效）
        }
    }
}
