package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListEntity
import kotlinx.coroutines.flow.Flow

interface BookListRepository {
    // 书单本身
    fun getAllBookLists(): Flow<List<BookListEntity>>
    fun getBookListById(id: Long): Flow<BookListEntity?>
    suspend fun getBookListByIdOnce(id: Long): BookListEntity?
    suspend fun createBookList(name: String, description: String? = null): Long
    suspend fun updateBookList(bookList: BookListEntity)
    suspend fun deleteBookList(id: Long)

    // 书单内书籍
    fun getBooksInBookList(bookListId: Long): Flow<List<BookEntity>>
    fun getBookListsForBook(bookId: Long): Flow<List<BookListEntity>>
    suspend fun addBookToList(bookListId: Long, bookId: Long)
    suspend fun removeBookFromList(bookListId: Long, bookId: Long)
    suspend fun isBookInBookList(bookListId: Long, bookId: Long): Boolean
    suspend fun addBooksToList(bookListId: Long, bookIds: List<Long>)
    suspend fun clearBookList(bookListId: Long)
}
