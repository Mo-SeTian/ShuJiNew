package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<BookEntity>>
    fun getBooksByStatus(status: BookStatus): Flow<List<BookEntity>>
    fun getBookById(id: Long): Flow<BookEntity?>
    fun searchBooks(query: String): Flow<List<BookEntity>>
    fun getBookCountByStatus(status: BookStatus): Flow<Int>
    suspend fun insertBook(book: BookEntity): Long
    suspend fun updateBook(book: BookEntity)
    suspend fun deleteBook(id: Long)
}
