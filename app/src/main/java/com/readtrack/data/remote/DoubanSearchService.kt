package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 豆瓣图书搜索服务
 *
 * 使用 OkHttp（配合 Conscrypt TLS）访问豆瓣搜索页。
 * 搜索页数据藏在 `window.__DATA__` JSON 中。
 */
@Singleton
class DoubanSearchService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val SEARCH_URL = "https://search.douban.com/book/subject_search"
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

                val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$SEARCH_URL?search_text=$encodedQuery&cat=1001"

                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://book.douban.com/")
                    .header("DNT", "1")
                    .header("Upgrade-Insecure-Requests", "1")

                if (cookie.isNotBlank()) {
                    requestBuilder.header("Cookie", cookie)
                }

                val request = requestBuilder.build()
                val response = okHttpClient.newCall(request).execute()

                val responseCode = response.code
                if (responseCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("豆瓣搜索页请求失败: HTTP $responseCode")
                    )
                }

                val html = response.body?.string().orEmpty()
                val results = parseSearchResponse(html, limit)
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
            .firstOrNull { it.matches(Regex("\\d{4}([-.].*)?"))
            }
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
