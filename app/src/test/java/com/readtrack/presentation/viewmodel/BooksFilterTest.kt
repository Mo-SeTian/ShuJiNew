package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BooksFilterTest {

    @Test
    fun normalizeSearchQuery_collapsesWhitespaceAndLowercases() {
        val normalized = normalizeSearchQuery("  三体　 刘慈欣  ")

        assertEquals("三体 刘慈欣", normalized)
    }

    @Test
    fun filterBooks_matchesAcrossTitleAuthorAndPublisherTokens() {
        val books = listOf(
            BookEntity(title = "三体", author = "刘慈欣", publisher = "重庆出版社", status = BookStatus.READING),
            BookEntity(title = "活着", author = "余华", publisher = "作家出版社", status = BookStatus.WANT_TO_READ)
        )

        val result = filterBooks(
            BooksFilterInput(
                books = books,
                status = BookStatus.READING,
                query = "刘慈欣 重庆"
            )
        )

        assertEquals(1, result.size)
        assertEquals("三体", result.first().title)
    }

    @Test
    fun filterBooks_returnsOriginalListWhenNoFilters() {
        val books = listOf(
            BookEntity(title = "三体"),
            BookEntity(title = "活着")
        )

        val result = filterBooks(BooksFilterInput(books = books, status = null, query = "  "))

        assertEquals(books, result)
        assertTrue(result === books)
    }
}
