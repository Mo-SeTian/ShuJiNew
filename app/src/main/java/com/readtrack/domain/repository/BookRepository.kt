package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
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
    /**
     * 原子操作：插入阅读记录 + 更新书籍进度/时间
     * 解决 addReadingRecord / addChapterProgress 中记录落库但书籍更新失败的数据不一致问题
     */
    suspend fun insertRecordAndUpdateBook(record: ReadingRecordEntity, book: BookEntity)
}
