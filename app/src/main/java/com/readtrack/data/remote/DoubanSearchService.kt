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
 * 说明：旧版 `api.douban.com/v2/book/search` 现已不稳定，
 * 会直接返回 400，导致应用搜索结果始终为空。
 * 这里改为抓取豆瓣搜索页内嵌的 `window.__DATA__` 数据。
 */
@Singleton
class DoubanSearchService @Inject constructor() {

    companion object {
        private const val SEARCH_URL = "https://search.douban.com/book/subject_search"
    }

    /**
     * 搜索书籍
     * @param query 搜索关键词
     * @param cookie 豆瓣Cookie（可选，保留兼容旧设置）
     * @param limit 返回结果数量
     */
    suspend fun searchBooks(query: String, cookie: String, limit: Int = 10): Result<List<BookSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$SEARCH_URL?search_text=$encodedQuery&cat=1001"
                val response = fetchSearchPage(url, cookie)
                val results = parseSearchResponse(response, limit)

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun fetchSearchPage(url: String, cookie: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Referer", "https://book.douban.com/")
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
        val searchDataRegex = Regex(
            pattern = """window\.__DATA__\s*=\s*(\{.*?})\s*;\s*window\.__USER__""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val match = searchDataRegex.find(html)
            ?: throw IllegalStateException("未能解析豆瓣搜索结果")

        val jsonObject = JSONObject(match.groupValues[1])
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