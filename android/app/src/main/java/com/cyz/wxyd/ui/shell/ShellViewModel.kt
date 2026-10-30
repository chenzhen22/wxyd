package com.cyz.wxyd.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyz.wxyd.AppContainer
import com.cyz.wxyd.data.remote.ApiException
import com.cyz.wxyd.ui.common.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** 语法速查/示例条目（`ShellSyntax` / `ShellExample` 合并；示例才有 code）。 */
data class ShellEntry(
    val name: String,
    val signature: String,
    val description: String,
    val code: String,
) {
    /** 标题行：签名优先，否则名字。 */
    val title: String get() = signature.ifBlank { name }
}

/** 主题（`GET /shellscript/topics` 裸数组 / `GET /shellscript/topics/{name}` 裸对象 → `ShellTopic`）。 */
data class ShellTopic(
    val name: String,
    val category: String,
    val intro: String,
    val syntaxes: List<ShellEntry>,
    val examples: List<ShellEntry>,
)

data class ShellUiState(
    val loading: Boolean = true,
    val keyword: String = "",
    val topics: List<ShellTopic> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    /** 打开的主题（null = 在列表） */
    val detail: ShellTopic? = null,
    val detailLoading: Boolean = false,
    val saving: Boolean = false,
) {
    /** 按关键词过滤（名字/分类），H5 同款。 */
    val visibleTopics: List<ShellTopic>
        get() {
            val kw = keyword.trim().lowercase()
            if (kw.isEmpty()) return topics
            return topics.filter {
                it.name.lowercase().contains(kw) || it.category.lowercase().contains(kw)
            }
        }
}

/**
 * Shell 脚本学习（计划 Task 13，**只读**学习模块）。
 *
 * 契约（H5 `h5.js` `renderShell`/`openShellTopic` 对照）：
 * - `GET shellscript/topics` → **裸数组** `List<ShellTopic>`，不包 `Result` → [getJsonRaw]；
 * - `GET shellscript/topics/{name}` → **裸对象**，不存在时 404 → 提示"主题不存在"；
 * - "存笔记"：`POST note/saveFromSource`（postBody 双层 `{"body":{...}}`），
 *   `sourceKey` 必须以 `shell/` 开头（服务端白名单），content 的 Markdown 组织与 H5 一致。
 */
class ShellViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(ShellUiState())
    val ui: StateFlow<ShellUiState> = _ui.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        refresh()
    }

    fun onKeywordChange(v: String) = _ui.update { it.copy(keyword = v) }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val arr = getJsonRaw("/shellscript/topics") as? JsonArray
                    ?: throw ApiException("PARSE_ERROR", "响应格式错误：主题列表不是数组")
                val topics = arr.mapNotNull { parseTopic(it as? JsonObject) }
                _ui.update { it.copy(loading = false, topics = topics, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    /** 打开详情：列表项里已带全部内容，先直接展示，再按名拉一次详情覆盖（列表可能精简）。 */
    fun open(topic: ShellTopic) {
        _ui.update { it.copy(detail = topic, detailLoading = true) }
        viewModelScope.launch {
            try {
                val fetched = parseTopic(getJsonRaw("/shellscript/topics/${enc(topic.name)}") as? JsonObject)
                _ui.update {
                    it.copy(
                        detailLoading = false,
                        detail = fetched ?: it.detail,
                        message = if (fetched == null) "主题详情为空" else it.message,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val notFound = (e as? ApiException)?.errorCode == "404"
                _ui.update {
                    it.copy(
                        detailLoading = false,
                        message = if (notFound) "主题不存在：${topic.name}" else e.userMessage(),
                    )
                }
            }
        }
    }

    fun closeDetail() = _ui.update { it.copy(detail = null) }

    /** 把主题完整存为当前用户的**私有**笔记（R2 一键沉淀），Markdown 结构与 H5 相同。 */
    fun saveToNote(topic: ShellTopic) {
        if (_ui.value.saving) return
        _ui.update { it.copy(saving = true) }
        val payload = buildJsonObject {
            put("sourceKey", "shell/${topic.name}")
            put("sourceTitle", topic.name)
            put("sourceCategory", topic.category)
            put("content", topicMarkdown(topic))
        }
        viewModelScope.launch {
            try {
                val body = container.apiClient.postBody("/note/saveFromSource", payload)
                val savedName = body["name"]?.jsonPrimitive?.contentOrNull
                _ui.update { it.copy(saving = false, message = "已存为私有笔记" + (savedName?.let { "：$it" } ?: "")) }
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, message = e.userMessage()) }
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }

    // ------------------------------------------------------------------

    private suspend fun getJsonRaw(path: String): kotlinx.serialization.json.JsonElement {
        val raw = container.apiClient.getRaw(path).use { it.body?.string().orEmpty() }
        return try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            throw ApiException("PARSE_ERROR", "响应格式错误：${e.message}")
        }
    }

    private fun enc(name: String): String = java.net.URLEncoder.encode(name, "UTF-8")

    private fun parseTopic(obj: JsonObject?): ShellTopic? {
        if (obj == null) return null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null
        return ShellTopic(
            name = name,
            category = obj["category"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            intro = obj["intro"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            syntaxes = entries(obj, "syntaxes"),
            examples = entries(obj, "examples"),
        )
    }

    private fun entries(obj: JsonObject, key: String): List<ShellEntry> =
        (obj[key] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
            ShellEntry(name = str("name"), signature = str("signature"), description = str("description"), code = str("code"))
        }

    companion object {
        /** 与 H5 `openShellTopic` 的沉淀格式保持一致。 */
        internal fun topicMarkdown(t: ShellTopic): String = buildString {
            appendLine("## 介绍")
            appendLine(t.intro)
            appendLine("## 语法")
            t.syntaxes.forEach { s ->
                appendLine("### ${s.name}")
                if (s.signature.isNotBlank()) {
                    appendLine("```bash")
                    appendLine(s.signature)
                    appendLine("```")
                }
                if (s.description.isNotBlank()) appendLine(s.description)
            }
            appendLine("## 示例")
            t.examples.forEach { ex ->
                appendLine("### ${ex.name}")
                if (ex.description.isNotBlank()) appendLine(ex.description)
                if (ex.code.isNotBlank()) {
                    appendLine("```bash")
                    appendLine(ex.code)
                    appendLine("```")
                }
            }
        }
    }
}
