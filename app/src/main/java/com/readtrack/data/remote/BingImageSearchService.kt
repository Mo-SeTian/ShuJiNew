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
) : BaseSearchService() {

    companion object {
        private const val BING_SEARCH_URL = "https://www.bing.com/images/search"
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        )
        private const val PAGE_SIZE = 35
    }

    suspend fun searchImages(
        query: String,
        page: Int = 0,
        apiKey: String = ""
    ): Result<List<BingImageResult>> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val results = searchViaScraping(query.trim(), page)
            results.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(it) }
            )
        }
    }

    private fun buildRequest(query: String, page: Int): Request {
        val encodedQuery = URLEncoder.encode(query + " book cover", "UTF-8")
        val first = page * PAGE_SIZE + 1
        val url = "$BING_SEARCH_URL?q=$encodedQuery&first=$first"
        val userAgent = USER_AGENTS.random()

        val builder = Request.Builder().url(url)
        buildCommonHeaders(builder, referer = "https://www.bing.com/", userAgent = userAgent)
        buildBingImageHeaders(builder)
        return builder.build()
    }

    private fun searchViaScraping(query: String, page: Int): Result<List<BingImageResult>> {
        return runCatching {
            val request = buildRequest(query, page)
            val response = okHttpClient.newCall(request).execute()

            val responseCode = response.code
            if (responseCode !in 200..299) {
                throw IllegalStateException("Bing 图片搜索请求失败: HTTP $responseCode")
            }

            val html = response.body?.string().orEmpty()
            parseMimgTags(html)
        }
    }

    private fun parseMimgTags(html: String): List<BingImageResult> {
        val results = mutableListOf<BingImageResult>()
        val mimgPattern = Regex("<img[^>]+class=\"[^\"]*mimg[^\"]*\"[^>]*>", RegexOption.IGNORE_CASE)

        var index = 0
        for (tag in mimgPattern.findAll(html)) {
            val tagStr = tag.value
            val url = extractAttr(tagStr, "src") ?: extractAttr(tagStr, "data-src")
            if (url.isNullOrBlank()) continue

            val cleanUrl = url.replace("&amp;", "&")
            val alt = extractAttr(tagStr, "alt")
                ?.replace(" 的图像结果", "")
                ?.replace(" image result", "")
                ?.trim()
                ?: ""
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

    private fun extractAttr(tag: String, attrName: String): String? {
        val regexStr = "(?i)\\s+" + attrName + "=\"([^\"]*)\""
        val pattern = Regex(regexStr)
        val match = pattern.find(tag)
        return match?.groupValues?.get(1)
    }

    private fun deriveLargeUrl(thumbnailUrl: String): String {
        val idPattern = Regex("/th/id/([^?&]+)")
        val idMatch = idPattern.find(thumbnailUrl) ?: return thumbnailUrl
        val id = idMatch.groupValues[1]
        return "https://th.bing.com/th/id/$id?w=500&c=7&r=0&o=5&pid=1.7"
    }
}

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