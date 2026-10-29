package com.cyz.wxyd.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * 三类请求体封装（spec 约束 4）。
 *
 * `postBody` / `postDirect` / `postMultipart` 是业务层构造请求体的**唯一入口**，
 * 业务层不得自行拼 JSON body —— 契约不规则，集中处理避免散落 bug（决策 D5）。
 *
 * ## 按接口的请求体格式表（来自 H5 源码核对，spec 附录）
 *
 * | path | 格式 | 封装方法 |
 * |---|---|---|
 * | `POST /api/login` | 双层 `{body:{username,password}}` | postBody |
 * | `POST /api/user/updateTheme` | 双层 `{body:{theme}}` | postBody |
 * | `POST /api/register/email/send` | 双层 `{body:{email,ticket}}` | postBody |
 * | `POST /api/register/email/verify` | 双层 `{body:{email,code}}` | postBody |
 * | `POST /api/register/email/complete` | 双层 `{body:{registerToken,password,username?}}` | postBody |
 * | `POST /api/password/reset/send` | 双层 `{body:{email,ticket}}` | postBody |
 * | `POST /api/password/reset/verify` | 双层 `{body:{email,code}}` | postBody |
 * | `POST /api/password/reset/complete` | 双层 `{body:{resetToken,password}}` | postBody |
 * | `POST /api/captcha/verify` | 双层 `{body:{captchaId,x}}` | postBody |
 * | welfare 雷达 `POST /api/radar/*` | 双层 `{body:{...}}` | postBody |
 * | dayinfo 订阅 `POST /api/dayinfo/subscribe/*` | 双层 `{body:{}}` | postBody |
 * | gateway 密钥 `POST /api/gateway/keys/{add,toggle,delete,reveal,model}` | 双层 `{body:{}}` | postBody |
 * | 笔记 `POST /api/note/{save,delete,saveFromSource,tags/rename}` | 双层 `{body:{...}}` | postBody |
 * | 分享删除 `POST /api/share/delete` | 双层 `{body:{...}}` | postBody |
 * | 机器人 `POST /api/robot/{add,update,delete}` | 直接字段 JSON | postDirect |
 * | Java 测试 `POST /api/javaapi/classes/{name}/test` | 直接字段 `{testName,code}` | postDirect |
 * | 机器人发消息 `POST /api/robot/send` | multipart/form-data | postMultipart |
 * | 分享上传 `POST /api/share/upload` | multipart/form-data（超管） | postMultipart |
 *
 * ### 不经本工厂的接口
 * - `GET` 接口（无请求体）：`GET /api/welfare/list`、`/welfare/detail`、`/dayinfo/list|latest|detail`、
 *   `/gateway/keys`、`/gateway/models`、`/robot/list`、`/note/*`（GET 组）、`/share/list`、
 *   `/menu/list?platform=h5`、`/user/info`、`captcha/slider`（GET 滑块图）。
 * - 文件下载流（非 JSON）：`share/download`、`note/export`、`note/exportAll` → `ApiClient.getRaw`。
 * - 裸对象/数组响应（不包 Result）：`shellscript/topics*`、`javaapi/classes*` → `ApiClient.getRaw`。
 *
 * ### 备注
 * - 双层格式的请求体原文形如 `{"body":{"username":"x","password":"y"}}`。
 * - 密码以明文随 `{body:{}}` 发送（与 H5 行为一致，spec 约束 10）；SM4 不在客户端处理。
 */
object RequestBodyFactory {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /** 双层 `{"body": {...}}` —— 多数业务 POST（见上表）。 */
    fun body(bodyFields: JsonObject): RequestBody =
        buildWrappedBody(bodyFields).toString().toRequestBody(JSON_MEDIA_TYPE)

    private fun buildWrappedBody(bodyFields: JsonObject): JsonObject =
        buildJsonObject {
            put("body", bodyFields)
        }

    /** 直接字段 JSON（不包 body 外层）—— `robot/{add,update,delete}`、`javaapi/.../test` 等。 */
    fun direct(fields: JsonObject): RequestBody =
        fields.toString().toRequestBody(JSON_MEDIA_TYPE)

    /**
     * multipart/form-data —— `robot/send`、`share/upload`。
     *
     * @param fields 普通表单字段（Content-Type: text/plain; charset=UTF-8）
     * @param files  文件字段名 → 本地文件
     */
    fun multipart(fields: Map<String, String>, files: Map<String, File>): RequestBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        fields.forEach { (name, value) ->
            builder.addFormDataPart(name, value)
        }
        files.forEach { (name, file) ->
            builder.addFormDataPart(
                name,
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType()),
            )
        }
        return builder.build()
    }
}
