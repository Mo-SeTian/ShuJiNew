package com.readtrack.data.remote

/**
 * 书籍搜索结果模型
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

/**
 * Open Library API 响应模型
 */
data class OpenLibrarySearchResponse(
    val numFound: Int,
    val start: Int,
    val docs: List<OpenLibraryDoc>
)

data class OpenLibraryDoc(
    val key: String?,
    val title: String?,
    val author_name: List<String>?,
    val publisher: List<String>?,
    val first_publish_year: Int?,
    val isbn: List<String>?,
    val cover_i: Int?,
    val number_of_pages_median: Int?,
    val first_sentence: List<String>?
)

fun OpenLibraryDoc.toBookSearchResult(): BookSearchResult {
    val coverId = cover_i
    val coverUrl = if (coverId != null) {
        "https://covers.openlibrary.org/b/id/$coverId-M.jpg"
    } else {
        isbn?.firstOrNull()?.let { isbn ->
            "https://covers.openlibrary.org/b/isbn/$isbn-M.jpg"
        }
    }
    
    return BookSearchResult(
        key = key ?: "",
        title = title ?: "未知书名",
        author = author_name?.firstOrNull(),
        authors = author_name ?: emptyList(),
        publisher = publisher?.firstOrNull(),
        publishYear = first_publish_year,
        isbn = isbn?.firstOrNull(),
        coverUrl = coverUrl,
        pageCount = number_of_pages_median,
        description = first_sentence?.firstOrNull()
    )
}
