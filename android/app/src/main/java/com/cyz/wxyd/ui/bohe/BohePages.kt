package com.cyz.wxyd.ui.bohe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyz.wxyd.WxydApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 账单按日期分组（组头 = 日期 + 当日合计）。 */
private data class DayGroup(val date: String, val dayTotal: Long, val bills: List<BoheBillItem>)

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

/** 2026-10-31 → 「10月31日 · 周五」。 */
private fun dayTitle(date: String): String = try {
    val d = LocalDate.parse(date)
    val week = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.CHINA)
        .replace("星期", "周")
    "${d.monthValue}月${d.dayOfMonth}日 · $week"
} catch (e: Exception) {
    date
}

/**
 * 账单列表页（需求 2：点账单卡片进入；左滑出编辑/删除）。
 * 只看自己的账单可编辑；好友只读视图复用本页（readOnly=true）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoheBillsPage(vm: BoheViewModel, app: WxydApp, onBack: () -> Unit, readOnly: Boolean = false) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (readOnly) "「${ui.viewingFriendName}」的账单" else "账单") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            OverviewHeader(vm, ui)
        }
        if (ui.loading && ui.bills.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return
        }
        val groups = groupByDay(ui.bills)
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "这个月还没记账，点 + 记一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp,
            ),
        ) {
            ui.message?.let { msg ->
                item {
                    Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            groups.forEach { g ->
                item(key = "head_${g.date}") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
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
                }
                items(count = g.bills.size, key = { "bill_${g.bills[it].id}" }) { idx ->
                    SwipeableBillRow(
                        bill = g.bills[idx],
                        readOnly = readOnly,
                        onEdit = { vm.showEditSheet(g.bills[idx]) },
                        onDelete = { vm.deleteBill(g.bills[idx].id) },
                    )
                }
            }
        }
    }
}

/** 单行账单（左滑手势出 编辑/删除 按钮，参照小程序 bill-item）。 */
@Composable
private fun SwipeableBillRow(
    bill: BoheBillItem,
    readOnly: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val maxSwipe = with(density) { 140.dp.toPx() }

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        // 底下两层操作按钮（右对齐）
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.45f),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!readOnly) {
                Button(
                    onClick = { onEdit(); offsetX = 0f },
                    modifier = Modifier
                        .height(56.dp)
                        .width(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("编辑", fontSize = 13.sp) }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = { onDelete(); offsetX = 0f },
                    modifier = Modifier
                        .height(56.dp)
                        .width(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("删除", fontSize = 13.sp) }
            }
        }
        // 上层内容行：跟随手势左移
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { offsetX.toDp() })
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .pointerInput(readOnly) {
                    if (readOnly) return@pointerInput
                    var startX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { startX = it.x },
                        onDragEnd = {
                            offsetX = if (offsetX < -maxSwipe / 2) -maxSwipe else 0f
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val base = if (startX > 0f) 0f else offsetX
                        offsetX = (base + dragAmount).coerceIn(-maxSwipe, 0f)
                    }
                }
                .padding(horizontal = 10.dp),
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
                    bill.note?.takeIf { it.isNotBlank() } ?: BoheCategories.label(bill.category),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    BoheCategories.label(bill.category) + if (bill.isLocalOnly) " · 未同步" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "-¥${formatYuan(bill.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 统计页（需求 2：日/月/年粒度，OverviewHeader 已含切换）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoheStatsPage(vm: BoheViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("统计") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            OverviewHeader(vm, ui)
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "分类占比",
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
                        "该周期暂无数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val total = ui.byCategory.sumOf { it.total }.coerceAtLeast(1)
                    ui.byCategory.forEach { cs ->
                        val ratio = cs.total.toFloat() / total
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
                                    .background(Color(0x33FFFFFF), RoundedCornerShape(3.dp)),
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
        // 周期内账单明细（按当前粒度列出）
        Text(
            "明细",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 88.dp,
            ),
        ) {
            ui.bills.take(50).forEach { bill ->
                item(key = "stat_${bill.id}") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(categoryColor(bill.category), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(categoryEmoji(bill.category), fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            bill.note?.takeIf { it.isNotBlank() } ?: BoheCategories.label(bill.category),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            bill.billDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "-¥${formatYuan(bill.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
