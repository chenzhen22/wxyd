package com.cyz.wxyd.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiResult
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File

/** 共享文件一行（`GET /share/list` → `body[]`：name/size/lastModified）。 */
data class ShareFileItem(
    val name: String,
    val size: Long,
    val lastModified: Long,
)

data class ShareUiState(
    val loading: Boolean = true,
    val files: List<ShareFileItem> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    /** 仅管理员可上传/删除（服务端同样校验） */
    val isAdmin: Boolean = false,
    val uploading: Boolean = false,
    val downloading: String? = null,
)

/**
 * 文件共享（计划 Task 12）。
 *
 * - 列表：`GET share/list`，body 是数组 → getRaw + [ApiResult.parseArray]；
 * - 下载：`GET share/download?name`，成功回文件流、**失败回 HTTP 404** →
 *   [com.cyz.wxyd.data.remote.FileDownloader] 映射为「文件不存在或无权访问」（Review Focus #5）；
 * - 上传 `POST share/upload`（multipart，字段名 `files`）与删除 `POST share/delete`（postBody 双层）
 *   都仅管理员可见、可用。
 */
class ShareViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(ShareUiState())
    val ui: StateFlow<ShareUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun setAdmin(isAdmin: Boolean) = _ui.update { it.copy(isAdmin = isAdmin) }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val raw = container.apiClient.getRaw("/share/list").use { it.body?.string().orEmpty() }
                val files = parseFiles(ApiResult.parseArray(raw))
                _ui.update { it.copy(loading = false, files = files, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun download(name: String) {
        if (_ui.value.downloading != null) return
        _ui.update { it.copy(downloading = name) }
        viewModelScope.launch {
            container.fileDownloader.download(
                path = "/share/download",
                query = mapOf("name" to name),
                defaultFileName = name,
            ).onSuccess { f: File ->
                _ui.update { it.copy(downloading = null, message = "已保存到：${f.absolutePath}") }
            }.onFailure { e ->
                _ui.update { it.copy(downloading = null, message = e.userMessage()) }
            }
        }
    }

    /** 上传单个文件（管理员）。服务端字段名是 `files`。 */
    fun upload(file: File) {
        if (!_ui.value.isAdmin || _ui.value.uploading) return
        _ui.update { it.copy(uploading = true) }
        viewModelScope.launch {
            try {
                container.apiClient.postMultipart("/share/upload", emptyMap(), mapOf("files" to file))
                _ui.update { it.copy(uploading = false, message = "上传成功") }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(uploading = false, message = e.userMessage()) }
            }
        }
    }

    fun delete(name: String) {
        if (!_ui.value.isAdmin) return
        viewModelScope.launch {
            try {
                container.apiClient.postBody("/share/delete", buildJsonObject { put("name", name) })
                _ui.update { it.copy(message = "已删除") }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
}

internal fun parseFiles(array: JsonArray): List<ShareFileItem> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
    ShareFileItem(
        name = name,
        size = obj["size"]?.jsonPrimitive?.longOrNull ?: 0L,
        lastModified = obj["lastModified"]?.jsonPrimitive?.longOrNull ?: 0L,
    )
}
