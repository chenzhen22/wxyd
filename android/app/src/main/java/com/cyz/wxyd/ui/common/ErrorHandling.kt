package com.cyz.wxyd.ui.common

import com.cyz.wxyd.data.remote.ApiException
import com.cyz.wxyd.data.remote.SessionExpiredException

/**
 * 异常 → 用户可读文案（spec 约束 1；计划 Task 4 Step 4）。
 *
 * 注意：401（[SessionExpiredException]）的**导航跳转**不在这里做，
 * 而由 [com.cyz.wxyd.ui.session.SessionViewModel] 全局收集后统一跳登录页；
 * 这里只负责给出 Snackbar 文案。
 */
fun Throwable.userMessage(): String = when (this) {
    is SessionExpiredException -> "登录已过期，请重新登录"
    is ApiException -> errorMsg.ifBlank { "请求失败（$errorCode）" }
    else -> "网络错误，请重试"
}
