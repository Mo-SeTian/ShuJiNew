package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bing 图片搜索服务
 * 使用 Bing 官方 API（非官方爬取方式，稳定可靠）
 */
@Singleton
class BingImageSearchService @Inject constructor() {

    companion object {
        // 使用 Bing Images search API (免费层每月1000次)
        private const val BING_SEARCH_API = "https://api.bing.microsoft.com/v7.0/images/search"
        // 如果没有 API Key，使用非官方方式抓取 Bing 图片搜索结果页
        private const val BING_FALLBACK_URL = "https://www.bing.com/images/search"
    }

    /**
     * 搜索图片
     * @param query 搜索关键词（通常是书名）
     * @param apiKey Bing API Key（可选，有则优先用官方 API）
     * @param limit 返回结果数量
     */
    suspend fun searchImages(
        query: String,
        apiKey: String = "",
        limit: Int = 20
    ): Result<List<BingImageResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val results = if (apiKey.isNotBlank()) {
                    searchViaApi(query, apiKey, limit)
                } else {
                    searchViaScraping(query, limit)
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 使用 Bing 官方 API 搜索图片
     */
    private fun searchViaApi(query: String, apiKey: String, limit: Int): List<BingImageResult> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BING_SEARCH_API?q=$encodedQuery&count=$limit&safeSearch=Moderate&imageType=Photo"

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36"
            )
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Bing API 请求失败: HTTP $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        return parseApiResponse(response, limit)
    }

    /**
     * 通过抓取 Bing 图片搜索结果页获取图片（无需 API Key）
     * 解析 Bing 搜索结果中的缩略图 URL
     */
    private fun searchViaScraping(query: String, limit: Int): List<BingImageResult> {
        val encodedQuery = URLEncoder.encode(query.trim() + " book cover", "UTF-8")
        val url = "$BING_FALLBACK_URL?q=$encodedQuery"

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            setRequestProperty("Cookie", "MUIDB= dummy") // 简化爬虫检测
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Bing 图片搜索请求失败: HTTP $responseCode")
        }

        val html = connection.inputStream.bufferedReader().use { it.readText() }
        return parseScrapingResponse(html, limit)
    }

    /**
     * 解析 Bing 官方 API 响应
     */
    private fun parseApiResponse(response: String, limit: Int): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()
        val json = JSONObject(response)
        val value = json.optJSONArray("value") ?: return emptyList()

        for (i in 0 until minOf(value.length(), limit)) {
            val item = value.optJSONObject(i) ?: continue
            val thumbnailUrl = item.optString("thumbnailUrl", "")
            val fullUrl = item.optString("contentUrl", "")
            val sourceUrl = item.optString("hostPageUrl", "")
            val title = item.optString("name", "")

            if (thumbnailUrl.isBlank() && fullUrl.isBlank()) continue

            results.add(
                BingImageResult(
                    id = "img_$i",
                    thumbnailUrl = thumbnailUrl,
                    fullUrl = fullUrl,
                    sourceUrl = sourceUrl,
                    title = title,
                    width = item.optInt("width", 0),
                    height = item.optInt("height", 0)
                )
            )
        }

        return results
    }

    /**
     * 解析 Bing 搜索结果页 HTML
     * 提取 JSON 数据中的图片信息
     */
    private fun parseScrapingResponse(html: String, limit: Int): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()

        // 方式1：尝试提取 JSON 数据
        val jsonResults = extractJsonFromHtml(html, limit)
        if (jsonResults.isNotEmpty()) {
            return jsonResults.take(limit)
        }

        // 方式2：尝试解析 BING 图片结果中的缩略图 URL
        // Bing 使用 base64 缩略图，格式为：data-type="image" data-thumbnail-key="..." data-bicep流量...
        // 这种方式不太可靠，所以我们返回空列表
        return results
    }

    /**
     * 从 HTML 中提取 Bing 内嵌的 JSON 数据
     */
    private fun extractJsonFromHtml(html: String, limit: Int): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()

        // 尝试找到 "m" 数组中的图片数据
        // 格式: ,"murl":"https://...","turl":"https://..."... 
        val murlPattern = Regex("\"murl\":\"(https?://[^\"]+)\"")
        val turlPattern = Regex("\"turl\":\"(https?://[^\"]+)\"")
        val titlePattern = Regex("\"mtitle\":\"([^\"]+)\"")

        val murls = murlPattern.findAll(html).map { it.groupValues[1] }.toList()
        val turls = turlPattern.findAll(html).map { it.groupValues[1] }.toList()
        val titles = titlePattern.findAll(html).map { it.groupValues[1] }.toList()

        val count = minOf(murls.size, limit)

        for (i in 0 until count) {
            val fullUrl = murls.getOrElse(i) { "" }
            val thumbnailUrl = turls.getOrElse(i) { fullUrl }
            val title = titles.getOrElse(i) { "" }

            if (fullUrl.isBlank()) continue

            results.add(
                BingImageResult(
                    id = "img_$i",
                    thumbnailUrl = thumbnailUrl,
                    fullUrl = fullUrl,
                    sourceUrl = "",
                    title = title,
                    width = 0,
                    height = 0
                )
            )
        }

        return results
    }
}

/**
 * Bing 图片搜索结果
 */
data class BingImageResult(
    val id: String,
    val thumbnailUrl: String,  // 缩略图 URL
    val fullUrl: String,       // 高清大图 URL
    val sourceUrl: String,     // 来源页面 URL
    val title: String,         // 图片标题/描述
    val width: Int,
    val height: Int
) {
    /**
     * 获取用于加载的 URL（优先用缩略图，缩略图为空时用大图）
     */
    fun getLoadUrl(): String = thumbnailUrl.ifBlank { fullUrl }

    /**
     * 判断是否为可用的图片 URL
     */
    fun isValid(): Boolean = thumbnailUrl.isNotBlank() || fullUrl.isNotBlank()
}
