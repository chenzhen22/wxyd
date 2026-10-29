package com.cyz.wxyd

import android.app.Application

/**
 * 小泽助手 Application：持有进程级依赖容器 [AppContainer]。
 */
class WxydApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
