package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus

internal data class BooksFilterInput(
    val books: List<BookEntity>,
    val status: BookStatus?,
    val query: String
)

internal fun normalizeSearchQuery(query: String): String =
    query.trim().lowercase().replace("\u3000", " ").replace(Regex("\\s+"), " ")

internal fun filterBooks(input: BooksFilterInput): List<BookEntity> {
    val normalizedQuery = normalizeSearchQuery(input.query)
    val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }

    if (input.status == null && tokens.isEmpty()) return input.books

    return input.books.filter { book ->
        val matchesStatus = input.status == null || book.status == input.status
        val matchesQuery = tokens.isEmpty() || matchesAllTokens(book, tokens)
        matchesStatus && matchesQuery
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
