package com.cyz.wxyd

import android.content.Context
import com.cyz.wxyd.data.local.BiometricStore
import com.cyz.wxyd.data.local.SessionCookieJar
import com.cyz.wxyd.data.local.SettingsStore
import com.cyz.wxyd.data.remote.ApiClient
import com.cyz.wxyd.data.remote.FileDownloader
import com.cyz.wxyd.ui.theme.ThemeController

/**
 * 进程级手工依赖容器（首版不引入 Hilt，依赖很少，手写工厂更直观）。
 *
 * 在 [WxydApp.onCreate] 中构造一次，Activity / ViewModel 通过
 * `(application as WxydApp).container` 取用。
 *
 * 生命周期与进程一致：ApiClient 内部 OkHttp 连接池、SessionCookieJar 内存缓存
 * 都应跨页面存活，不能随 Activity 重建。
 */
class AppContainer(context: Context) {

    val settingsStore: SettingsStore = SettingsStore(context)

    val cookieJar: SessionCookieJar = SessionCookieJar(settingsStore)

    /**
     * Base URL 固定为云端部署地址（[SettingsStore.DEFAULT_BASE_URL]），
     * 不再支持登录页/"我的"自选；保留 DataStore 动态读取通道以兼容 CookieJar。
     */
    val apiClient: ApiClient = ApiClient(
        baseUrl = SettingsStore.DEFAULT_BASE_URL,
        cookieJar = cookieJar,
    )

    /** 指纹登录（凭据 Keystore 加密存本机，见 [BiometricStore]）。 */
    val biometricStore: BiometricStore = BiometricStore(context)

    /**
     * 本次进程内最近一次密码登录成功的凭据（仅内存，供"开启指纹登录"时加密落盘；
     * 会话恢复而非密码登录时为 null，需重新登录后才能开启）。
     */
    @Volatile
    var pendingLoginCredential: Pair<String, String>? = null

    val themeController: ThemeController = ThemeController(settingsStore, apiClient)

    /** 文件下载（notes 导出 / share 下载），404 → 可读异常（Review Focus #5）。 */
    val fileDownloader: FileDownloader = FileDownloader(apiClient, context.applicationContext)
}
