package com.cyz.wxyd.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder

/**
 * 文件下载（计划 Task 12，Review Focus #5）。
 *
 * 走 [ApiClient.getRaw]（GET 裸响应）：与 `note/export|exportAll`、`share/download` 一致 ——
 * 这三个接口成功时直接回文件流，失败时回 JSON（`share/download` 用 **HTTP 404** 表示不存在）。
 *
 * 处理规则：
 * - HTTP 404 → 「文件不存在或无权访问」（不抛裸 IO 异常、不生成空文件）；
 * - 其他非 2xx → 「下载失败（HTTP xxx）」；
 * - 先写临时文件再 rename，避免半截文件混进 [Environment.DIRECTORY_DOWNLOADS]；
 * - 文件名优先取 `Content-Disposition`，其次用调用方给的默认名。
 */
class FileDownloader(private val client: ApiClient, private val context: Context) {

    /**
     * @param path   `/share/download` 等
     * @param query  查询参数（如 `name=`）
     * @param target 目标目录（默认 `Downloads/`，即应用专属目录，免存储权限）
     * @param defaultFileName 服务端没给文件名时使用
     */
    suspend fun download(
        path: String,
        query: Map<String, String> = emptyMap(),
        target: File = defaultDir(),
        defaultFileName: String = "download.bin",
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            client.getRaw(path, query).use { response ->
                if (response.code == 404) {
                    throw ApiException("404", "文件不存在或无权访问")
                }
                if (!response.isSuccessful) {
                    throw ApiException(response.code.toString(), "下载失败（HTTP ${response.code}）")
                }
                val body = response.body ?: throw ApiException("EMPTY", "下载失败（响应为空）")
                val name = sanitizeFileName(dispositionName(response.header("Content-Disposition")) ?: defaultFileName)
                target.mkdirs()
                val out = File(target, name)
                val tmp = File(target, ".$name.part")
                try {
                    body.byteStream().use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                    if (out.exists()) out.delete()
                    if (!tmp.renameTo(out)) throw ApiException("IO", "保存文件失败")
                    out
                } catch (e: Exception) {
                    tmp.delete()
                    throw e
                }
            }
        }
    }

    /** 用系统查看器打开已下载文件（FileProvider）。 */
    fun open(file: File): Result<Unit> = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = context.contentResolver.getType(uri) ?: guessMime(file.name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        /** 应用专属 Downloads 目录（无需存储权限；用户可在系统「文件」里看到）。 */
        fun defaultDir(): File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .resolve("xiaozhe")

        /** 从 `Content-Disposition` 取 `filename*=UTF-8''xxx` 或 `filename="xxx"`。 */
        internal fun dispositionName(header: String?): String? {
            if (header.isNullOrBlank()) return null
            val star = Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(header)
            if (star != null) {
                return runCatching { URLDecoder.decode(star.groupValues[1].trim(), "UTF-8") }.getOrNull()
            }
            val plain = Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(header)
            return plain?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        }

        /** 只保留文件名本身，拒绝路径穿越。 */
        internal fun sanitizeFileName(raw: String): String {
            val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
            val safe = if (base.isEmpty() || base == "." || base == ".." || base.contains("..")) "download.bin" else base
            return safe.take(120)
        }

        private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "md", "txt", "log" -> "text/plain"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }
}
