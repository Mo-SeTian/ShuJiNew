package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 豆瓣图书搜索服务
 */
@Singleton
class DoubanSearchService @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://api.douban.com/v2/book/search"
    }

    /**
     * 搜索书籍
     * @param query 搜索关键词
     * @param cookie 豆瓣Cookie
     * @param limit 返回结果数量
     */
    suspend fun searchBooks(query: String, cookie: String, limit: Int = 10): Result<List<BookSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                if (cookie.isBlank()) {
                    return@withContext Result.failure(Exception("请先在设置中配置豆瓣Cookie"))
                }

                val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$BASE_URL?q=$encodedQuery&count=$limit"

                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("Cookie", cookie)
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = (connection as java.net.HttpURLConnection).responseCode
                if (responseCode != 200) {
                    return@withContext Result.failure(Exception("豆瓣API返回错误: $responseCode"))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val results = parseSearchResponse(response)

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 解析豆瓣API响应
     */
    private fun parseSearchResponse(json: String): List<BookSearchResult> {
        val results = mutableListOf<BookSearchResult>()
        
        try {
            val jsonObject = JSONObject(json)
            val books = jsonObject.optJSONArray("books") ?: return results
            
            for (i in 0 until books.length()) {
                val book = books.getJSONObject(i)
                val result = parseBook(book)
                if (result.title.isNotBlank()) {
                    results.add(result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results
    }

    /**
     * 解析单个书籍
     */
    private fun parseBook(book: JSONObject): BookSearchResult {
        // 解析作者
        val authorArray = book.optJSONArray("author")
        val authors = mutableListOf<String>()
        if (authorArray != null) {
            for (i in 0 until authorArray.length()) {
                authors.add(authorArray.getString(i))
            }
        }
        
        // 解析封面
        val coverUrl = book.optString("image", null)?.takeIf { it.isNotBlank() && it.startsWith("http") }
        
        // 解析出版年份
        val pubDate = book.optString("pubdate", "")
        val publishYear = pubDate.take(4).toIntOrNull()
        
        // 解析页数
        val pages = book.optString("pages", "").replace("[^0-9]".toRegex(), "").toIntOrNull()
        
        // 解析ISBN
        val isbn13 = book.optString("isbn13", "")
        val isbn10 = book.optString("isbn10", "")
        val isbn = if (isbn13.isNotBlank()) isbn13 else if (isbn10.isNotBlank()) isbn10 else null
        
        return BookSearchResult(
            key = book.optString("id", ""),
            title = book.optString("title", "未知书名"),
            author = authors.firstOrNull(),
            authors = authors,
            publisher = book.optString("publisher", null)?.takeIf { it.isNotBlank() },
            publishYear = publishYear,
            isbn = isbn,
            coverUrl = coverUrl,
            pageCount = pages,
            description = book.optString("summary", null)?.take(200)?.takeIf { it.isNotBlank() }
        )
    }
}
