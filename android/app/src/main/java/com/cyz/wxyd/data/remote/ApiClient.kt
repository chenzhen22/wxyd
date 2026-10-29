package com.cyz.wxyd.data.remote

import com.cyz.wxyd.data.local.SessionCookieJar
import com.cyz.wxyd.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OkHttp 轻封装网络层（spec 第 7 节：手写 OkHttp，不用 Retrofit）。
 *
 * 职责集中处理：
 * 1. 所有请求加 `X-Requested-With: XMLHttpRequest` 头（spec 约束 2，
 *    否则后端 AuthInterceptor 对未登录返回 302 而非 401）；
 * 2. Base URL 运行时可变：每次请求动态从 [settingsStore] 读取拼接
 *    （用户在"我的"里改地址后无需重建客户端；SettingsStore 未设值时回退构造参数）；
 * 3. 响应 401 → 抛 [SessionExpiredException]（清会话由上层处理，spec 约束 1）；
 * 4. JSON 请求体统一 `application/json; charset=utf-8`（[RequestBodyFactory]）；
 * 5. 统一解析 `Result { errorCode, errorMsg, body }`，成功码 `"000000"`
 *    （[getResult] / [postBody] / [postDirect] / [postMultipart]）；
 * 6. 裸对象/数组响应与文件流走 [getRaw]，不做 Result 解包
 *    （`shellscript/topics*`、`javaapi/classes*`、`share/download` 等）。
 *
 * 请求体三类格式见 [RequestBodyFactory] 的注释表 —— 业务层不得自行拼 JSON body。
 *
 * 本类不做 SM4 处理（spec 约束 10，后端已处理加解密）。
 */
class ApiClient(
    baseUrl: String,
    private val cookieJar: SessionCookieJar,
    private val settingsStore: SettingsStore? = null,
) {

    /** 构造时传入的默认 Base URL；[settingsStore] 中有值时优先用后者。 */
    private val fallbackBaseUrl: String = baseUrl.trim().trimEnd('/')

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // 明文 HTTP / 自签证书放行由 res/xml/network_security_config.xml 承担（spec 约束 12）
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(HEADER_X_REQUESTED_WITH, "XMLHttpRequest")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * GET，解析统一 `Result`。errorCode != "000000" 抛 [ApiException]；
     * HTTP 401 抛 [SessionExpiredException]。返回 `Result.body`。
     */
    suspend fun getResult(path: String, query: Map<String, String> = emptyMap()): JsonObject {
        val raw = execute(buildRequest(path, query))
        return parseResultBody(raw)
    }

    /** POST `{"body": {...}}` 双层格式（多数业务接口，见 RequestBodyFactory 表）。 */
    suspend fun postBody(path: String, bodyFields: JsonObject): JsonObject {
        val raw = execute(buildRequest(path).post(RequestBodyFactory.body(bodyFields)).build())
        return parseResultBody(raw)
    }

    /** POST 直接字段 JSON（`robot/{add,update,delete}`、`javaapi/.../test`）。 */
    suspend fun postDirect(path: String, fields: JsonObject): JsonObject {
        val raw = execute(buildRequest(path).post(RequestBodyFactory.direct(fields)).build())
        return parseResultBody(raw)
    }

    /** POST multipart/form-data（`robot/send`、`share/upload`）。 */
    suspend fun postMultipart(
        path: String,
        fields: Map<String, String>,
        files: Map<String, File>,
    ): JsonObject {
        val raw = execute(
            buildRequest(path).post(RequestBodyFactory.multipart(fields, files)).build(),
        )
        return parseResultBody(raw)
    }

    /**
     * GET 裸响应：不解析 `Result`、不校验 errorCode。
     * 供裸对象/数组接口（`shellscript/topics*`、`javaapi/classes*`）与
     * 文件下载流（`share/download`、`note/export|exportAll`）使用；
     * 返回的 [Response] 由调用方负责 close。
     * HTTP 401 仍会抛 [SessionExpiredException]。
     */
    suspend fun getRaw(path: String, query: Map<String, String> = emptyMap()): Response =
        executeRaw(buildRequest(path, query))

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private suspend fun buildRequest(
        path: String,
        query: Map<String, String> = emptyMap(),
    ): Request {
        val base = resolveBaseUrl()
        val baseUrl = base.toHttpUrlOrNull()
            ?: throw ApiException("CONFIG_ERROR", "服务器地址未配置或非法：\"$base\"")
        val url = baseUrl.newBuilder()
            .addEncodedPathSegments(path.trimStart('/'))
            .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
        return Request.Builder().url(url).build()
    }

    /** Base URL 动态读取：SettingsStore 有值优先，否则用构造时默认值。 */
    private suspend fun resolveBaseUrl(): String =
        settingsStore?.baseUrlFlow?.first().orEmpty().ifEmpty { fallbackBaseUrl }

    /** 执行请求并把响应体读成字符串（Result 解包前）。401 / 非 2xx 在此集中处理。 */
    private suspend fun execute(request: Request): String =
        withContext(Dispatchers.IO) {
            client.newCall(request).await().use { response ->
                if (response.code == HTTP_UNAUTHORIZED) throw SessionExpiredException()
                if (!response.isSuccessful) {
                    throw ApiException(response.code.toString(), "HTTP ${response.code} ${response.message}")
                }
                response.body?.string() ?: ""
            }
        }

    /** 执行请求返回裸 [Response]（getRaw 用），不做 Result / 状态码业务校验（仅 401）。 */
    private suspend fun executeRaw(request: Request): Response =
        withContext(Dispatchers.IO) {
            val response = client.newCall(request).await()
            if (response.code == HTTP_UNAUTHORIZED) {
                response.close()
                throw SessionExpiredException()
            }
            response
        }

    private fun parseResultBody(raw: String): JsonObject {
        val result = ApiResult.parse(raw)
        if (!result.isSuccess) {
            throw ApiException(result.errorCode, result.errorMsg ?: "未知错误")
        }
        return result.body ?: buildJsonObject { }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isCancelled) return
                cont.resumeWithException(
                    ApiException("NETWORK_ERROR", e.message ?: "网络请求失败"),
                )
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

    companion object {
        private const val HEADER_X_REQUESTED_WITH = "X-Requested-With"
        private const val HTTP_UNAUTHORIZED = 401
    }
}
