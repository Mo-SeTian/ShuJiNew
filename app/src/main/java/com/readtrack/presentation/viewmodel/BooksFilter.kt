package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus

/** 书籍列表排序方式 */
enum class BookSortOrder(val displayName: String) {
    LAST_READ("最近阅读"),
    UPDATED("最近更新"),
    TITLE("书名"),
    ADDED("添加时间"),
    PROGRESS("阅读进度");

    companion object {
        fun default(): BookSortOrder = LAST_READ
    }
}

internal data class BooksFilterInput(
    val books: List<BookEntity>,
    val status: BookStatus?,
    val query: String,
    val sortOrder: BookSortOrder = BookSortOrder.default()
)

internal fun normalizeSearchQuery(query: String): String =
    query.trim().lowercase().replace("\u3000", " ").replace(Regex("\\s+"), " ")

internal fun filterBooks(input: BooksFilterInput): List<BookEntity> {
    val normalizedQuery = normalizeSearchQuery(input.query)
    val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
    val hasFilter = input.status != null || tokens.isNotEmpty()

    val filtered = if (!hasFilter) {
        input.books
    } else {
        input.books.filter { book ->
            val matchesStatus = input.status == null || book.status == input.status
            val matchesQuery = tokens.isEmpty() || matchesAllTokens(book, tokens)
            matchesStatus && matchesQuery
        }
    }

    return if (hasFilter || input.sortOrder != BookSortOrder.LAST_READ) {
        sortBooks(filtered, input.sortOrder)
    } else {
        filtered
    }
}

private fun sortBooks(books: List<BookEntity>, sortOrder: BookSortOrder): List<BookEntity> {
    return when (sortOrder) {
        BookSortOrder.LAST_READ -> books.sortedByDescending { it.lastReadAt ?: 0L }
        BookSortOrder.UPDATED -> books.sortedByDescending { it.updatedAt }
        BookSortOrder.TITLE -> books.sortedBy { it.title.lowercase() }
        BookSortOrder.ADDED -> books.sortedByDescending { it.createdAt }
        BookSortOrder.PROGRESS -> books.sortedByDescending { book ->
            when {
                (book.totalChapters ?: 0) > 0 -> book.currentChapter.toDouble() / (book.totalChapters ?: 1)
                book.totalPages > 0 -> book.currentPage / book.totalPages
                else -> 0.0
            }
        }
    }
}

private fun matchesAllTokens(book: BookEntity, tokens: List<String>): Boolean {
    val searchableText = buildString {
        append(book.title)
        append(' ')
        append(book.author.orEmpty())
        append(' ')
        append(book.publisher.orEmpty())
    }.lowercase()
    return tokens.all(searchableText::contains)
}
