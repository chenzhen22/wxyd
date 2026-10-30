package com.cyz.wxyd.ui.notes

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.session.SessionViewModel

/**
 * 记忆笔记列表（计划 Task 12）：我的 / 公开两段式 + 关键词检索 + 新建。
 * 点击一行进入 [NoteEditScreen]（同一 Composable 内按 `ui.editing` 切换，不走单独路由）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(app: WxydApp, session: SessionViewModel, onBack: () -> Unit) {
    val vm: NotesViewModel = viewModel(factory = appFactory(app) { NotesViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val me by session.session.collectAsState()

    LaunchedEffect(me?.id) { me?.let { vm.setMyId(it.id) } }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    if (ui.editing != null) {
        NoteEditScreen(vm = vm, onBack = vm::closeEditing)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::newNote) { Text("＋", style = MaterialTheme.typography.titleLarge) }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = ui.scope == NoteScope.MINE,
                    onClick = { vm.setScope(NoteScope.MINE) },
                    label = { Text("我的") },
                )
                FilterChip(
                    selected = ui.scope == NoteScope.PUBLIC,
                    onClick = { vm.setScope(NoteScope.PUBLIC) },
                    label = { Text("公开") },
                )
            }
            OutlinedTextField(
                value = ui.keyword,
                onValueChange = vm::onKeywordChange,
                label = { Text("搜索标题与正文") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.refresh() }),
                trailingIcon = { TextButton(onClick = vm::refresh) { Text("搜索") } },
            )
            Spacer(Modifier.height(6.dp))

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading && ui.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    ui.error != null && ui.items.isEmpty() -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = vm::refresh) { Text("重试") }
                    }

                    ui.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (ui.keyword.isBlank() && ui.scope == NoteScope.MINE) "暂无笔记，点右下角 ＋ 新建"
                            else "没有找到相关笔记",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ui.items, key = { "${it.ownerId}/${it.name}" }) { n ->
                            NoteRow(
                                item = n,
                                mine = n.ownerId == ui.myId || ui.scope == NoteScope.MINE,
                                onOpen = { vm.open(n) },
                                onExport = { vm.export(n.ownerId, n.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(item: NoteItem, mine: Boolean, onOpen: () -> Unit, onExport: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (item.visibility == "private") Text("🔒", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                buildString {
                    if (!mine) append(item.ownerName.ifBlank { "他人" } + " · ")
                    append(item.category.ifBlank { "未分类" })
                    if (item.updateTime.isNotBlank()) append(" · " + item.updateTime)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.preview.isNotBlank()) {
                Text(item.preview, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            if (item.tags.isNotEmpty()) {
                Text("🏷 " + item.tags.joinToString(" "), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onExport) { Text("导出", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** 保留给将来外部打开的删除确认（编辑页内使用）。 */
@Composable
internal fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除笔记") },
        text = { Text("确定删除「$name」？删除后不可恢复。") },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
