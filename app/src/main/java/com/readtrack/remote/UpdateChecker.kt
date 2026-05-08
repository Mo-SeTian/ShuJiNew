package com.readtrack.remote

import com.readtrack.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateChecker {

    private const val GITHUB_API = "https://api.github.com/repos/Mo-SeTian/ShuJiNew/releases/latest"
    private const val GITEE_API = "https://gitee.com/api/v5/repos/mosetian/ShuJiNew/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdate(
        client: OkHttpClient,
        source: String // "github" or "gitee"
    ): UpdateResult {
        return when (source) {
            "gitee" -> checkGitee(client)
            else -> checkGitHub(client)
        }
    }

    private fun checkGitHub(client: OkHttpClient): UpdateResult {
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw RuntimeException("GitHub API 请求失败: ${response.code}")

        val release = json.decodeFromString(GitHubRelease.serializer(), body)
        val tagVersion = release.tagName.removePrefix("v")
        val hasUpdate = compareVersions(tagVersion, BuildConfig.VERSION_NAME) > 0

        val downloadUrl = release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl
            ?: release.htmlUrl

        return UpdateResult(
            hasUpdate = hasUpdate,
            latestVersion = tagVersion,
            currentVersion = BuildConfig.VERSION_NAME,
            releaseNotes = release.body ?: "",
            downloadUrl = downloadUrl,
            releasePageUrl = release.htmlUrl
        )
    }

    private fun checkGitee(client: OkHttpClient): UpdateResult {
        val request = Request.Builder()
            .url(GITEE_API)
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw RuntimeException("Gitee API 请求失败: ${response.code}")

        val release = json.decodeFromString(GiteeRelease.serializer(), body)
        val tagVersion = release.tagName.removePrefix("v")
        val hasUpdate = compareVersions(tagVersion, BuildConfig.VERSION_NAME) > 0

        val downloadUrl = release.attachFiles.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl
            ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl
            ?: release.htmlUrl

        return UpdateResult(
            hasUpdate = hasUpdate,
            latestVersion = tagVersion,
            currentVersion = BuildConfig.VERSION_NAME,
            releaseNotes = release.body ?: "",
            downloadUrl = downloadUrl,
            releasePageUrl = release.htmlUrl
        )
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
