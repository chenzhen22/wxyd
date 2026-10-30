package com.cyz.wxyd

import android.content.Context
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
     * Base URL 默认留空：用户首次启动在登录页/"我的"里填写部署地址
     * （spec 第 7 节「服务器地址」）。真实地址由 [SettingsStore.baseUrlFlow]
     * 在每次请求时动态读取。
     */
    val apiClient: ApiClient = ApiClient(
        baseUrl = SettingsStore.DEFAULT_BASE_URL,
        cookieJar = cookieJar,
        settingsStore = settingsStore,
    )

    val themeController: ThemeController = ThemeController(settingsStore, apiClient)

    /** 文件下载（notes 导出 / share 下载），404 → 可读异常（Review Focus #5）。 */
    val fileDownloader: FileDownloader = FileDownloader(apiClient, context.applicationContext)
}
