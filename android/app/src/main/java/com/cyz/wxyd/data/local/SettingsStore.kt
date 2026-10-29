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
 * 首版仅持久化三项（spec 第 7 节「离线缓存」决策）：
 * - 服务器地址 Base URL（运行时可变，用户在"我的"里改；可填 `http://` 明文内网地址）
 * - 主题（服务端 `user/updateTheme` 只认的 7 值之一，存本机兜底）
 * - 会话 Cookie（JSESSIONID，由 [SessionCookieJar] 读写，登录态跨进程保留）
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val THEME = stringPreferencesKey("theme")
        val JSESSIONID = stringPreferencesKey("jsessionid")
    }

    /** 默认 Base URL：留空引导用户在"我的"里填写部署地址（spec 第 7 节）。 */
    val baseUrlFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.BASE_URL] ?: DEFAULT_BASE_URL }

    suspend fun setBaseUrl(v: String) {
        context.dataStore.edit { it[Keys.BASE_URL] = v.trim().trimEnd('/') }
    }

    /** 主题 7 值：dark/light/eyecare/techblue/orange/pink/system（spec 界面范式节）。 */
    val themeFlow: Flow<String> =
        context.dataStore.data.map { prefs ->
            val stored = prefs[Keys.THEME]
            if (stored in VALID_THEMES) stored else DEFAULT_THEME
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
        const val DEFAULT_BASE_URL = ""
        const val DEFAULT_THEME = "dark"
        val VALID_THEMES = setOf(
            "dark", "light", "eyecare", "techblue", "orange", "pink", "system",
        )
    }
}
