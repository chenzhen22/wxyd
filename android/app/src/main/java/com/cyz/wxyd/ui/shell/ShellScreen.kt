package com.cyz.wxyd.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory

/**
 * Shell 脚本学习（计划 Task 13）：主题列表 → 详情（语法速查 + 示例），详情可"存笔记"。
 * 只读模块，不提供编辑入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(app: WxydApp, onBack: () -> Unit) {
    val vm: ShellViewModel = viewModel(factory = appFactory(app) { ShellViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) { ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.detail?.name ?: "Shell 脚本学习") },
                navigationIcon = {
                    IconButton(onClick = { if (ui.detail != null) vm.closeDetail() else onBack() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    val d = ui.detail
                    if (d != null) {
                        TextButton(onClick = { vm.saveToNote(d) }, enabled = !ui.saving) {
                            Text(if (ui.saving) "保存中…" else "存笔记")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            val detail = ui.detail
            if (detail != null) {
                ShellDetail(
                    topic = detail,
                    appendToNote = { vm.saveToNote(detail) },
                    appendEnabled = !ui.saving,
                )
            } else {
                ShellList(
                    ui = ui,
                    onKeyword = vm::onKeywordChange,
                    onOpen = vm::open,
                    onRetry = vm::refresh,
                )
            }
        }
    }
}

@Composable
private fun ShellList(
    ui: ShellUiState,
    onKeyword: (String) -> Unit,
    onOpen: (ShellTopic) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.keyword,
            onValueChange = onKeyword,
            label = { Text("搜索主题 / 分类") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
            ui.loading && ui.topics.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ui.error != null && ui.topics.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) { Text("重试") }
            }

            ui.visibleTopics.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (ui.topics.isEmpty()) "暂无主题" else "无匹配结果",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.visibleTopics, key = { it.name }) { t ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(t) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (t.category.isNotBlank()) {
                                Text(t.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShellDetail(topic: ShellTopic, appendToNote: () -> Unit, appendEnabled: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(topic.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (topic.category.isNotBlank()) {
                    Text(topic.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (topic.intro.isNotBlank()) Text(topic.intro, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (topic.syntaxes.isNotEmpty()) {
            SectionTitle("语法速查")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    topic.syntaxes.forEach { s ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            CodeBlock(s.title)
                            if (s.description.isNotBlank()) {
                                Text(s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        if (topic.examples.isNotEmpty()) {
            SectionTitle("示例")
            topic.examples.forEach { ex ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ex.name.ifBlank { "示例" }, style = MaterialTheme.typography.titleSmall)
                        if (ex.description.isNotBlank()) {
                            Text(ex.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (ex.code.isNotBlank()) CodeBlock(ex.code)
                    }
                }
            }
        }
        TextButton(onClick = appendToNote, enabled = appendEnabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (appendEnabled) "存为私有笔记" else "保存中…")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun CodeBlock(code: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(10.dp),
    ) {
        Text(code, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
