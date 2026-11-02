package com.cyz.wxyd.ui.bohe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 分类 emoji（与小程序视觉一致，未知分类兜底 📦）。 */
private fun categoryEmoji(key: String): String = when (key) {
    "food" -> "🍚"
    "transport" -> "🚌"
    "shop" -> "🛍"
    "house" -> "🏠"
    "fun" -> "🎮"
    "medical" -> "💊"
    "study" -> "📚"
    else -> "📦"
}

private fun formatYuan(cents: Long): String = String.format("%.2f", cents / 100.0)

/** 薄荷账主页：月份切换 + 账单/统计/我的关注 3 卡片 + 记账 FAB（超管另有导入入口）。 */
@Composable
fun BoheScreen(app: WxydApp, sessionViewModel: com.cyz.wxyd.ui.session.SessionViewModel) {
    val vm: BoheViewModel = viewModel(factory = appFactory(app) { BoheViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val session by sessionViewModel.session.collectAsState()

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(ui.message) {
        // message 展示后由 UI 自行保留（SnackBar 简化为文本），不自动清除
    }

    Scaffold(
        floatingActionButton = {
            if (ui.viewingFriendId == null) {
                FloatingActionButton(onClick = { vm.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
        ) {
            ui.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            ui.message?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            // 好友查看模式提示
            if (ui.viewingFriendId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "正在查看：${ui.viewingFriendName}（只读）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { vm.backToMe() }) { Text("切回我") }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
            ) {
                item { MonthHeader(vm, ui) }
                if (session?.isAdmin == true && ui.viewingFriendId == null) {
                    item { ImportCard(app, vm) }
                }
                if (ui.loading && ui.bills.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    item { BillsCard(vm, ui) }
                    item { StatsCard(ui) }
                    if (ui.viewingFriendId == null) {
                        item { FriendsCard(vm, ui) }
                    }
                }
            }
        }
    }

    if (ui.showAddDialog) {
        AddBillDialog(vm, ui)
    }
}

@Composable
private fun MonthHeader(vm: BoheViewModel, ui: BoheUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { vm.prevMonth() }) { Text("‹") }
        Text(
            "${ui.month} 支出 ¥${formatYuan(ui.monthTotal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(onClick = { vm.nextMonth() }) { Text("›") }
    }
}

@Composable
private fun BillsCard(vm: BoheViewModel, ui: BoheUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("账单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (ui.bills.isEmpty()) {
                Text(
                    "本月暂无账单",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ui.bills.take(30).forEach { bill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(categoryEmoji(bill.category), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                bill.note?.takeIf { it.isNotBlank() } ?: BoheCategories.label(bill.category),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${bill.billDate} · ${BoheCategories.label(bill.category)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "-¥${formatYuan(bill.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (ui.viewingFriendId == null) {
                            Text(
                                "删",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable {
                                        vm.deleteBill(bill.id)
                                    },
                            )
                        }
                    }
                }
                if (ui.bills.size > 30) {
                    Text(
                        "… 共 ${ui.bills.size} 条，仅显示前 30 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(ui: BoheUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (ui.byCategory.isEmpty()) {
                Text(
                    "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val max = ui.byCategory.maxOf { it.total }.coerceAtLeast(1)
                ui.byCategory.forEach { cs ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "${categoryEmoji(cs.category)} ${BoheCategories.label(cs.category)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "¥${formatYuan(cs.total)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth(cs.total.toFloat() / max)
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsCard(vm: BoheViewModel, ui: BoheUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("我的关注", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (ui.friends.isEmpty()) {
                Text(
                    "还没有好友。输入对方用户名申请关注，通过后可互相查看账单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ui.friends.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    f.status == 1 -> "已互关，点击查看账单"
                                    f.outgoing -> "等待对方通过"
                                    else -> "对方申请关注你"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (f.status == 1) {
                            Text(
                                "查看 ›",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { vm.openFriend(f) },
                            )
                            Text(
                                "删",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable { vm.removeFriend(f.userId) },
                            )
                        } else if (!f.outgoing) {
                            TextButton(onClick = { vm.handleFriend(f.userId, true) }) { Text("通过") }
                            TextButton(onClick = { vm.handleFriend(f.userId, false) }) { Text("拒绝") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = ui.friendKeyword,
                    onValueChange = { vm.onFriendKeywordChange(it) },
                    placeholder = { Text("对方用户名") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.requestFriend() }) { Text("加好友") }
            }
        }
    }
}

/**
 * 超管导入卡片：选云导出 JSON 文件 + 目标用户名，调 bohe/import。
 * 文件读文本后解析为数组塞进请求体；结果（inserted/updated/skipped）走 vm 的 message 通道。
 */
@Composable
private fun ImportCard(app: WxydApp, vm: BoheViewModel) {
    var targetUsername by remember { mutableStateOf("chenzhen") }
    var busy by remember { mutableStateOf(false) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null || busy) return@rememberLauncherForActivityResult
        busy = true
        // 读取文件 + 发请求放到协程
        kotlinx.coroutines.MainScope().launch {
            try {
                val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取文件")
                val arr = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonArray
                val body = kotlinx.serialization.json.buildJsonObject {
                    put("targetUsername", kotlinx.serialization.json.JsonPrimitive(targetUsername))
                    put("bills", arr)
                }
                app.container.apiClient.postBody("/bohe/import", body)
                vm.importDone("导入成功：${arr.size} 条已提交")
            } catch (e: Exception) {
                vm.importDone(e.userMessage())
            } finally {
                busy = false
            }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("导入账单（超管）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = targetUsername,
                onValueChange = { targetUsername = it },
                label = { Text("目标用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { picker.launch("application/json") }, enabled = !busy) {
                    Text(if (busy) "导入中…" else "选择云导出 JSON 并导入")
                }
            }
        }
    }
}

/** 记一笔弹窗：分类下拉（8 内置分类）、金额（元）、备注、日期默认今天。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(vm: BoheViewModel, ui: BoheUiState) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("food") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var categoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { vm.dismissAddDialog() },
        title = { Text("记一笔") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额（元）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = BoheCategories.label(category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { Text("▾") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryMenu = true },
                        enabled = true,
                    )
                    if (categoryMenu) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        ) {
                            Column {
                                BoheCategories.LIST.forEach { (key, label) ->
                                    Text(
                                        "${
                                            when (key) {
                                                "food" -> "🍚"
                                                "transport" -> "🚌"
                                                "shop" -> "🛍"
                                                "house" -> "🏠"
                                                "fun" -> "🎮"
                                                "medical" -> "💊"
                                                "study" -> "📚"
                                                else -> "📦"
                                            }
                                        } $label",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                category = key
                                                categoryMenu = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日期：", style = MaterialTheme.typography.bodySmall)
                    Text(
                        date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { date = LocalDate.now() },
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { date = date.minusDays(1) }) { Text("‹") }
                    IconButton(onClick = { date = date.plusDays(1) }) { Text("›") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.addBill(amount, category, note, date) },
                enabled = !ui.saving,
            ) { Text(if (ui.saving) "保存中…" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissAddDialog() }) { Text("取消") }
        },
    )
}
