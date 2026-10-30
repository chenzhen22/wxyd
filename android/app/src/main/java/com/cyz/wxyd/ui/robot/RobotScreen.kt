package com.cyz.wxyd.ui.robot

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 钉钉机器人（计划 Task 11）。
 *
 * 列表 + 新增/编辑（[ModalBottomSheet]）+ 删除（二次确认）+ 发送（文本或文件）。
 * 请求体三类格式见 [RobotViewModel] 类注释 —— add/update/delete 是**直接字段**，
 * send 是 **multipart**，与其它模块的双层 `{body:{}}` 不同。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotScreen(app: WxydApp, onBack: () -> Unit) {
    val vm: RobotViewModel = viewModel(factory = appFactory(app) { RobotViewModel(it.container) })
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<RobotItem?>(null) }

    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("钉钉机器人") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                actions = { TextButton(onClick = vm::openCreate) { Text("＋ 新增") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading && ui.robots.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ui.error != null && ui.robots.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = vm::refresh) { Text("重试") }
                }

                ui.robots.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("暂无机器人，点右上角新增", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ui.robots, key = { it.id }) { robot ->
                        RobotCard(
                            robot = robot,
                            onSend = { vm.openSend(robot) },
                            onEdit = { vm.openEdit(robot) },
                            onDelete = { pendingDelete = robot },
                        )
                    }
                }
            }
        }
    }

    ui.editing?.let { form ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::closeEdit, sheetState = sheetState) {
            EditForm(
                form = form,
                busy = ui.busy,
                onChange = vm::updateEdit,
                onSave = vm::save,
            )
        }
    }

    ui.sending?.let { form ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::closeSend, sheetState = sheetState) {
            SendForm(
                form = form,
                busy = ui.busy,
                onChange = vm::updateSend,
                onSend = vm::send,
            )
        }
    }

    pendingDelete?.let { robot ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除机器人") },
            text = { Text("确定删除「${robot.name}」？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    vm.delete(robot.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RobotCard(robot: RobotItem, onSend: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(robot.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "Token 尾号：" + robot.accessToken.takeLast(6).ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onSend) { Text("发送") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EditForm(
    form: RobotEditForm,
    busy: Boolean,
    onChange: ((RobotEditForm) -> RobotEditForm) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (form.isEdit) "编辑机器人" else "新增机器人", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> onChange { f -> f.copy(name = v) } },
            label = { Text("名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.accessToken,
            onValueChange = { v -> onChange { f -> f.copy(accessToken = v) } },
            label = { Text("ACCESS_TOKEN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.secret,
            onValueChange = { v -> onChange { f -> f.copy(secret = v) } },
            label = { Text(if (form.isEdit) "SECRET（留空则不修改）" else "SECRET") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        FilledTonalButton(
            onClick = onSave,
            enabled = form.canSave && !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (busy) "保存中…" else "保存") }
    }
}

@Composable
private fun SendForm(
    form: RobotSendForm,
    busy: Boolean,
    onChange: ((RobotSendForm) -> RobotSendForm) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 系统文件选择器 → 复制到缓存目录（multipart 需要真实 File）
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val file = copyToCache(context, uri)
                onChange { f -> f.copy(file = file) }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("发送消息 · ${form.robotName}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.type == RobotSendType.TEXT,
                onClick = { onChange { f -> f.copy(type = RobotSendType.TEXT) } },
                label = { Text("纯文本") },
            )
            FilterChip(
                selected = form.type == RobotSendType.FILE,
                onClick = { onChange { f -> f.copy(type = RobotSendType.FILE) } },
                label = { Text("文件") },
            )
        }
        if (form.type == RobotSendType.TEXT) {
            OutlinedTextField(
                value = form.text,
                onValueChange = { v -> onChange { f -> f.copy(text = v) } },
                label = { Text("内容") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextButton(onClick = { picker.launch("*/*") }) {
                Text(if (form.file == null) "选择文件" else "已选择：${form.file.name}（重新选择）")
            }
        }
        FilledTonalButton(
            onClick = onSend,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (busy) "发送中…" else "发送") }
    }
}

/** 把 content Uri 复制到 cacheDir 的临时文件（用 Uri 最后一段作文件名）。 */
private suspend fun copyToCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    runCatching {
        val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "upload.bin"
        val out = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        out
    }.getOrNull()
}
