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
}
