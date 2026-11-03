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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import com.cyz.wxyd.ui.common.userMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 分类 emoji（与小程序视觉一致，未知分类兜底 📦）。 */
fun categoryEmoji(key: String): String = when (key) {
    "food" -> "🍚"
    "transport" -> "🚌"
    "shop" -> "🛍"
    "house" -> "🏠"
    "fun" -> "🎮"
    "medical" -> "💊"
    "study" -> "📚"
    else -> "📦"
}

/** 分类色板（浅色底，账单/统计/新增共用）。 */
fun categoryColor(key: String): Color = when (key) {
    "food" -> Color(0xFFFFEDD5)
    "transport" -> Color(0xFFDBEAFE)
    "shop" -> Color(0xFFF3E8FF)
    "house" -> Color(0xFFDCFCE7)
    "fun" -> Color(0xFFFEF3C7)
    "medical" -> Color(0xFFFEE2E2)
    "study" -> Color(0xFFE0E7FF)
    else -> Color(0xFFF1F5F9)
}

fun formatYuan(cents: Long): String = String.format(Locale.CHINA, "%.2f", cents / 100.0)

/** 薄荷账主 Tab：渐变总览 + 账单/统计两张入口卡（点击进子页），FAB 直接记账。 */
@Composable
fun BoheScreen(app: WxydApp, sessionViewModel: com.cyz.wxyd.ui.session.SessionViewModel) {
    val vm: BoheViewModel = viewModel(factory = appFactory(app) { BoheViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var showBillsPage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showStatsPage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    // 子页压栈显示（不显示底部栏由 MainScreen 的模块页范式处理不了内嵌状态，这里用全屏覆盖）
    if (showBillsPage) {
        BoheBillsPage(vm = vm, app = app, onBack = { showBillsPage = false })
    } else if (showStatsPage) {
        BoheStatsPage(vm = vm, onBack = { showStatsPage = false })
    } else {
        Scaffold(
            floatingActionButton = {
                if (ui.viewingFriendId == null) {
                    androidx.compose.material3.FloatingActionButton(onClick = { vm.showAddSheet() }) {
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
                ui.message?.let { msg ->
                    item {
                        Text(
                            msg,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                // 好友查看模式横幅
                if (ui.viewingFriendId != null) {
                    item { FriendViewingBanner(vm, ui) }
                }
                item {
                    EntryCard(
                        emoji = "🧾",
                        title = "账单",
                        subtitle = if (ui.bills.isEmpty()) "暂无账单" else "${ui.bills.size} 笔 · 本期 ¥${formatYuan(ui.monthTotal)}",
                        onClick = { showBillsPage = true },
                    )
                }
                item {
                    EntryCard(
                        emoji = "📊",
                        title = "统计",
                        subtitle = "按${ui.granularity.label}统计 · 支出 ¥${formatYuan(ui.monthTotal)}",
                        onClick = { showStatsPage = true },
                    )
                }
            }
        }
    }

    if (ui.showAddSheet) {
        AddBillSheet(vm = vm, ui = ui, app = app)
    }
}

/** 入口卡：emoji + 标题 + 副标题，点按进子页。 */
@Composable
private fun EntryCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3EB489).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 渐变总览头图：本期支出大数字 + 粒度切换 + 期切换 + 好友查看横幅。 */
@Composable
fun OverviewHeader(vm: BoheViewModel, ui: BoheUiState) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3EB489), Color(0xFF2C7A5B))),
                    RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 粒度切换（日/月/年）
                Row {
                    StatsGranularity.entries.forEach { g ->
                        val selected = g == ui.granularity
                        Text(
                            g.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { vm.setGranularity(g) }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButtonWhite("‹") { vm.prevPeriod() }
                Text(
                    ui.month.take(
                        when (ui.granularity) {
                            StatsGranularity.DAY -> 10
                            StatsGranularity.MONTH -> 7
                            StatsGranularity.YEAR -> 4
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                TextButtonWhite("›") { vm.nextPeriod() }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "支出（元）",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                formatYuan(ui.monthTotal),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            if (ui.viewingFriendId == null && ui.bills.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "共 ${ui.bills.size} 笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun TextButtonWhite(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 20.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** 好友只读查看模式横幅。 */
@Composable
fun FriendViewingBanner(vm: BoheViewModel, ui: BoheUiState) {
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
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        androidx.compose.material3.TextButton(onClick = { vm.backToMe() }) { Text("切回我") }
    }
}
