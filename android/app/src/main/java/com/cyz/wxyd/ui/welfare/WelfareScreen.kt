package com.cyz.wxyd.ui.welfare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory

/**
 * 福利专区列表（计划 Task 8 Step 2）。界面范式：顶栏 + 返回 + 下拉刷新；
 * 顶部"📡 羊毛雷达"为页内区块（[RadarSection]），不是独立卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareScreen(
    app: WxydApp,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val vm: WelfareViewModel = viewModel(factory = appFactory(app) { WelfareViewModel(it.container) })
    WelfareContent(app = app, vm = vm, onBack = onBack, onOpenDetail = onOpenDetail)
}

/** 列表主体：接收外部 VM（详情路由用同一实例渲染，避免整页重拉）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WelfareContent(
    app: WxydApp,
    vm: WelfareViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // 触底（倒数第 2 项可见）加载下一页
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            ui.hasMore && last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) vm.loadMore() }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("福利专区") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OutlinedTextField(
                value = ui.keyword,
                onValueChange = vm::onKeywordChange,
                placeholder = { Text("搜索标题 / 正文") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.refresh(ui.keyword) }),
                trailingIcon = {
                    TextButton(onClick = { vm.refresh(ui.keyword) }) { Text("搜索") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            )
            PullToRefreshBox(
                isRefreshing = ui.loading,
                onRefresh = { vm.refresh(ui.keyword) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        RadarSection(
                            radar = ui.radar,
                            onReload = vm::loadRadar,
                            onAdd = vm::addRadar,
                            onToggle = vm::toggleRadar,
                            onTest = vm::testRadar,
                            onDelete = vm::deleteRadar,
                            onConsumeMessage = vm::consumeRadarMessage,
                            onConsumeError = vm::consumeRadarError,
                            showMessage = { msg -> snackbar.showSnackbar(msg) },
                        )
                    }
                    if (!ui.loading && ui.posts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (ui.keyword.isBlank()) "暂无福利内容" else "没有找到相关内容",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(ui.posts, key = { it.id }) { post ->
                        WelfareCard(post = post, onClick = { onOpenDetail(post.id) })
                    }
                    if (ui.loadingMore) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                    if (!ui.hasMore && ui.posts.isNotEmpty()) {
                        item {
                            Text(
                                "— 已经到底了 —",
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelfareCard(post: WelfarePost, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (post.summary.isNotBlank()) {
                Text(
                    post.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (post.publishedAt.isNotBlank()) {
                Text(post.publishedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
