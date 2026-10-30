package com.cyz.wxyd.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wxyd_settings")

/**
 * 本地设置存储（Preferences DataStore）。
 *
 * 首版仅持久化两项（spec 第 7 节「离线缓存」决策）：
 * - 主题（服务端 `user/updateTheme` 只认的 7 值之一，存本机兜底）
 * - 会话 Cookie（JSESSIONID，由 [SessionCookieJar] 读写，登录态跨进程保留）
 *
 * 服务器地址不再可配置：固定 [DEFAULT_BASE_URL]（云端部署），
 * 指纹登录凭据另存 [BiometricStore]（独立 DataStore 语义，加密存储）。
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val JSESSIONID = stringPreferencesKey("jsessionid")
    }

    /** 主题 7 值：dark/light/eyecare/techblue/orange/pink/system（spec 界面范式节）。 */
    val themeFlow: Flow<String> =
        context.dataStore.data.map { prefs ->
            val stored = prefs[Keys.THEME]
            if (stored != null && stored in VALID_THEMES) stored else DEFAULT_THEME
        }

    suspend fun setTheme(v: String) {
        val coerced = if (v in VALID_THEMES) v else DEFAULT_THEME
        context.dataStore.edit { it[Keys.THEME] = coerced }
    }

    /** 当前会话 JSESSIONID（供 CookieJar 持久化）。 */
    val jsessionIdFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.JSESSIONID] }

    suspend fun setJsessionId(v: String?) {
        context.dataStore.edit { prefs ->
            if (v.isNullOrEmpty()) prefs.remove(Keys.JSESSIONID)
            else prefs[Keys.JSESSIONID] = v
        }
    }

    /** 清除会话（登出 / 401 后由上层调用）。 */
    suspend fun clearSession() {
        context.dataStore.edit { it.remove(Keys.JSESSIONID) }
    }

    companion object {
        /** 固定部署地址（云端部署；登录页与"我的"不再展示/编辑）。 */
        const val DEFAULT_BASE_URL = "https://wxyd.us.ci/api"
        const val DEFAULT_THEME = "dark"
        val VALID_THEMES = setOf(
            "dark", "light", "eyecare", "techblue", "orange", "pink", "system",
        )
    }
}
