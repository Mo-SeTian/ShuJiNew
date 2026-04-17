package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bing 图片搜索服务
 * 通过解析 Bing 图片搜索结果页 HTML 提取图片 URL
 */
@Singleton
class BingImageSearchService @Inject constructor() {

    companion object {
        private const val BING_SEARCH_URL = "https://www.bing.com/images/search"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    /**
     * 搜索图片
     * @param query 搜索关键词（通常是书名）
     * @param apiKey Bing API Key（暂未实现，保留接口）
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

                val results = searchViaScraping(query.trim(), limit)
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 通过抓取 Bing 图片搜索结果页获取图片
     */
    private fun searchViaScraping(query: String, limit: Int): List<BingImageResult> {
        val encodedQuery = URLEncoder.encode(query + " book cover", "UTF-8")
        val url = "$BING_SEARCH_URL?q=$encodedQuery"

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            setRequestProperty("Referer", "https://www.bing.com/")
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Bing 图片搜索请求失败: HTTP $responseCode")
        }

        val html = connection.inputStream.bufferedReader().use { it.readText() }
        return parseMimgTags(html, limit)
    }

    /**
     * 解析 HTML 中的 <img class="mimg"> 标签
     * Bing 图片搜索结果页中，所有缩略图都带有 "mimg" class
     */
    private fun parseMimgTags(html: String, limit: Int): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()

        // 正则匹配所有包含 mimg class 的 <img> 标签
        val mimgPattern = Regex("""<img[^>]+class="[^"]*mimg[^"]*"[^>]*>""", RegexOption.IGNORE_CASE)

        var index = 0
        for (tag in mimgPattern.findAll(html)) {
            if (results.size >= limit) break

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
     * 使用原始字符串避免正则转义问题
     */
    private fun extractAttr(tag: String, attrName: String): String? {
        // 构造模式：attrName="..."，忽略大小写
        val regexStr = """(?i)\s+""" + attrName + """="([^"]*)" """
        val pattern = Regex(regexStr.trimEnd())
        val match = pattern.find(tag)
        return match?.groupValues?.get(1)
    }

    /**
     * 根据缩略图 URL 构造更大尺寸的 URL
     */
    private fun deriveLargeUrl(thumbnailUrl: String): String {
        val idPattern = Regex("""/th/id/([^?&]+)""")
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
