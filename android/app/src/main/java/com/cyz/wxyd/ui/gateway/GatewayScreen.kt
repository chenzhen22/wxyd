package com.cyz.wxyd.ui.gateway

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory

/**
 * 模型网关（用户侧 Key 管理，计划 Task 10）。
 *
 * Key 卡片列表（模型 / 掩码 / 今日是否有效）+ 启停 / 明文 / 删除；新增走 [ModalBottomSheet]；
 * 明文与删除/删除确认都用 [AlertDialog]。「每日失效」的 Key 卡片上给出醒目的"启用"入口
 * （spec 约束 9：Key 只在启用当天有效）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayScreen(app: WxydApp, onBack: () -> Unit) {
    val vm: GatewayViewModel = viewModel(factory = appFactory(app) { GatewayViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // 删除二次确认（必须声明在读取它的 KeyCard lambda 之前）
    var pendingDelete by remember { mutableStateOf<GatewayKey?>(null) }

    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型网关") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                actions = {
                    val full = ui.max > 0 && ui.keys.size >= ui.max
                    TextButton(onClick = vm::openSheet, enabled = !full) { Text("＋ 新增") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading && ui.keys.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ui.error != null && ui.keys.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = vm::refresh) { Text("重试") }
                }

                ui.keys.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("还没有 Key，点右上角新增一把", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "Key 仅启用当天有效，次日需重新启用" + if (ui.max > 0) "（${ui.keys.size}/${ui.max}）" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(ui.keys, key = { it.id }) { k ->
                        KeyCard(
                            key = k,
                            activeToday = vm.isActiveToday(k),
                            onToggle = { vm.toggleKey(k) },
                            onReveal = { vm.revealKey(k.id) },
                            onDelete = { pendingDelete = k },
                        )
                    }
                }
            }
        }
    }

    if (ui.sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::closeSheet, sheetState = sheetState) {
            AddKeyForm(
                models = ui.models,
                defaultModel = ui.defaultModel,
                selected = ui.formModel,
                submitting = ui.submitting,
                onSelect = vm::onFormModelChange,
                onSubmit = vm::addKey,
            )
        }
    }

    ui.revealed?.let { plain ->
        RevealDialog(plain = plain, onDismiss = vm::dismissReveal)
    }

    pendingDelete?.let { k ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除 Key") },
            text = { Text("确定删除「${k.name}」？删除后使用该 Key 的客户端会立即失效。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    vm.deleteKey(k.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun KeyCard(
    key: GatewayKey,
    activeToday: Boolean,
    onToggle: () -> Unit,
    onReveal: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(key.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        activeToday -> "● 今日可用"
                        key.enabled -> "● 已过期（非今天启用）"
                        else -> "● 已停用"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (activeToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Switch(checked = key.enabled, onCheckedChange = { onToggle() })
            }
            Text(
                "模型：" + key.model.ifBlank { GatewayUiState.AUTO_MODEL } + "（auto = 跟随请求/系统默认）",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    key.maskedKey,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onReveal) { Text("查看明文", style = MaterialTheme.typography.bodySmall) }
            }
            if (key.lastUsedTime.isNotBlank()) {
                Text(
                    "最近使用：${key.lastUsedTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (!activeToday) {
                    // 每日失效的 Key：显眼的一键启用入口（spec 约束 9）
                    FilledTonalButton(onClick = onToggle) { Text("启用") }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** 新增表单：名称 + 模型（auto 或目录内任选）。 */
@Composable
private fun AddKeyForm(
    models: List<String>,
    defaultModel: String,
    selected: String,
    submitting: Boolean,
    onSelect: (String) -> Unit,
    onSubmit: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("新增 Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(32) },
            label = { Text("名称（如：笔记本）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("绑定模型", style = MaterialTheme.typography.bodySmall)
        FilterChip(
            selected = selected == GatewayUiState.AUTO_MODEL,
            onClick = { onSelect(GatewayUiState.AUTO_MODEL) },
            label = {
                Text("auto（跟随系统" + (if (defaultModel.isNotBlank()) "，默认 $defaultModel" else "") + "）")
            },
        )
        models.forEach { m ->
            FilterChip(selected = selected == m, onClick = { onSelect(m) }, label = { Text(m) })
        }
        FilledTonalButton(
            onClick = { onSubmit(name) },
            enabled = name.isNotBlank() && !submitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (submitting) "创建中…" else "创建（明文仅显示一次）") }
    }
}

/** 明文弹窗：等宽字体 + 复制。 */
@Composable
private fun RevealDialog(plain: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Key 明文") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("请妥善保管，不要外泄。", style = MaterialTheme.typography.bodySmall)
                Text(plain, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(plain)) }) { Text("复制") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
