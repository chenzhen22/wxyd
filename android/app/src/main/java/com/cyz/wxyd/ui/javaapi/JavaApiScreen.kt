package com.cyz.wxyd.ui.javaapi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
 * Java 8 API 学习（计划 Task 13）：类列表 → 详情（方法列表 + 在线运行测试）→ 存笔记。
 * 在线运行由服务端 `javax.tools.JavaCompiler` 执行，客户端只提交 `{testName, code}`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JavaApiScreen(app: WxydApp, onBack: () -> Unit) {
    val vm: JavaApiViewModel = viewModel(factory = appFactory(app) { JavaApiViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) { ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.detail?.name ?: "Java API 学习") },
                navigationIcon = {
                    IconButton(onClick = { if (ui.detail != null) vm.closeDetail() else onBack() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            val detail = ui.detail
            if (detail != null) {
                JavaApiDetail(ui = ui, item = detail, vm = vm)
            } else {
                JavaApiList(ui = ui, onKeyword = vm::onKeywordChange, onOpen = vm::open, onRetry = vm::refresh)
            }
        }
    }
}

@Composable
private fun JavaApiList(
    ui: JavaApiUiState,
    onKeyword: (String) -> Unit,
    onOpen: (ApiClassItem) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ui.keyword,
            onValueChange = onKeyword,
            label = { Text("搜索 API / 包名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
            ui.loading && ui.classes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ui.error != null && ui.classes.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) { Text("重试") }
            }

            ui.visibleClasses.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (ui.classes.isEmpty()) "暂无条目" else "无匹配结果",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.visibleClasses, key = { it.name }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(c) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (c.packageName.isNotBlank()) {
                                Text(c.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JavaApiDetail(ui: JavaApiUiState, item: ApiClassItem, vm: JavaApiViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (item.packageName.isNotBlank()) {
                    Text(item.packageName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (item.intro.isNotBlank()) Text(item.intro, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (item.methods.isNotEmpty()) {
            SectionTitle("方法列表")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    item.methods.forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider()
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CodeText(m.title)
                            if (m.description.isNotBlank()) {
                                Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (item.testCases.isNotEmpty()) {
            SectionTitle("在线运行测试")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item.testCases.forEachIndexed { i, tc ->
                            FilterChip(
                                selected = ui.caseIndex == i,
                                onClick = { vm.selectCase(i) },
                                label = { Text(tc.name.ifBlank { "用例 ${i + 1}" }, maxLines = 1) },
                            )
                        }
                    }
                    item.testCases.getOrNull(ui.caseIndex)?.let { tc ->
                        if (tc.description.isNotBlank()) {
                            Text(tc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (tc.expectedOutput.isNotBlank()) {
                            Text("期望输出：${tc.expectedOutput}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = ui.code,
                        onValueChange = vm::onCodeChange,
                        label = { Text("代码") },
                        minLines = 10,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = vm::run, enabled = !ui.running, modifier = Modifier.fillMaxWidth()) {
                        Text(if (ui.running) "运行中…" else "▶ 运行测试")
                    }
                    ui.runResult?.let { r ->
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val head = (if (r.passed) "✅ 测试通过" else "❌ 测试未通过") +
                                    (r.executionTime?.let { " · ${it}ms" } ?: "")
                                Text(head, style = MaterialTheme.typography.titleSmall)
                                if (r.output.isNotBlank()) {
                                    CodeText("---- 输出 ----\n${r.output}")
                                }
                                if (r.error.isNotBlank()) {
                                    Text(
                                        "---- 错误 ----\n${r.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                if (r.output.isBlank() && r.error.isBlank()) {
                                    Text("（无输出）", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (ui.detailLoading) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        }

        FilledTonalButton(
            onClick = { vm.saveToNote(item) },
            enabled = !ui.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.saving) "保存中…" else "存为私有笔记")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun CodeText(code: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Text(code, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
