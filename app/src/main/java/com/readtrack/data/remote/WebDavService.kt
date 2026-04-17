package com.readtrack.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    suspend fun uploadBackup(config: WebDavConfig, json: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            ensureRemoteDirectory(config)

            val latestUrl = buildUrl(config, LATEST_BACKUP_FILE)
            putJson(config, latestUrl, json)

            val historyUrl = buildUrl(config, buildHistoryFileName())
            putJson(config, historyUrl, json)
            latestUrl
        }
    }

    suspend fun downloadBackup(config: WebDavConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig(config)
            val latestUrl = buildUrl(config, LATEST_BACKUP_FILE)
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

    private fun buildHistoryFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "readtrack_backup_${formatter.format(Date())}.json"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        const val LATEST_BACKUP_FILE = "readtrack_backup_latest.json"
    }
}
