package com.cyz.wxyd.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cyz.wxyd.WxydApp

/**
 * 统一 ViewModel 工厂：所有 ViewModel 只依赖 [WxydApp]（从中取 [com.cyz.wxyd.AppContainer]），
 * 免去每个 ViewModel 写 `Factory` 样板。
 *
 * 用法（Screen 内）：
 * ```
 * val vm = viewModel(factory = appFactory(app) { LoginViewModel(it.container) })
 * ```
 *
 * 首版不引入 Hilt（依赖少，与 spec "不引入复杂脚手架" 一致）。
 */
inline fun <reified VM : ViewModel> appFactory(
    app: WxydApp,
    crossinline create: (app: WxydApp) -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        create(app) as T
}
