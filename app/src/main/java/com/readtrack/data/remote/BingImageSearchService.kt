package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bing 图片搜索服务
 * 使用 OkHttp 访问 Bing 图片搜索页，解析 HTML 提取图片 URL
 * 支持分页加载（每页 ~35 张，可通过 first 参数翻页）
 */
@Singleton
class BingImageSearchService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val BING_SEARCH_URL = "https://www.bing.com/images/search"
        // User-Agent 列表，随机选用以降低反爬风险
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        )
        private const val PAGE_SIZE = 35  // 每页 ~35 张图片
    }

    /**
     * 搜索图片（第 N 页）
     * @param query 搜索关键词（通常是书名）
     * @param page 页码（0-based），第一页 page=0
     * @param apiKey Bing API Key（暂未实现，保留接口）
     */
    suspend fun searchImages(
        query: String,
        page: Int = 0,
        apiKey: String = ""
    ): Result<List<BingImageResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val results = searchViaScraping(query.trim(), page)
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 通过 OkHttp 抓取 Bing 图片搜索结果页
     * first=1 为第一页，每翻一页 +35
     */
    private fun searchViaScraping(query: String, page: Int): List<BingImageResult> {
        val encodedQuery = URLEncoder.encode(query + " book cover", "UTF-8")
        val first = page * PAGE_SIZE + 1
        val url = "$BING_SEARCH_URL?q=$encodedQuery&first=$first"

        // 随机选一个 User-Agent
        val userAgent = USER_AGENTS.random()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .header("Referer", "https://www.bing.com/")
            .header("DNT", "1")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .build()

        val response = okHttpClient.newCall(request).execute()

        val responseCode = response.code
        if (responseCode !in 200..299) {
            throw IllegalStateException("Bing 图片搜索请求失败: HTTP $responseCode")
        }

        val html = response.body?.string().orEmpty()
        return parseMimgTags(html)
    }

    /**
     * 解析 HTML 中的 <img class="mimg"> 标签
     */
    private fun parseMimgTags(html: String): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()

        // 正则匹配所有包含 mimg class 的 <img> 标签
        val mimgPattern = Regex("<img[^>]+class=\"[^\"]*mimg[^\"]*\"[^>]*>", RegexOption.IGNORE_CASE)

        var index = 0
        for (tag in mimgPattern.findAll(html)) {
            val tagStr = tag.value

            // 提取 src 属性（优先），其次 data-src（懒加载图）
            val url = extractAttr(tagStr, "src") ?: extractAttr(tagStr, "data-src")
            if (url.isNullOrBlank()) continue

            // 解码 HTML 实体
            val cleanUrl = url.replace("&amp;", "&")

            // 提取 alt 属性作为标题
            val alt = extractAttr(tagStr, "alt")
                ?.replace(" 的图像结果", "")
                ?.replace(" image result", "")
                ?.trim()
                ?: ""

            // 提取图片 ID 并构造较大尺寸的 URL
            val largeUrl = deriveLargeUrl(cleanUrl)

            if (cleanUrl.contains("/th/id/")) {
                results.add(
                    BingImageResult(
                        id = "img_$index",
                        thumbnailUrl = cleanUrl,
                        fullUrl = largeUrl,
                        sourceUrl = "",
                        title = alt,
                        width = 0,
                        height = 0
                    )
                )
                index++
            }
        }

        return results
    }

    /**
     * 从 HTML 标签字符串中提取指定属性的值
     */
    private fun extractAttr(tag: String, attrName: String): String? {
        val regexStr = "(?i)\\s+" + attrName + "=\"([^\"]*)\""
        val pattern = Regex(regexStr)
        val match = pattern.find(tag)
        return match?.groupValues?.get(1)
    }

    /**
     * 根据缩略图 URL 构造更大尺寸的 URL
     */
    private fun deriveLargeUrl(thumbnailUrl: String): String {
        val idPattern = Regex("/th/id/([^?&]+)")
        val idMatch = idPattern.find(thumbnailUrl) ?: return thumbnailUrl
        val id = idMatch.groupValues[1]
        return "https://th.bing.com/th/id/$id?w=500&c=7&r=0&o=5&pid=1.7"
    }
}

/**
 * Bing 图片搜索结果
 */
data class BingImageResult(
    val id: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val sourceUrl: String,
    val title: String,
    val width: Int,
    val height: Int
) {
    fun getLoadUrl(): String = fullUrl.ifBlank { thumbnailUrl }
    fun isValid(): Boolean = thumbnailUrl.isNotBlank()
}
