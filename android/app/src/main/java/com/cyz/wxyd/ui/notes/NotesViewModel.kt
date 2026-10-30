package com.cyz.wxyd.ui.notes

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** 检索范围：我的 / 他人公开（H5 `noteScope`）。 */
enum class NoteScope(val wire: String) { MINE("mine"), PUBLIC("public") }

/** 笔记载体（服务端 `Note`：name/category/visibility/tags/content/source）。 */
data class NoteDoc(
    val name: String = "",
    val category: String = "",
    /** `public` | `private` */
    val visibility: String = "public",
    val tags: List<String> = emptyList(),
    val content: String = "",
    val source: String = "",
) {
    val isPrivate: Boolean get() = visibility == "private"
}

/** 列表行（`note/search` 数组元素：Note + `ownerName/updateTime/preview`）。 */
data class NoteItem(
    val ownerId: Long,
    val name: String,
    val category: String,
    val visibility: String,
    val tags: List<String>,
    val ownerName: String,
    val updateTime: String,
    val preview: String,
)

data class NotesUiState(
    val scope: NoteScope = NoteScope.MINE,
    val keyword: String = "",
    val loading: Boolean = true,
    val items: List<NoteItem> = emptyList(),
    val error: String? = null,
    val message: String? = null,

    /** 当前用户 id（判断是否本人的笔记，决定是否只读） */
    val myId: Long = 0,

    /** 打开/编辑的笔记（null = 不在编辑页） */
    val editing: NoteDoc? = null,
    /** 打开时的原标题，保存时作为 `oldName`（改名） */
    val originalName: String = "",
    val readOnly: Boolean = false,
    val saving: Boolean = false,
    /** 编辑页 "预览/编辑" 切换 */
    val preview: Boolean = false,
)

/**
 * 记忆笔记（计划 Task 12）。
 *
 * 接口（全部对照 H5 `h5.js` 与 `NoteController`）：
 * - 检索：`GET note/search?scope=mine|public&field=all[&keyword]`，body 是**数组** → getRaw + parseArray；
 * - 读单篇：`GET note/get?ownerId&name`（对象）；
 * - 保存 / 删除：`POST note/save|delete`，postBody 双层；保存字段
 *   `{name, category, visibility, content, tags[], oldName[, source]}`，成功后 body 是**新标题**；
 * - 导出：`GET note/export?ownerId&name`，文件流 → [com.cyz.wxyd.data.remote.FileDownloader]。
 */
class NotesViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(NotesUiState())
    val ui: StateFlow<NotesUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun setMyId(id: Long) = _ui.update { it.copy(myId = id) }

    // ---- 列表 ----

    fun setScope(scope: NoteScope) {
        if (_ui.value.scope == scope) return
        _ui.update { it.copy(scope = scope) }
        refresh()
    }

    fun onKeywordChange(v: String) = _ui.update { it.copy(keyword = v) }

    fun refresh() {
        val s = _ui.value
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val query = buildMap {
                    put("scope", s.scope.wire)
                    put("field", "all")
                    if (s.keyword.isNotBlank()) put("keyword", s.keyword.trim())
                }
                val raw = container.apiClient.getRaw("/note/search", query).use { it.body?.string().orEmpty() }
                val items = parseItems(ApiResult.parseArray(raw))
                _ui.update { it.copy(loading = false, items = items, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    // ---- 打开 / 编辑 ----

    fun newNote() = _ui.update {
        it.copy(editing = NoteDoc(), originalName = "", readOnly = false, preview = false)
    }

    /** 打开笔记：先拉全文，再进编辑页；他人笔记只读。 */
    fun open(item: NoteItem) {
        viewModelScope.launch {
            try {
                val body = container.apiClient.getResult(
                    "/note/get",
                    mapOf("ownerId" to item.ownerId.toString(), "name" to item.name),
                )
                val doc = parseNote(body)
                val mine = item.ownerId == _ui.value.myId
                _ui.update { it.copy(editing = doc, originalName = item.name, readOnly = !mine, preview = !mine) }
            } catch (e: Exception) {
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun updateEditing(transform: (NoteDoc) -> NoteDoc) =
        _ui.update { s -> s.editing?.let { s.copy(editing = transform(it)) } ?: s }

    fun closeEditing() = _ui.update { it.copy(editing = null, readOnly = false, preview = false) }

    fun togglePreview() = _ui.update { it.copy(preview = !it.preview) }

    fun save() {
        val s = _ui.value
        val doc = s.editing ?: return
        if (s.readOnly || s.saving) return
        if (doc.name.isBlank()) {
            _ui.update { it.copy(message = "请输入标题") }
            return
        }
        _ui.update { it.copy(saving = true) }
        val payload = buildJsonObject {
            put("name", doc.name.trim())
            put("category", doc.category.trim())
            put("visibility", if (doc.isPrivate) "private" else "public")
            put("content", doc.content)
            put("tags", buildJsonArray { doc.tags.forEach { add(JsonPrimitive(it)) } })
            put("oldName", s.originalName)
            // 编辑时保留来源，避免一次保存把来源页断链
            if (doc.source.isNotBlank()) put("source", doc.source)
        }
        viewModelScope.launch {
            try {
                container.apiClient.postBody("/note/save", payload)
                _ui.update { it.copy(saving = false, editing = null, readOnly = false, message = "保存成功") }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, message = e.userMessage()) }
            }
        }
    }

    fun delete(name: String) {
        viewModelScope.launch {
            try {
                container.apiClient.postBody("/note/delete", buildJsonObject { put("name", name) })
                _ui.update { it.copy(message = "已删除", editing = null) }
                refresh()
            } catch (e: Exception) {
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    /** 导出为 md（GET note/export?ownerId&name）。 */
    fun export(ownerId: Long, name: String) {
        viewModelScope.launch {
            container.fileDownloader.download(
                path = "/note/export",
                query = mapOf("ownerId" to ownerId.toString(), "name" to name),
                defaultFileName = "$name.md",
            ).onSuccess { f ->
                _ui.update { it.copy(message = "已导出：${f.absolutePath}") }
            }.onFailure { e ->
                _ui.update { it.copy(message = e.userMessage()) }
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
}

/** 解析 `note/search` 数组（同样兼容 `tags` 为数组或逗号串）。 */
internal fun parseItems(array: JsonArray): List<NoteItem> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
    NoteItem(
        ownerId = obj["ownerId"]?.jsonPrimitive?.longOrNull ?: 0L,
        name = name,
        category = obj["category"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        visibility = obj["visibility"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { "public" },
        tags = parseTags(obj["tags"]),
        ownerName = obj["ownerName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        updateTime = obj["updateTime"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        preview = obj["preview"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}

/** 解析 `note/get` 的 body（Note 对象）。 */
internal fun parseNote(body: JsonObject): NoteDoc = NoteDoc(
    name = body["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    category = body["category"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    visibility = body["visibility"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { "public" },
    tags = parseTags(body["tags"]),
    content = body["content"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    source = body["source"]?.jsonPrimitive?.contentOrNull.orEmpty(),
)

private fun parseTags(el: JsonElement?): List<String> {
    if (el == null || el is JsonNull) return emptyList()
    return when (el) {
        is JsonArray -> el.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }.filter { it.isNotEmpty() }
        // 兼容 "a,b" 字符串形式
        else -> (el as? JsonPrimitive)?.contentOrNull.orEmpty()
            .split(',', '，')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
