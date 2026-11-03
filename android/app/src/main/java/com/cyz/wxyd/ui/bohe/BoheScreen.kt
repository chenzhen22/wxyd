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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

/**
 * 分类主题色（浅色底 / 深色字对比：色块底 + emoji，账单与统计共用同一色板）。
 */
private fun categoryColor(key: String): Color = when (key) {
    "food" -> Color(0xFFFFEDD5)     // 橙
    "transport" -> Color(0xFFDBEAFE) // 蓝
    "shop" -> Color(0xFFF3E8FF)     // 紫
    "house" -> Color(0xFFDCFCE7)    // 绿
    "fun" -> Color(0xFFFEF3C7)      // 黄
    "medical" -> Color(0xFFFEE2E2)  // 红
    "study" -> Color(0xFFE0E7FF)    // 靛
    else -> Color(0xFFF1F5F9)       // 灰
}

private fun formatYuan(cents: Long): String = String.format(Locale.CHINA, "%.2f", cents / 100.0)

/** 账单按日期分组后的一个分组（日期头 + 当日合计）。 */
private data class DayGroup(val date: String, val dayTotal: Long, val bills: List<BoheBillItem>)

/** 服务端已按 bill_date DESC 排序，顺序聚合同日账单即可。 */
private fun groupByDay(bills: List<BoheBillItem>): List<DayGroup> {
    val out = mutableListOf<DayGroup>()
    for (b in bills) {
        val last = out.lastOrNull()
        if (last != null && last.date == b.billDate) {
            out[out.size - 1] = last.copy(bills = last.bills + b, dayTotal = last.dayTotal + b.amount)
        } else {
            out.add(DayGroup(b.billDate, b.amount, listOf(b)))
        }
    }
    return out
}

/** 2026-10-31 → 「10月31日 · 周五」。解析失败回退原文。 */
private fun dayTitle(date: String): String = try {
    val d = LocalDate.parse(date)
    val week = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.CHINA)
        .replace("星期", "周")
    "${d.monthValue}月${d.dayOfMonth}日 · $week"
} catch (e: Exception) {
    date
}

/** 薄荷账主页：渐变总览头图 + 按日分组账单流 + 分类统计 + 我的关注 + 记账 FAB（超管另有导入入口）。 */
@Composable
fun BoheScreen(app: WxydApp, sessionViewModel: com.cyz.wxyd.ui.session.SessionViewModel) {
    val vm: BoheViewModel = viewModel(factory = appFactory(app) { BoheViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val session by sessionViewModel.session.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        floatingActionButton = {
            if (ui.viewingFriendId == null) {
                FloatingActionButton(onClick = { vm.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp,
            ),
        ) {
            item { OverviewHeader(vm, ui) }
            ui.error?.let {
                item {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            ui.message?.let {
                item {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
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

    if (ui.showAddDialog) {
        AddBillDialog(vm, ui)
    }
}

/** 好友只读查看模式的提示横幅（放在头图下、内容上）。 */
@Composable
private fun FriendViewingBanner(vm: BoheViewModel, ui: BoheUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
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

/**
 * 顶部总览头图：薄荷绿渐变 + 本月支出大数字 + 月份切换 + 日均/笔数。
 * 日均 = 月合计 / 当月已过天数（未来月份按整月天数）。
 */
@Composable
private fun OverviewHeader(vm: BoheViewModel, ui: BoheUiState) {
    val dayCount = run {
        val y = ui.month.substringBefore("-").toIntOrNull() ?: return@run 1
        val m = ui.month.substringAfter("-").toIntOrNull() ?: return@run 1
        val len = java.time.YearMonth.of(y, m).lengthOfMonth()
        val today = LocalDate.now()
        if (y == today.year && m == today.monthValue) today.dayOfMonth else len
    }
    val daily = ui.monthTotal / dayCount.coerceAtLeast(1)
    val billCount = ui.bills.size

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3EB489), Color(0xFF2C7A5B))),
                    RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "本月支出（元）",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.prevMonth() }) {
                        Text("‹", color = Color.White, fontSize = 20.sp)
                    }
                    Text(
                        ui.month,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { vm.nextMonth() }) {
                        Text("›", color = Color.White, fontSize = 20.sp)
                    }
                }
                Text(
                    formatYuan(ui.monthTotal),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "日均 ¥${formatYuan(daily)} · $billCount 笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        if (ui.viewingFriendId != null) {
            Spacer(Modifier.height(8.dp))
            FriendViewingBanner(vm, ui)
        }
    }
}

/** 账单流：按日期分组，组头「10月31日 · 周五 · 当日合计」，逐笔分类色块 + emoji。 */
@Composable
private fun BillsCard(vm: BoheViewModel, ui: BoheUiState) {
    val groups = remember(ui.bills) { groupByDay(ui.bills) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("账单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (groups.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "这个月还没记账，点 + 记一笔",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                groups.forEach { g ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            dayTitle(g.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "-¥${formatYuan(g.dayTotal)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    g.bills.forEach { bill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(categoryColor(bill.category), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(categoryEmoji(bill.category), fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    bill.note?.takeIf { it.isNotBlank() }
                                        ?: BoheCategories.label(bill.category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    BoheCategories.label(bill.category),
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
                                        .clickable { vm.deleteBill(bill.id) },
                                )
                            }
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

/** 统计卡：分类占比进度条，色板与账单色块一致；顶部附本月最大单笔。 */
@Composable
private fun StatsCard(ui: BoheUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                ui.bills.maxByOrNull { it.amount }?.let { top ->
                    Text(
                        "最大单笔 ¥${formatYuan(top.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (ui.byCategory.isEmpty()) {
                Text(
                    "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val monthTotal = ui.byCategory.sumOf { it.total }.coerceAtLeast(1)
                ui.byCategory.forEach { cs ->
                    val ratio = cs.total.toFloat() / monthTotal
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "${categoryEmoji(cs.category)} ${BoheCategories.label(cs.category)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "¥${formatYuan(cs.total)} · ${(ratio * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ratio)
                                    .height(6.dp)
                                    .background(categoryColor(cs.category), RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 我的关注：待处理申请横幅置顶，已通过好友头像行，底部加好友输入。 */
@Composable
private fun FriendsCard(vm: BoheViewModel, ui: BoheUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("我的关注", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val pending = ui.friends.filter { f -> f.status != 1 && !f.outgoing }
            if (pending.isNotEmpty()) {
                pending.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "申请关注你",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = { vm.handleFriend(f.userId, true) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 0.dp,
                            ),
                        ) { Text("通过") }
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = { vm.handleFriend(f.userId, false) }) { Text("拒绝") }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            val accepted = ui.friends.filter { it.status == 1 }
            if (accepted.isEmpty() && pending.isEmpty()) {
                Text(
                    "还没有好友。输入对方用户名申请关注，通过后可互相查看账单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                accepted.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.openFriend(f) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF3EB489).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(f.label.take(1), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "查看 ›",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "删",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { vm.removeFriend(f.userId) },
                        )
                    }
                }
                val waiting = ui.friends.filter { it.status != 1 && it.outgoing }
                waiting.forEach { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(f.label.take(1), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${f.label} · 等待对方通过",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
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

/** 记一笔弹窗：4×2 分类网格（emoji+色块选中态）、金额（元）、备注、日期默认今天。 */
@Composable
private fun AddBillDialog(vm: BoheViewModel, ui: BoheUiState) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("food") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }

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
                Spacer(Modifier.height(12.dp))
                // 4×2 分类网格：选中描边高亮
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoheCategories.LIST.chunked(4).forEach { rowCats ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowCats.forEach { (key, label) ->
                                val selected = key == category
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clipCircle(selected)
                                        .clickable { category = key }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (selected) Color(0xFF3EB489) else categoryColor(key),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(categoryEmoji(key), fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color(0xFF2C7A5B)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
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

/** 选中分类加薄荷绿描边；未选中无边框。 */
private fun Modifier.clipCircle(selected: Boolean): Modifier =
    if (selected) {
        this.then(
            Modifier.background(Color.Transparent, CircleShape),
        )
    } else {
        this
    }
