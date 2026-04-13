package com.readtrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书籍搜索服务
 * 使用 Open Library API: https://openlibrary.org/search.json
 */
@Singleton
class BookSearchService @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://openlibrary.org/search.json"
        private const val USER_AGENT = "ReadTrack/1.0 (Android App)"
    }

    /**
     * 搜索书籍
     * @param query 搜索关键词（书名、作者等）
     * @param limit 返回结果数量限制
     */
    suspend fun searchBooks(query: String, limit: Int = 10): Result<List<BookSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }

                val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$BASE_URL?q=$encodedQuery&limit=$limit&fields=key,title,author_name,publisher,first_publish_year,isbn,cover_i,number_of_pages_median,first_sentence"

                val connection = java.net.URL(url).openConnection()
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 30000  // 30秒连接超时
                connection.readTimeout = 30000      // 30秒读取超时

                val responseCode = (connection as java.net.HttpURLConnection).responseCode
                if (responseCode != 200) {
                    return@withContext Result.failure(Exception("服务器返回错误: $responseCode"))
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
     * 解析 Open Library API 响应
     */
    private fun parseSearchResponse(json: String): List<BookSearchResult> {
        val results = mutableListOf<BookSearchResult>()
        
        try {
            val jsonObject = JSONObject(json)
            val docs = jsonObject.optJSONArray("docs") ?: return results
            
            for (i in 0 until docs.length()) {
                val doc = docs.getJSONObject(i)
                val result = parseDoc(doc)
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
     * 解析单个文档
     */
    private fun parseDoc(doc: JSONObject): BookSearchResult {
        val key = doc.optString("key", "")
        val title = doc.optString("title", "未知书名")
        
        val authorArray = doc.optJSONArray("author_name")
        val authors = mutableListOf<String>()
        if (authorArray != null) {
            for (i in 0 until authorArray.length()) {
                authors.add(authorArray.getString(i))
            }
        }
        
        val publisherArray = doc.optJSONArray("publisher")
        val publisher = if (publisherArray != null && publisherArray.length() > 0) {
            publisherArray.getString(0)
        } else null
        
        val isbnArray = doc.optJSONArray("isbn")
        val isbn = if (isbnArray != null && isbnArray.length() > 0) {
            isbnArray.getString(0)
        } else null
        
        val coverI = if (doc.has("cover_i")) doc.getInt("cover_i") else null
        val coverUrl = if (coverI != null) {
            "https://covers.openlibrary.org/b/id/$coverI-M.jpg"
        } else if (isbn != null) {
            "https://covers.openlibrary.org/b/isbn/$isbn-M.jpg"
        } else null
        
        val firstSentenceArray = doc.optJSONArray("first_sentence")
        val description = if (firstSentenceArray != null && firstSentenceArray.length() > 0) {
            firstSentenceArray.getString(0)
        } else null
        
        return BookSearchResult(
            key = key,
            title = title,
            author = authors.firstOrNull(),
            authors = authors,
            publisher = publisher,
            publishYear = if (doc.has("first_publish_year")) doc.getInt("first_publish_year") else null,
            isbn = isbn,
            coverUrl = coverUrl,
            pageCount = if (doc.has("number_of_pages_median")) doc.getInt("number_of_pages_median") else null,
            description = description
        )
    }
}
