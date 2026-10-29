package com.cyz.wxyd.ui.nav

/**
 * 路由约定（计划 Task 4 Interfaces，后续任务全部复用）。
 *
 * - 会话为 null 时只挂 `login` / `register` / `forgot`；
 * - 会话存在时挂 `main`（双 Tab 容器）与各模块路由；
 * - 模块卡片点击 → [module]（`module/{key}`），按 menu/list 的 `key` 分发。
 */
object Routes {
    // ---- 无会话 ----
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"

    // ---- 有会话 ----
    const val MAIN = "main"

    // 模块路由（Task 8–13 逐个实装，未实装 key 显示"建设中"）
    const val WELFARE = "welfare"
    const val WELFARE_DETAIL = "welfare/{id}"
    const val DAYINFO = "dayinfo"
    const val GATEWAY = "gateway"
    const val ROBOT = "robot"
    const val NOTES = "notes"
    const val SHARE = "share"
    const val SHELL = "shell"
    const val JAVAAPI = "javaapi"

    /** 通用模块入口：菜单卡片按 key 进来，由路由表分发。 */
    const val MODULE = "module/{key}"

    fun module(key: String) = "module/$key"
    fun welfareDetail(id: Long) = "welfare/$id"
}
