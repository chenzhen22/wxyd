package com.cyz.wxyd.ui.dayinfo

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.MarkdownView
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.robot.RobotItem

/**
 * 金融日报（Task 9）。
 *
 * 默认展示最新一期（列表首条，date-desc），顶部横向 chip 行切换期次；
 * 正文为 Markdown（`brief` 渲染为"📌 今日要点"callout）；
 * 订阅管理在 [ModalBottomSheet] 内（新增 / 启停 / 测试 / 删除），与 H5 的订阅交互一一对应。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayInfoScreen(
    app: WxydApp,
    onBack: () -> Unit,
) {
    val vm: DayInfoViewModel = viewModel(factory = appFactory(app) { DayInfoViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let { snackbar.showSnackbar(it); vm.consumeError() }
    }
    LaunchedEffect(ui.subMessage) {
        ui.subMessage?.let { snackbar.showSnackbar(it); vm.consumeSubMessage() }
    }
    LaunchedEffect(ui.subError) {
        ui.subError?.let { snackbar.showSnackbar(it); vm.consumeSubError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("金融日报") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                actions = {
                    TextButton(onClick = vm::openSubSheet) { Text("🔔 订阅") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ui.error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = vm::refresh) { Text("重试") }
                }

                ui.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("暂无日报", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> DayInfoContent(ui = ui, onSelectIssue = vm::selectIssue)
            }
        }

        if (ui.subSheetOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = vm::closeSubSheet, sheetState = sheetState) {
                SubSheetContent(
                    ui = ui,
                    onReload = vm::loadSubs,
                    onAdd = vm::addSub,
                    onToggle = vm::toggleSub,
                    onTest = vm::testSub,
                    onDelete = vm::deleteSub,
                )
            }
        }
    }
}

@Composable
private fun DayInfoContent(ui: DayInfoUiState, onSelectIssue: (Long) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // 期次切换：横向 chip 行（H5 的 Select 等价）
        if (ui.items.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.items.forEach { item ->
                    FilterChip(
                        selected = item.id == ui.selectedId,
                        onClick = { onSelectIssue(item.id) },
                        label = { Text(item.infoDate.ifBlank { "期次#${item.id}" }) },
                    )
                }
            }
        }

        // 详情正文
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (d.infoDate.isNotBlank()) {
                        item {
                            Text(
                                d.infoDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (d.brief.isNotBlank()) {
                        item {
                            BriefCallout(d.brief)
                        }
                    }
                    item {
                        MarkdownView(markdown = d.content, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefCallout(brief: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        // 左侧 accent 边：用 colorScheme.primaryContainer 作底，箭头 emoji 作标记
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("📌 今日要点", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(brief, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ============================================================
// 订阅管理（ModalBottomSheet）
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubSheetContent(
    ui: DayInfoUiState,
    onReload: () -> Unit,
    onAdd: (channel: String, robotId: Long?, email: String) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onTest: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    // 删除二次确认（H5 `confirm('确定删除该订阅？')`）
    var pendingDelete by remember { mutableStateOf<DayInfoSub?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔔 日报订阅", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReload) { Text("刷新") }
        }
        Text(
            "新一期日报入库时自动推送到所选通道",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (ui.subsLoading && ui.subs.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        if (!ui.subsLoading && ui.subs.isEmpty()) {
            Text("还没有订阅", style = MaterialTheme.typography.bodySmall)
        }
        ui.subs.forEach { sub ->
            SubRow(
                sub = sub,
                onToggle = { onToggle(sub.id, !sub.enabled) },
                onTest = { onTest(sub.id) },
                onDelete = { pendingDelete = sub },
            )
        }

        AddSubForm(
            robots = ui.robots,
            onAdd = onAdd,
        )
    }

    pendingDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除订阅") },
            text = { Text("确定删除该订阅？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(sub.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SubRow(
    sub: DayInfoSub,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
) {
    val target = if (sub.channel == "dingtalk") "机器人#${sub.robotId ?: "-"}" else sub.email
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    (if (sub.channel == "dingtalk") "🤖 " else "✉️ ") + target,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    (if (sub.channel == "dingtalk") "钉钉" else "邮箱") + if (sub.enabled) " · 启用" else " · 已暂停",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = sub.enabled, onCheckedChange = { onToggle() })
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onTest) { Text("测试", style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = onDelete) {
                Text("删除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** 新增订阅表单：通道二选一，dingtalk 必须选机器人（空则提示），email 填邮箱。 */
@Composable
private fun AddSubForm(
    robots: List<RobotItem>,
    onAdd: (channel: String, robotId: Long?, email: String) -> Unit,
) {
    var channel by remember { mutableStateOf("dingtalk") }
    var robotId by remember { mutableStateOf(robots.firstOrNull()?.id) }
    var email by remember { mutableStateOf("") }

    val canSubmit = channel == "email" && EMAIL_REGEX.matches(email.trim()) ||
        channel == "dingtalk" && robotId != null

    Spacer(Modifier.height(4.dp))
    Text("添加订阅", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = channel == "dingtalk", onClick = { channel = "dingtalk" }, label = { Text("钉钉机器人") })
        FilterChip(selected = channel == "email", onClick = { channel = "email" }, label = { Text("邮件") })
    }

    if (channel == "dingtalk") {
        if (robots.isEmpty()) {
            Text(
                "请先添加钉钉机器人",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text("选择机器人", style = MaterialTheme.typography.bodySmall)
            robots.forEach { robot ->
                FilterChip(
                    selected = robot.id == robotId,
                    onClick = { robotId = robot.id },
                    label = { Text(robot.name) },
                )
            }
        }
    } else {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("接收邮箱") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    FilledTonalButton(
        onClick = {
            if (channel == "dingtalk" && robotId == null) return@FilledTonalButton
            onAdd(
                channel,
                if (channel == "dingtalk") robotId else null,
                if (channel == "email") email.trim() else "",
            )
        },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) { Text("添加订阅") }
}

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
