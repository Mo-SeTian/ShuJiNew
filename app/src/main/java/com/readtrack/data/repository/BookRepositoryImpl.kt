package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val readingRecordDao: ReadingRecordDao,
    private val database: ReadTrackDatabase
) : BookRepository {

    override fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    override fun getBooksByStatus(status: BookStatus): Flow<List<BookEntity>> =
        bookDao.getBooksByStatus(status)

    override fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    override fun searchBooks(query: String): Flow<List<BookEntity>> =
        bookDao.searchBooks(query)

    override fun getBookCountByStatus(status: BookStatus): Flow<Int> =
        bookDao.getBookCountByStatus(status)

    override suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    override suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    override suspend fun deleteBook(id: Long) {
        database.withTransaction {
            bookDao.getBookByIdOnce(id) ?: return@withTransaction
            // 级联删除所有阅读记录
            readingRecordDao.deleteRecordsByBookId(id)
            bookDao.deleteBookById(id)
        }
    }

    override suspend fun insertRecordAndUpdateBook(record: ReadingRecordEntity, book: BookEntity) {
        database.withTransaction {
            readingRecordDao.insertRecord(record)
            bookDao.updateBook(book)
        }
    }

    override suspend fun deleteRecordAndRecalculateBook(record: ReadingRecordEntity) {
        database.withTransaction {
            readingRecordDao.deleteRecord(record)
            record.bookId?.let { recalculateBookProgress(it) }
        }
    }

    override suspend fun updateRecordAndRecalculateBook(record: ReadingRecordEntity) {
        database.withTransaction {
            readingRecordDao.insertRecord(record) // insert with REPLACE (id already set)
            record.bookId?.let { recalculateBookProgress(it) }
        }
    }

    private suspend fun recalculateBookProgress(bookId: Long) {
        val book = bookDao.getBookByIdOnce(bookId) ?: return
        val records = readingRecordDao.getRecordsByBookIdOnce(bookId)
        if (records.isEmpty()) {
            // 无记录时重置进度
            val reset = book.copy(currentPage = 0.0, currentChapter = 0, updatedAt = System.currentTimeMillis())
            bookDao.updateBook(reset)
            return
        }
        // 取最新一条 NORMAL 记录的 toPage（状态记录 pagesRead/toPage 为 0，直接取会重置进度）
        val latestNormal = records.filter { it.recordType == RecordType.NORMAL }.maxByOrNull { it.date }
        if (latestNormal == null) {
            // 只有状态记录，无实际阅读进度，保持当前进度不变
            return
        }
        val updatedAt = System.currentTimeMillis()
        if (book.progressType == ProgressType.PAGE) {
            val updated = book.copy(currentPage = latestNormal.toPage, lastReadAt = latestNormal.date, updatedAt = updatedAt)
            bookDao.updateBook(updated)
        } else {
            val updated = book.copy(currentChapter = latestNormal.toPage.toInt(), lastReadAt = latestNormal.date, updatedAt = updatedAt)
            bookDao.updateBook(updated)
        }
    }

    override suspend fun updateBookStatus(bookId: Long, newStatus: BookStatus, recordType: RecordType) {
        database.withTransaction {
            val book = bookDao.getBookByIdOnce(bookId) ?: return@withTransaction
            val snapshot = BookSnapshot.from(book, newStatus)
            // 写入状态变更记录（pagesRead/fromPage/toPage 均为 0）
            val statusRecord = ReadingRecordEntity(
                bookId = bookId,
                bookSnapshot = snapshot,
                pagesRead = 0.0,
                fromPage = 0.0,
                toPage = 0.0,
                date = System.currentTimeMillis(),
                recordType = recordType
            )
            readingRecordDao.insertRecord(statusRecord)
            // 更新图书状态
            val updatedBook = book.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            bookDao.updateBook(updatedBook)
        }
    }

    override suspend fun insertBookWithStatus(book: BookEntity) {
        database.withTransaction {
            val bookId = bookDao.insertBook(book)
            val snapshot = BookSnapshot.from(book, book.status)
            // 先记 STATUS_ADDED，再根据初始状态追加对应记录
            val now = System.currentTimeMillis()
            readingRecordDao.insertRecord(ReadingRecordEntity(
                bookId = bookId,
                bookSnapshot = snapshot,
                pagesRead = 0.0, fromPage = 0.0, toPage = 0.0,
                date = now,
                recordType = RecordType.STATUS_ADDED
            ))
            val statusRecordType = bookStatusToRecordType(book.status)
            if (statusRecordType != null) {
                readingRecordDao.insertRecord(ReadingRecordEntity(
                    bookId = bookId,
                    bookSnapshot = snapshot,
                    pagesRead = 0.0, fromPage = 0.0, toPage = 0.0,
                    date = now,
                    recordType = statusRecordType
                ))
            }
        }
    }
}

private fun bookStatusToRecordType(status: BookStatus): RecordType? = when (status) {
    BookStatus.READING -> RecordType.STATUS_READING
    BookStatus.FINISHED -> RecordType.STATUS_FINISHED
    BookStatus.ON_HOLD, BookStatus.ABANDONED -> RecordType.STATUS_DROPPED
    BookStatus.WANT_TO_READ -> null
}
