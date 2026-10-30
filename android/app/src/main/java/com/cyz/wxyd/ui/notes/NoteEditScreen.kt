package com.cyz.wxyd.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyz.wxyd.ui.common.MarkdownView

/**
 * 笔记编辑页（计划 Task 12 Step 2）。
 *
 * 字段与 H5 `note/save` 对齐：标题 / 分类 / 可见性（公共|私有）/ 标签（逗号分隔）/ 正文（Markdown）。
 * "预览"切换 Markdown 渲染；他人笔记只读。标题改名时由 ViewModel 把原标题作为 `oldName` 提交。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(vm: NotesViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val doc = ui.editing ?: return
    var pendingDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.readOnly) "查看笔记" else if (ui.originalName.isBlank()) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                actions = {
                    val canDelete = !ui.readOnly && ui.originalName.isNotBlank()
                    if (canDelete) {
                        TextButton(onClick = { pendingDelete = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!ui.readOnly) {
                        TextButton(onClick = vm::save, enabled = !ui.saving) {
                            Text(if (ui.saving) "保存中…" else "保存")
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = doc.name,
                onValueChange = { v -> if (!ui.readOnly) vm.updateEditing { it.copy(name = v) } },
                label = { Text("标题") },
                singleLine = true,
                readOnly = ui.readOnly,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = doc.category,
                onValueChange = { v -> if (!ui.readOnly) vm.updateEditing { it.copy(category = v.take(20)) } },
                label = { Text("分类") },
                singleLine = true,
                readOnly = ui.readOnly,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !doc.isPrivate,
                    onClick = { if (!ui.readOnly) vm.updateEditing { it.copy(visibility = "public") } },
                    label = { Text("公共") },
                )
                FilterChip(
                    selected = doc.isPrivate,
                    onClick = { if (!ui.readOnly) vm.updateEditing { it.copy(visibility = "private") } },
                    label = { Text("私有") },
                )
            }
            OutlinedTextField(
                value = doc.tags.joinToString(", "),
                onValueChange = { v ->
                    if (!ui.readOnly) {
                        vm.updateEditing {
                            it.copy(tags = v.split(',', '，').map { t -> t.trim() }.filter { t -> t.isNotEmpty() })
                        }
                    }
                },
                label = { Text("标签（逗号分隔）") },
                singleLine = true,
                readOnly = ui.readOnly,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = vm::togglePreview) { Text(if (ui.preview) "编辑" else "预览") }
            if (ui.preview) {
                MarkdownView(markdown = doc.content.ifBlank { "*（空）*" }, modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedTextField(
                    value = doc.content,
                    onValueChange = { v -> if (!ui.readOnly) vm.updateEditing { it.copy(content = v) } },
                    label = { Text("正文（Markdown）") },
                    minLines = 12,
                    readOnly = ui.readOnly,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!ui.readOnly) {
                    FilledTonalButton(onClick = vm::save, enabled = !ui.saving, modifier = Modifier.fillMaxWidth()) {
                        Text(if (ui.saving) "保存中…" else "保存")
                    }
                }
            }
        }
    }

    if (pendingDelete) {
        ConfirmDeleteDialog(
            name = ui.originalName,
            onConfirm = { vm.delete(ui.originalName) },
            onDismiss = { pendingDelete = false },
        )
    }
}
