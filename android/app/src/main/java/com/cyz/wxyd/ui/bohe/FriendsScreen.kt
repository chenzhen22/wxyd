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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory

/**
 * 好友 Tab（需求 3）：顶部模糊搜索加好友 + 待处理申请横幅 + 好友列表。
 * 点好友 → 弹窗（薄荷账入口 → 查看对方账单/统计，只读）。
 */
@Composable
fun FriendsScreen(app: WxydApp, onRemovedSelf: () -> Unit = {}) {
    val vm: BoheViewModel = viewModel(factory = appFactory(app) { BoheViewModel(it.container) })
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        ui.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        // 搜索框（需求 3：模糊搜索添加好友）
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.searchKeyword,
                onValueChange = { vm.onSearchChange(it) },
                placeholder = { Text("搜索用户名 / 昵称加好友") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.search() }, enabled = !ui.searching) {
                Text(if (ui.searching) "…" else "搜索")
            }
        }
        // 搜索结果
        ui.searchResults.forEach { u ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(u.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        u.username ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { vm.addFriend(u.userId) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) { Text("加好友") }
            }
        }
        Spacer(Modifier.height(12.dp))
        // 待处理申请横幅（需求 3：右侧提示）
        val pending = ui.friends.filter { f -> f.status != 1 && !f.outgoing }
        if (pending.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "${pending.size} 个好友申请待处理",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    pending.forEach { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(f.label)
                            Spacer(Modifier.width(10.dp))
                            Text(f.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Button(
                                onClick = { vm.handleFriend(f.userId, true) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            ) { Text("通过") }
                            Spacer(Modifier.width(6.dp))
                            TextButton(onClick = { vm.handleFriend(f.userId, false) }) { Text("拒绝") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        // 好友列表
        val accepted = ui.friends.filter { it.status == 1 }
        if (accepted.isEmpty() && pending.isEmpty() && ui.searchResults.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "还没有好友\n搜索对方用户名，通过后可互相查看账单",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                modifier = Modifier.weight(1f),
            ) {
                accepted.forEach { f ->
                    item(key = "friend_${f.userId}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .clickable { vm.openFriend(f) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(f.label)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "已互相关注",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                ui.friends.filter { it.status != 1 && it.outgoing }.forEach { f ->
                    item(key = "wait_${f.userId}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(f.label, dim = true)
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
            }
        }
    }

    // 好友查看模式：全屏账单页（复用账单页 readOnly）
    if (ui.viewingFriendId != null) {
        Box(Modifier.fillMaxSize()) {
            BoheBillsPage(vm = vm, app = app, onBack = { vm.backToMe() }, readOnly = true)
        }
    }
}

@Composable
private fun Avatar(name: String, dim: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                (if (dim) Color(0xFFF1F5F9) else Color(0xFF3EB489).copy(alpha = 0.2f)),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1), style = MaterialTheme.typography.titleSmall)
    }
}
