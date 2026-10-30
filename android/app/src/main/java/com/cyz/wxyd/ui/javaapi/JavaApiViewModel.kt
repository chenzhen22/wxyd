package com.cyz.wxyd.ui.javaapi

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder

/** 类列表项（`GET /javaapi/classes` 裸数组 → `ApiClass`，列表时 methods/testCases 可能为空）。 */
data class ApiClassItem(
    val name: String,
    val packageName: String,
    val intro: String,
    val methods: List<ApiMethodItem>,
    val testCases: List<ApiTestCase>,
)

data class ApiMethodItem(val name: String, val signature: String, val description: String) {
    val title: String get() = signature.ifBlank { name }
}

data class ApiTestCase(
    val name: String,
    val description: String,
    val assertion: String,
    val code: String,
    val expectedOutput: String,
)

/** `POST .../test` 返回的 `CodeResult`（同上，裸对象）。 */
data class RunResult(
    val success: Boolean,
    val passed: Boolean,
    val output: String,
    val error: String,
    val executionTime: Long?,
)

data class JavaApiUiState(
    val loading: Boolean = true,
    val keyword: String = "",
    val classes: List<ApiClassItem> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    /** 详情（null = 在列表） */
    val detail: ApiClassItem? = null,
    val detailLoading: Boolean = false,
    /** 当前编辑的测试用例序号 */
    val caseIndex: Int = 0,
    /** 当前编辑的代码（切换用例时由用例 code 填充） */
    val code: String = "",
    val running: Boolean = false,
    val runResult: RunResult? = null,
    val saving: Boolean = false,
) {
    val visibleClasses: List<ApiClassItem>
        get() {
            val kw = keyword.trim().lowercase()
            if (kw.isEmpty()) return classes
            return classes.filter { it.name.lowercase().contains(kw) || it.packageName.lowercase().contains(kw) }
        }
}

/**
 * Java 8 API 学习（计划 Task 13，**只读**学习模块 + 在线运行）。
 *
 * 契约（H5 `h5-javaapi.js` 对照）：
 * - `GET javaapi/classes` → **裸 List** → [getJsonRaw]；
 * - `GET javaapi/classes/{name}` → 裸 `ApiClass`；
 * - `POST javaapi/classes/{name}/test`：**直接字段** `{testName, code}`（非 `body` 双层），
 *   返回裸 `CodeResult`；服务端 400 时也回 CodeResult —— 走 [ApiClient.postDirectRaw] 拿状态码+正文；
 * - "存笔记"：`note/saveFromSource` postBody，`sourceKey = javaapi/{name}`。
 */
class JavaApiViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(JavaApiUiState())
    val ui: StateFlow<JavaApiUiState> = _ui.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        refresh()
    }

    fun onKeywordChange(v: String) = _ui.update { it.copy(keyword = v) }

    fun refresh() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val arr = getJsonRaw("/javaapi/classes") as? JsonArray
                    ?: throw ApiException("PARSE_ERROR", "响应格式错误：类列表不是数组")
                _ui.update { it.copy(loading = false, classes = arr.mapNotNull { parseClass(it as? JsonObject) }, error = null) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun open(item: ApiClassItem) {
        _ui.update { it.copy(detail = item, detailLoading = true, runResult = null, caseIndex = 0, code = item.testCases.firstOrNull()?.code.orEmpty()) }
        viewModelScope.launch {
            try {
                val c = parseClass(getJsonRaw("/javaapi/classes/${enc(item.name)}") as? JsonObject)
                _ui.update {
                    it.copy(
                        detailLoading = false,
                        detail = c ?: it.detail,
                        runResult = null,
                        caseIndex = 0,
                        code = c?.testCases?.firstOrNull()?.code ?: it.code,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(detailLoading = false, message = e.userMessage()) }
            }
        }
    }

    fun closeDetail() = _ui.update { it.copy(detail = null, runResult = null) }

    /** 切换测试用例：用该用例自带 code 覆盖编辑区（H5 `fill(i)` 同）。 */
    fun selectCase(index: Int) {
        val c = _ui.value.detail ?: return
        val tc = c.testCases.getOrNull(index) ?: return
        _ui.update { it.copy(caseIndex = index, code = tc.code, runResult = null) }
    }

    fun onCodeChange(v: String) = _ui.update { it.copy(code = v) }

    /** 在线运行（服务端 10 秒超时 + 安全黑名单）。 */
    fun run() {
        val state = _ui.value
        val c = state.detail ?: return
        val tc = c.testCases.getOrNull(state.caseIndex) ?: return
        if (state.code.isBlank()) {
            _ui.update { it.copy(message = "代码不能为空") }
            return
        }
        if (state.running) return
        _ui.update { it.copy(running = true, runResult = null) }
        viewModelScope.launch {
            try {
                val (_, raw) = container.apiClient.postDirectRaw(
                    "/javaapi/classes/${enc(c.name)}/test",
                    buildJsonObject {
                        put("testName", tc.name)
                        put("code", state.code)
                    },
                )
                val result = parseRunResult(raw)
                _ui.update {
                    it.copy(
                        running = false,
                        runResult = result,
                        message = if (result == null) "运行失败：响应无法解析" else it.message,
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(running = false, message = e.userMessage()) }
            }
        }
    }

    /** 把 API 条目（方法 + 可运行示例）存为私有笔记，Markdown 结构与 H5 相同。 */
    fun saveToNote(item: ApiClassItem) {
        if (_ui.value.saving) return
        _ui.update { it.copy(saving = true) }
        val payload = buildJsonObject {
            put("sourceKey", "javaapi/${item.name}")
            put("sourceTitle", item.name)
            put("sourceCategory", "Java API")
            put("content", classMarkdown(item))
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

    private suspend fun getJsonRaw(path: String): JsonElement {
        val raw = container.apiClient.getRaw(path).use { it.body?.string().orEmpty() }
        return try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            throw ApiException("PARSE_ERROR", "响应格式错误：${e.message}")
        }
    }

    private fun enc(name: String): String = URLEncoder.encode(name, "UTF-8")

    private fun parseClass(obj: JsonObject?): ApiClassItem? {
        if (obj == null) return null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null
        fun list(key: String): List<JsonObject> =
            (obj[key] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        fun Str(o: JsonObject, k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
        return ApiClassItem(
            name = name,
            packageName = Str(obj, "packageName"),
            intro = Str(obj, "intro"),
            methods = list("methods").map {
                ApiMethodItem(name = Str(it, "name"), signature = Str(it, "signature"), description = Str(it, "description"))
            },
            testCases = list("testCases").map {
                ApiTestCase(
                    name = Str(it, "name"),
                    description = Str(it, "description"),
                    assertion = Str(it, "assertion"),
                    code = Str(it, "code"),
                    expectedOutput = Str(it, "expectedOutput"),
                )
            },
        )
    }

    private fun parseRunResult(raw: String): RunResult? {
        val obj = try {
            json.parseToJsonElement(raw) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return null
        fun bool(k: String) = obj[k]?.jsonPrimitive?.contentOrNull == "true"
        fun str(k: String) = obj[k]?.jsonPrimitive?.contentOrNull.orEmpty()
        return RunResult(
            success = bool("success"),
            passed = bool("passed"),
            output = str("output"),
            error = str("error"),
            executionTime = obj["executionTime"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
        )
    }

    companion object {
        /** 与 H5 `h5-javaapi.js` 的沉淀格式保持一致。 */
        internal fun classMarkdown(c: ApiClassItem): String = buildString {
            appendLine("## 介绍")
            appendLine(c.intro)
            appendLine("## 方法")
            c.methods.forEach { m ->
                appendLine("### ${m.title}")
                if (m.description.isNotBlank()) appendLine(m.description)
            }
            appendLine("## 可运行示例")
            c.testCases.forEach { t ->
                appendLine("### ${t.name}")
                if (t.description.isNotBlank()) appendLine(t.description)
                if (t.code.isNotBlank()) {
                    appendLine("```java")
                    appendLine(t.code)
                    appendLine("```")
                }
            }
        }
    }
}
