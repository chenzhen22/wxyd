package com.cyz.wxyd.ui.welfare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.MarkdownView
import com.cyz.wxyd.ui.common.appFactory

/**
 * 福利详情（计划 Task 8 Step 2）：`GET /welfare/detail?id` → Markdown 正文。
 *
 * 独立路由 `welfare/{id}`（不是覆盖层），返回键行为与 Android 习惯一致。
 * ViewModel 从上一页的 `NavBackStackEntry` 作用域取，保证与列表页共享同一个实例和缓存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareDetailScreen(
    app: WxydApp,
    id: Long,
    onBack: () -> Unit,
) {
    val vm: WelfareViewModel = viewModel(factory = appFactory(app) { WelfareViewModel(it.container) })
    val ui by vm.ui.collectAsState()

    // 首帧 / id 变化时拉详情（同一 VM 切到别的 id 也能重新加载）
    LaunchedEffect(id) { vm.openDetail(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.detail?.title ?: "福利详情", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.detailLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ui.detailError != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(ui.detailError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }

                else -> ui.detail?.let { d ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (d.publishedAt.isNotBlank()) {
                            Text(
                                d.publishedAt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MarkdownView(markdown = d.resolveMarkdown(), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
