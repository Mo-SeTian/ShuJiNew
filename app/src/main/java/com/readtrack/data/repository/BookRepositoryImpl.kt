package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
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

    override suspend fun deleteBook(id: Long) = bookDao.deleteBookById(id)

    override suspend fun insertRecordAndUpdateBook(record: ReadingRecordEntity, book: BookEntity) {
        database.withTransaction {
            readingRecordDao.insertRecord(record)
            bookDao.updateBook(book)
        }
    }

    override suspend fun deleteRecordAndRecalculateBook(record: ReadingRecordEntity) {
        database.withTransaction {
            readingRecordDao.deleteRecord(record)
            recalculateBookProgress(record.bookId)
        }
    }

    override suspend fun updateRecordAndRecalculateBook(record: ReadingRecordEntity) {
        database.withTransaction {
            readingRecordDao.insertRecord(record) // insert with REPLACE (id already set)
            recalculateBookProgress(record.bookId)
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
        // 取最新一条记录的 toPage
        val latest = records.maxByOrNull { it.date } ?: return
        val updatedAt = System.currentTimeMillis()
        if (book.progressType.name == "PAGE") {
            val updated = book.copy(currentPage = latest.toPage, lastReadAt = latest.date, updatedAt = updatedAt)
            bookDao.updateBook(updated)
        } else {
            val updated = book.copy(currentChapter = latest.toPage.toInt(), lastReadAt = latest.date, updatedAt = updatedAt)
            bookDao.updateBook(updated)
        }
    }
}
