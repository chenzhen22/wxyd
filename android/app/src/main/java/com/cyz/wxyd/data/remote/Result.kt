package com.cyz.wxyd.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 统一响应契约（spec 约束 3）：
 * `Result { errorCode, errorMsg, body }`，成功 `errorCode == "000000"`。
 *
 * 注：模型类命名为 [ApiResult] 以避免与 Kotlin 内置 `kotlin.Result` 混淆，
 * 但字段与语义与后端 `Result` 完全一致。
 */
data class ApiResult(
    val errorCode: String,
    val errorMsg: String?,
    val body: JsonObject?,
) {
    val isSuccess: Boolean get() = errorCode == SUCCESS_CODE

    companion object {
        /** 后端统一成功码（spec 约束 3）。 */
        const val SUCCESS_CODE = "000000"

        private val json = Json { ignoreUnknownKeys = true }

        /** 从响应原文解析 [ApiResult]。 */
        fun parse(raw: String): ApiResult = try {
            parse(json.parseToJsonElement(raw).jsonObject)
        } catch (e: Exception) {
            throw ApiException("PARSE_ERROR", "响应格式错误：${e.message}")
        }

        /** 从已解析的 JSON 对象构造 [ApiResult]。 */
        fun parse(obj: JsonObject): ApiResult {
            val errorCode = obj["errorCode"]?.jsonPrimitive?.contentOrNull
                ?: throw ApiException("-1", "响应缺少 errorCode 字段")
            val errorMsg = obj["errorMsg"]?.jsonPrimitive?.contentOrNull
            val body = obj["body"] as? JsonObject
            return ApiResult(errorCode = errorCode, errorMsg = errorMsg, body = body)
        }
    }
}

/** 后端返回业务错误（errorCode != "000000"）时抛出。 */
class ApiException(val errorCode: String, val errorMsg: String) :
    Exception("[$errorCode] $errorMsg")

/** 响应 401：会话过期，需上层清除会话并引导重新登录（spec 约束 1）。 */
class SessionExpiredException : Exception("会话已过期，请重新登录")
