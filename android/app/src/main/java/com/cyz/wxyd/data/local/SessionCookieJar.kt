package com.cyz.wxyd.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 会话 Cookie 持久化（spec 约束 1：HttpSession / JSESSIONID）。
 *
 * - 内存 Map 缓存 + Preferences DataStore 持久化，JSESSIONID 跨进程重启保留。
 * - 401 后由上层调 [SettingsStore.clearSession] 清除，本类 [clear] 同步清内存与存储。
 * - 只关心 `JSESSIONID`，其余 Cookie 忽略。
 *
 * @param settingsStore 持久化后端（与 baseUrl/theme 同一 DataStore 文件）
 * @param externalScope 持久化写入所用协程作用域；默认自带 SupervisorJob + IO，随进程存活
 */
class SessionCookieJar(
    private val settingsStore: SettingsStore,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : CookieJar {

    /** host → JSESSIONID 值（内存缓存）。Cookie 域粒度按 host 存，避免不同部署串会话。 */
    private val cache = HashMap<String, String>()
    private val lock = Any()

    init {
        // 进程启动后从 DataStore 异步回填缓存（fire-and-forget，不阻塞构造线程，
        // 可在主线程安全构造）。首请求若在回填前到达，cache 为空则不带 Cookie，
        // 登录流程会重新建立会话，无副作用。
        externalScope.launch {
            val saved = settingsStore.jsessionIdFlow.first()
            if (saved != null) {
                synchronized(lock) { cache[STORED_KEY] = saved }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            if (cookie.name != JSESSIONID_NAME) return@forEach
            val host = cookie.domain.ifEmpty { url.host }
            synchronized(lock) {
                cache[host] = cookie.value
                cache[STORED_KEY] = cookie.value
            }
            externalScope.launch { settingsStore.setJsessionId(cookie.value) }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val value = synchronized(lock) {
            cache[url.host] ?: cache[STORED_KEY]
        } ?: return emptyList()
        return listOf(
            Cookie.Builder()
                .name(JSESSIONID_NAME)
                .value(value)
                .domain(url.host)
                .path("/")
                .build(),
        )
    }

    /** 清空会话（登出 / 401）。 */
    fun clear() {
        synchronized(lock) { cache.clear() }
        externalScope.launch { settingsStore.clearSession() }
    }

    companion object {
        private const val JSESSIONID_NAME = "JSESSIONID"
        /** DataStore 槽位 key（跨 host 的兜底存储）。 */
        private const val STORED_KEY = ""
    }
}
