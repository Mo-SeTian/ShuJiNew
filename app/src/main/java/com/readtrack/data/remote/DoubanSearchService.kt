package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 豆瓣图书搜索服务
 *
 * 旧版 `api.douban.com/v2/book/search` 已不稳定，改用抓取豆瓣搜索页。
 * 搜索页数据藏在 `window.__DATA__` JSON 中。
 * 备选域名：search.douban.com / book.douban.com
 */
@Singleton
class DoubanSearchService @Inject constructor() {

    companion object {
        // 豆瓣搜索页 URL（可改用 www.douban.com 或 m.douban.com 作为备选）
        private val SEARCH_URLS = listOf(
            "https://search.douban.com/book/subject_search",
            "https://www.douban.com/search",
            "https://m.douban.com/search/"
        )
        // 随机选一个 User-Agent
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        )
        // 最大重试次数
        private const val MAX_RETRIES = 2
    }

    /**
     * 搜索书籍
     * @param query 搜索关键词
     * @param cookie 豆瓣Cookie（可选）
     * @param limit 返回结果数量
     */
    suspend fun searchBooks(query: String, cookie: String, limit: Int = 10): Result<List<BookSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                var lastError: Exception? = null

                // 尝试每个搜索 URL
                for ((urlIndex, baseUrl) in SEARCH_URLS.withIndex()) {
                    val isPrimary = urlIndex == 0

                    for (retry in 0..MAX_RETRIES) {
                        try {
                            val url = buildSearchUrl(baseUrl, encodedQuery, urlIndex)
                            val response = fetchSearchPage(url, cookie, retry > 0)
                            val results = parseSearchResponse(response, limit)
                            return@withContext Result.success(results)
                        } catch (e: Exception) {
                            lastError = e
                            // 如果是 403/429/503 错误，换 User-Agent 重试
                            if (e.message?.contains("HTTP 403") == true ||
                                e.message?.contains("HTTP 429") == true ||
                                e.message?.contains("HTTP 503") == true) {
                                // 换个 URL 再试
                                break
                            }
                            // 其他错误直接重试一次
                        }
                    }
                }

                return@withContext Result.failure(
                    lastError ?: IllegalStateException("豆瓣搜索请求失败（所有域名均失败）")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 根据不同域名构造搜索 URL
     */
    private fun buildSearchUrl(baseUrl: String, encodedQuery: String, urlIndex: Int): String {
        return when (urlIndex) {
            0 -> "$baseUrl?search_text=$encodedQuery&cat=1001"        // search.douban.com
            1 -> "$baseUrl?header=book&q=$encodedQuery"              // www.douban.com
            2 -> "$baseUrl?query=$encodedQuery&type=book"             // m.douban.com
            else -> "$baseUrl?search_text=$encodedQuery&cat=1001"
        }
    }

    private fun fetchSearchPage(url: String, cookie: String, isRetry: Boolean): String {
        val userAgent = if (isRetry) USER_AGENTS.random() else USER_AGENTS.first()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            setRequestProperty("Referer", "https://book.douban.com/")
            setRequestProperty("DNT", "1")
            setRequestProperty("Upgrade-Insecure-Requests", "1")
            setRequestProperty("Sec-Fetch-Dest", "document")
            setRequestProperty("Sec-Fetch-Mode", "navigate")
            setRequestProperty("Sec-Fetch-Site", "none")
            setRequestProperty("Sec-Fetch-User", "?1")
            if (cookie.isNotBlank()) {
                setRequestProperty("Cookie", cookie)
            }
            connectTimeout = 15000
            readTimeout = 15000
            instanceFollowRedirects = true
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException(
                if (errorBody.isNotBlank()) {
                    "豆瓣搜索页请求失败: HTTP $responseCode"
                } else {
                    "豆瓣搜索页请求失败: HTTP $responseCode"
                }
            )
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * 解析豆瓣搜索页中的 window.__DATA__ 数据
     */
    private fun parseSearchResponse(html: String, limit: Int): List<BookSearchResult> {
        val jsonText = extractSearchDataJson(html)
        val jsonObject = JSONObject(jsonText)
        val items = jsonObject.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<BookSearchResult>()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            if (item.optString("tpl_name") != "search_subject") continue

            val result = parseBook(item)
            if (result.title.isBlank()) continue

            results += result
            if (results.size >= limit) break
        }

        return results
    }

    private fun extractSearchDataJson(html: String): String {
        val startMarker = "window.__DATA__ ="
        val endMarker = "window.__USER__"

        val startIndex = html.indexOf(startMarker)
        if (startIndex == -1) {
            throw IllegalStateException("未找到豆瓣搜索数据起始标记")
        }

        val endIndex = html.indexOf(endMarker, startIndex)
        if (endIndex == -1) {
            throw IllegalStateException("未找到豆瓣搜索数据结束标记")
        }

        val scriptSegment = html.substring(startIndex + startMarker.length, endIndex)
        val jsonText = scriptSegment.substringBeforeLast(';').trim()

        if (!jsonText.startsWith("{")) {
            throw IllegalStateException("豆瓣搜索数据格式异常")
        }

        return jsonText
    }

    /**
     * 解析单个搜索结果
     */
    private fun parseBook(book: JSONObject): BookSearchResult {
        val abstractParts = book.optString("abstract")
            .split("/")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val author = abstractParts.getOrNull(0)
        val publisher = abstractParts.getOrNull(1)
        val publishYear = abstractParts
            .firstOrNull { it.matches(Regex("\\d{4}([-.].*)?")) }
            ?.take(4)
            ?.toIntOrNull()

        val title = book.optString("title", "未知书名")
            .replace(Regex("\\s+"), " ")
            .trim()

        val coverUrl = book.optString("cover_url")
            .takeIf { it.isNotBlank() && it.startsWith("http") }

        val subjectUrl = book.optString("url")
        val subjectId = subjectUrl.trimEnd('/').substringAfterLast('/', missingDelimiterValue = "")

        return BookSearchResult(
            key = subjectId.ifBlank { book.opt("id")?.toString().orEmpty() },
            title = title,
            author = author,
            authors = listOfNotNull(author),
            publisher = publisher,
            publishYear = publishYear,
            isbn = null,
            coverUrl = coverUrl,
            pageCount = null,
            description = null
        )
    }
}
