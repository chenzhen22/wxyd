package com.cyz.wxyd.ui.welfare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyz.wxyd.ui.robot.RobotItem

/**
 * 羊毛雷达区块（计划 Task 8 Step 3；spec 决策 D12：**welfare 页内子区块，不是独立首页卡片**）。
 *
 * 订阅列表 + 新增（[ModalBottomSheet]）+ 启停 / 测试 / 删除；全部走 `/radar/*`（postBody 双层）。
 * 入口是一个"📡 羊毛雷达"按钮，但仍在福利页里，不占用工作台卡片位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarSection(
    radar: RadarUiState,
    onReload: () -> Unit,
    onAdd: (keyword: String, channel: String, robotId: Long?, email: String) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onTest: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onConsumeMessage: () -> Unit,
    onConsumeError: () -> Unit,
    showMessage: suspend (String) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(radar.message) { radar.message?.let { showMessage(it); onConsumeMessage() } }
    LaunchedEffect(radar.error) { radar.error?.let { showMessage(it); onConsumeError() } }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📡 羊毛雷达", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "管理") }
            }
            Text(
                "关键词命中新帖时自动推送（只推新帖）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                if (radar.loading && radar.subs.isEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                if (!radar.loading && radar.subs.isEmpty()) {
                    Text("还没有订阅", style = MaterialTheme.typography.bodySmall)
                }
                radar.subs.forEach { sub ->
                    RadarRow(
                        sub = sub,
                        onToggle = { onToggle(sub.id, !sub.enabled) },
                        onTest = { onTest(sub.id) },
                        onDelete = { onDelete(sub.id) },
                    )
                }
                FilledTonalButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("＋ 添加订阅")
                }
            }
        }
    }

    if (showAdd) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAdd = false }, sheetState = sheetState) {
            AddRadarForm(
                robots = radar.robots,
                onSubmit = { kw, ch, rid, em ->
                    showAdd = false
                    onAdd(kw, ch, rid, em)
                },
            )
        }
    }
}

@Composable
private fun RadarRow(sub: RadarSub, onToggle: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit) {
    val target = if (sub.channel == "dingtalk") "机器人#${sub.robotId ?: "-"}" else sub.email
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🔍 ${sub.keyword}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "→ ${if (sub.channel == "dingtalk") "钉钉" else "邮箱"} · $target${if (sub.enabled) "" else " · 已暂停"}",
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

/** 新增订阅表单：通道二选一，钉钉通道必须选机器人，邮件通道填邮箱（服务端同校验）。 */
@Composable
private fun AddRadarForm(
    robots: List<RobotItem>,
    onSubmit: (keyword: String, channel: String, robotId: Long?, email: String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("dingtalk") }
    var robotId by remember { mutableStateOf(robots.firstOrNull()?.id) }
    var email by remember { mutableStateOf("") }
    val canSubmit = keyword.isNotBlank() &&
        (channel == "email" && EMAIL_REGEX.matches(email.trim()) || channel == "dingtalk" && robotId != null)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("新增订阅", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it.take(64) },
            label = { Text("关键词（如：VPS / 吃喝）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = channel == "dingtalk", onClick = { channel = "dingtalk" }, label = { Text("钉钉推送") })
            FilterChip(selected = channel == "email", onClick = { channel = "email" }, label = { Text("邮件推送") })
        }
        if (channel == "dingtalk") {
            if (robots.isEmpty()) {
                Text(
                    "还没有钉钉机器人，请先到「钉钉机器人」模块添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text("钉钉机器人", style = MaterialTheme.typography.bodySmall)
                robots.forEach { robot ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = robot.id == robotId,
                            onClick = { robotId = robot.id },
                            label = { Text(robot.name) },
                        )
                    }
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
                onSubmit(
                    keyword.trim(),
                    channel,
                    if (channel == "dingtalk") robotId else null,
                    if (channel == "email") email.trim() else "",
                )
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("添加订阅") }
    }
}

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
