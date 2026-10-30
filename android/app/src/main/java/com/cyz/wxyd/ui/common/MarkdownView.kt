package com.cyz.wxyd.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * Markdown 渲染的统一入口（依赖 `com.github.jeziellago:compose-markdown`）。
 *
 * 包一层的目的：库版本升级或换库时只改这一处，各模块只认 [MarkdownView]；
 * 文字色/背景跟随当前主题（H5 详情页同款：正文用 onBackground）。
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    MarkdownText(
        markdown = markdown,
        modifier = modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        isTextSelectable = true,
    )
}
