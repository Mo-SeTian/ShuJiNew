package com.readtrack.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.readtrack.util.buildBackupTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val remotePath: String
) {
    fun isValid(): Boolean =
        serverUrl.isNotBlank() &&
            username.isNotBlank() &&
            password.isNotBlank() &&
            remotePath.isNotBlank()
}

@Singleton
class WebDavService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun testConnection(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            ensureRemoteDirectory(config)
        }
    }

    // ───── ZIP 上传/下载 ─────

    /**
     * 上传 ZIP 备份文件到 WebDAV（保存 latest + 历史快照）
     * @return 上传的 URL
     */
    suspend fun uploadBackupZip(config: WebDavConfig, zipData: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            ensureRemoteDirectory(config)

            val latestUrl = buildUrl(config, LATEST_ZIP_FILE)
            putBinary(config, latestUrl, zipData)

            val historyUrl = buildUrl(config, buildZipFileName())
            putBinary(config, historyUrl, zipData)
            enforceRetention(config, ".zip")
            latestUrl
        }
    }

    /**
     * 从 WebDAV 下载 ZIP 备份文件
     * @return ZIP 文件字节数组
     */
    suspend fun downloadBackupZip(config: WebDavConfig, fileName: String? = null): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            val targetFile = fileName ?: LATEST_ZIP_FILE
            val url = buildUrl(config, targetFile)
            val request = requestBuilder(config, url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败（HTTP ${response.code}）")
                }
                response.body?.bytes() ?: throw IllegalStateException("远端备份为空")
            }
        }
    }

    // ───── 旧 JSON 上传/下载（保留兼容，逐步移除以支持旧版备份） ─────

    suspend fun uploadBackup(config: WebDavConfig, json: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            ensureRemoteDirectory(config)

            val latestUrl = buildUrl(config, LATEST_BACKUP_FILE)
            putJson(config, latestUrl, json)

            val historyUrl = buildUrl(config, buildHistoryFileName())
            putJson(config, historyUrl, json)
            enforceRetention(config, ".json")
            latestUrl
        }
    }

    /**
     * 删除远端备份文件（用于历史快照清理）。
     */
    suspend fun deleteBackup(config: WebDavConfig, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            val url = buildUrl(config, fileName)
            val request = requestBuilder(config, url).delete().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("删除备份失败（HTTP ${response.code}）")
                }
            }
        }
    }

    /**
     * 历史快照保留策略：同一类型（.zip / .json）只保留最近 [MAX_BACKUP_SNAPSHOTS] 份，
     * 超出部分按最后修改时间从旧到新删除。latest 文件不参与清理。
     */
    private suspend fun enforceRetention(config: WebDavConfig, extension: String) {
        runCatching {
            val files = listBackups(config).getOrNull() ?: return
            val snapshots = files.filter {
                it.fileName != LATEST_ZIP_FILE &&
                    it.fileName != LATEST_BACKUP_FILE &&
                    it.fileName.endsWith(extension)
            }
            if (snapshots.size <= MAX_BACKUP_SNAPSHOTS) return

            snapshots
                .sortedByDescending { it.lastModified }
                .drop(MAX_BACKUP_SNAPSHOTS)
                .forEach { deleteBackup(config, it.fileName) }
        }.onFailure {
            android.util.Log.w("WebDavService", "历史备份清理失败: ${it.message}")
        }
    }

    suspend fun downloadBackup(config: WebDavConfig, fileName: String? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            val targetFile = fileName ?: LATEST_BACKUP_FILE
            val latestUrl = buildUrl(config, targetFile)
            val request = requestBuilder(config, latestUrl).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败（HTTP ${response.code}）")
                }
                response.body?.string().orEmpty().ifBlank {
                    throw IllegalStateException("远端备份为空")
                }
            }
        }
    }

    data class BackupFileInfo(
        val fileName: String,
        val lastModified: Long,
        val size: Long
    )

    suspend fun listBackups(config: WebDavConfig): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            val propfindUrl = buildUrl(config, null)
            val request = requestBuilder(config, propfindUrl)
                .header("Depth", "1")
                .method("PROPFIND", PROPFIND_BODY)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("列出备份失败（HTTP ${response.code}）")
                }
                val body = response.body?.string().orEmpty()
                parseBackupFileList(body)
            }
        }
    }

    private fun parseBackupFileList(xml: String): List<BackupFileInfo> {
        val result = mutableListOf<BackupFileInfo>()
        val fileNameRegex = "<d:href>([^<]*(?:readtrack_backup|backup)[^<]*)</d:href>".toRegex()
        val lastModifiedRegex = "<d:getlastmodified>([^<]+)</d:getlastmodified>".toRegex()
        val sizeRegex = "<d:getcontentlength>([^<]+)</d:getcontentlength>".toRegex()

        val hrefMatches = fileNameRegex.findAll(xml).toList()
        val lastModifiedMatches = lastModifiedRegex.findAll(xml).toList()
        val sizeMatches = sizeRegex.findAll(xml).toList()

        for (i in hrefMatches.indices) {
            val fileName = hrefMatches[i].groupValues[1]
                .substringAfterLast("/")
                .substringAfterLast("%2F")
            // 匹配 .json 和 .zip 文件
            if (!fileName.endsWith(".json") && !fileName.endsWith(".zip")) continue

            val lastModified = lastModifiedMatches.getOrNull(i)?.groupValues?.get(1)
                ?.let { parseHttpDate(it) } ?: 0L
            val size = sizeMatches.getOrNull(i)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            result.add(BackupFileInfo(fileName, lastModified, size))
        }

        return result.sortedByDescending { it.lastModified }
    }

    private fun parseHttpDate(dateStr: String): Long {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
        return try { formatter.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
    }

    private fun validateConfig(config: WebDavConfig) {
        require(config.isValid()) { "请先填写完整的 WebDAV 地址、用户名、密码和远程目录" }
        require(config.serverUrl.toHttpUrlOrNull() != null) { "WebDAV 地址格式无效" }
    }

    private fun ensureRemoteDirectory(config: WebDavConfig) {
        val segments = config.remotePath.trim().trim('/').split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) return

        var currentPath = ""
        segments.forEach { segment ->
            currentPath = if (currentPath.isBlank()) segment else "$currentPath/$segment"
            val url = buildUrl(config.copy(remotePath = currentPath))
            if (collectionExists(config, url)) return@forEach

            val request = requestBuilder(config, url)
                .method("MKCOL", EMPTY_BODY)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code !in listOf(200, 201, 204, 301, 302, 405)) {
                    throw IllegalStateException("创建远程目录失败（HTTP ${response.code}）")
                }
            }
        }
    }

    private fun collectionExists(config: WebDavConfig, url: String): Boolean {
        val request = requestBuilder(config, url)
            .header("Depth", "0")
            .method("PROPFIND", EMPTY_BODY)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            response.code in listOf(200, 207, 301, 302, 405)
        }
    }

    private fun putJson(config: WebDavConfig, url: String, json: String) {
        val request = requestBuilder(config, url)
            .put(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("上传失败（HTTP ${response.code}）")
            }
        }
    }

    private fun putBinary(config: WebDavConfig, url: String, data: ByteArray) {
        val request = requestBuilder(config, url)
            .put(data.toRequestBody(OCTET_STREAM_MEDIA_TYPE))
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("上传失败（HTTP ${response.code}）")
            }
        }
    }

    private fun requestBuilder(config: WebDavConfig, url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Authorization", basicAuth(config.username, config.password))
            .header("Accept", "application/json, text/plain, */*")
    }

    private fun buildUrl(config: WebDavConfig, fileName: String? = null): String {
        val baseUrl = config.serverUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("WebDAV 地址格式无效")
        val builder = baseUrl.newBuilder()
        config.remotePath
            .trim()
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
            .forEach { builder.addPathSegment(it) }
        if (!fileName.isNullOrBlank()) {
            builder.addPathSegment(fileName)
        }
        return builder.build().toString()
    }

    private fun basicAuth(username: String, password: String): String {
        val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        return "Basic $token"
    }

    private fun buildHistoryFileName(): String = "readtrack_backup_${buildBackupTimestamp()}.json"

    private fun buildZipFileName(): String = "readtrack_backup_${buildBackupTimestamp()}.zip"

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        private val PROPFIND_BODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:getlastmodified/>
                    <d:getcontentlength/>
                </d:prop>
            </d:propfind>
        """.trimIndent().toRequestBody("application/xml".toMediaType())
        const val LATEST_BACKUP_FILE = "readtrack_backup_latest.json"
        const val LATEST_ZIP_FILE = "readtrack_backup_latest.zip"
        private const val MAX_BACKUP_SNAPSHOTS = 10
    }
}
