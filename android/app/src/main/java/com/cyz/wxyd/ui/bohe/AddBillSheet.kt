package com.cyz.wxyd.ui.bohe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyz.wxyd.WxydApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 记一笔半屏弹层（需求 2：样式完全参考小程序 bill-card）：
 * 日期 pill + 大金额 + 分类横滑 + 备注行 + 数字键盘（完成键绿色跨 2 列）。
 * 编辑模式预填 [ui.editBill]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillSheet(vm: BoheViewModel, ui: BoheUiState, app: WxydApp) {
    val editing = ui.editBill
    var amountStr by remember(editing) {
        mutableStateOf(
            editing?.let { String.format(Locale.CHINA, "%.2f", it.amount / 100.0) } ?: "",
        )
    }
    var category by remember(editing) { mutableStateOf(editing?.category ?: "food") }
    var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
    var date by remember(editing) { mutableStateOf(editing?.billDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()) }

    // 日期步进（‹/› 一天；点日期回今天）
    ModalBottomSheet(onDismissRequest = { vm.dismissAddSheet() }) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp),
        ) {
            // 顶行：日期 pill + 关闭
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📅 ${date.year}年${date.monthValue}月${date.dayOfMonth}日",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .clickable { date = LocalDate.now() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "‹",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { date = date.minusDays(1) }
                        .padding(horizontal = 8.dp),
                )
                Text(
                    "›",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { date = date.plusDays(1) }
                        .padding(horizontal = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "支出 · ${BoheCategories.label(category)}${if (editing != null) " · 编辑" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                "¥ ${amountStr.ifEmpty { "0.00" }}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3EB575),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            // 分类横滑（与小程序 cats 一致）
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BoheCategories.LIST.forEach { (key, label) ->
                    val selected = key == category
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { category = key },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (selected) Color(0xFF3EB489) else categoryColor(key),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(categoryEmoji(key), fontSize = 20.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color(0xFF2C7A5B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 备注行
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "备注",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    decorationBox = { inner ->
                        Box {
                            if (note.isEmpty()) {
                                Text(
                                    "选填，买点啥…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            // 数字键盘（4 列：⌫ / 完成 跨 2 列）
            Numpad(
                disabled = ui.saving,
                onKey = { k ->
                    when (k) {
                        "back" -> amountStr = amountStr.dropLast(1)
                        "done" -> vm.saveBill(amountStr, category, note, date)
                        "dot" -> if (!amountStr.contains('.')) amountStr = (amountStr.ifEmpty { "0" }) + "."
                        else -> {
                            // 金额上限 8 位整数 + 2 位小数
                            val next = amountStr + k
                            val (intPart, decPart) = next.split('.').let { it[0] to it.getOrNull(1) }
                            if (intPart.length <= 8 && (decPart == null || decPart.length <= 2) && next != "0") {
                                amountStr = next
                            }
                        }
                    }
                },
            )
        }
    }
}

/** 小程序 numpad 复刻：7 8 9 ⌫ / 4 5 6 （空） / 1 2 3 （空） / 0 . 完成跨2列。 */
@Composable
private fun Numpad(disabled: Boolean, onKey: (String) -> Unit) {
    val keys = listOf(
        listOf("7", "8", "9", "back"),
        listOf("4", "5", "6", null),
        listOf("1", "2", "3", null),
        listOf("0", "dot", "done", "done"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                var prevPlaceholder = false
                row.forEach { k ->
                    if (k == null) {
                        // 功能占位：隐藏单元格
                        Box(
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    } else {
                        // done 跨 2 列（第二次出现占满剩余）
                        val isDoneRepeat = k == "done" && prevPlaceholder
                        if (isDoneRepeat) {
                            prevPlaceholder = true
                            return@forEach
                        }
                        val isDone = k == "done"
                        Box(
                            modifier = Modifier
                                .weight(if (isDone) 1f else 1f)
                                .height(52.dp)
                                .background(
                                    when {
                                        isDone -> Color(0xFF3EB575)
                                        k == "back" -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                )
                                .clickable(enabled = !disabled) { onKey(k) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when (k) {
                                    "back" -> "⌫"
                                    "dot" -> "."
                                    "done" -> if (disabled) "保存中…" else "完成"
                                    else -> k
                                },
                                fontSize = 18.sp,
                                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
