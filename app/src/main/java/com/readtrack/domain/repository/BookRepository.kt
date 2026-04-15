package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
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

    /**
     * 原子操作：删除阅读记录 + 重算书籍进度
     * 从剩余记录中找到最新一条，用其 toPage 重算 currentPage / currentChapter
     */
    suspend fun deleteRecordAndRecalculateBook(record: ReadingRecordEntity)

    /**
     * 更新阅读记录 + 重算书籍进度
     * 更新记录后用新的 toPage 重算 currentPage / currentChapter
     */
    suspend fun updateRecordAndRecalculateBook(record: ReadingRecordEntity)

    /**
     * 更新图书状态，同时写入状态变更记录
     * 状态记录类型：ADDED（添加）、READING（开始阅读）、FINISHED（读完）、DROPPED（抛弃）
     */
    suspend fun updateBookStatus(bookId: Long, newStatus: BookStatus, recordType: RecordType)

    /**
     * 添加图书，同时写入状态变更记录（STATUS_ADDED）
     * 用于记录"添加图书"事件到阅读记录中
     */
    suspend fun insertBookWithStatus(book: BookEntity)
}
