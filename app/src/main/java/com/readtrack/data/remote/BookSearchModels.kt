package com.readtrack.data.remote

/**
 * 书籍搜索结果模型（豆瓣和通用）
 */
data class BookSearchResult(
    val key: String,
    val title: String,
    val author: String?,
    val authors: List<String>,
    val publisher: String?,
    val publishYear: Int?,
    val isbn: String?,
    val coverUrl: String?,
    val pageCount: Int?,
    val description: String?
)
