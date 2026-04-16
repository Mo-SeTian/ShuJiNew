package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.domain.repository.BookListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookListRepositoryImpl @Inject constructor(
    private val bookListDao: BookListDao,
    private val bookDao: BookDao
) : BookListRepository {

    override fun getAllBookLists(): Flow<List<BookListEntity>> =
        bookListDao.getAllBookLists()

    override fun getBookListById(id: Long): Flow<BookListEntity?> =
        bookListDao.getBookListById(id)

    override suspend fun getBookListByIdOnce(id: Long): BookListEntity? =
        bookListDao.getBookListByIdOnce(id)

    override suspend fun createBookList(name: String, description: String?): Long {
        val bookList = BookListEntity(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return bookListDao.insertBookList(bookList)
    }

    override suspend fun updateBookList(bookList: BookListEntity) {
        bookListDao.updateBookList(bookList.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteBookList(id: Long) {
        bookListDao.deleteBookListById(id)
    }

    override fun getBooksInBookList(bookListId: Long): Flow<List<BookEntity>> =
        bookListDao.getBooksInBookList(bookListId)

    override fun getBookListsForBook(bookId: Long): Flow<List<BookListEntity>> =
        bookListDao.getBookListsForBook(bookId)

    override suspend fun addBookToList(bookListId: Long, bookId: Long) {
        bookListDao.addBookToList(BookListCrossRef(bookListId, bookId))
        bookListDao.updateBookCount(bookListId)
        // 如果书单还没有封面，取第一本书的封面作为书单封面
        val bookList = bookListDao.getBookListByIdOnce(bookListId)
        if (bookList != null && bookList.coverPath == null) {
            val book = bookDao.getBookByIdOnce(bookId)
            if (book?.coverPath != null) {
                bookListDao.updateBookList(
                    bookList.copy(coverPath = book.coverPath, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    override suspend fun removeBookFromList(bookListId: Long, bookId: Long) {
        bookListDao.removeBookFromList(bookListId, bookId)
        bookListDao.updateBookCount(bookListId)
        // 如果移除后书单空了，清除封面
        val bookList = bookListDao.getBookListByIdOnce(bookListId)
        if (bookList != null) {
            val books = bookListDao.getBooksInBookList(bookListId)
            // 书单封面逻辑在 Flow 收集时更新
        }
    }

    override suspend fun isBookInBookList(bookListId: Long, bookId: Long): Boolean =
        bookListDao.isBookInBookList(bookListId, bookId)

    override suspend fun addBooksToList(bookListId: Long, bookIds: List<Long>) {
        val crossRefs = bookIds.map { BookListCrossRef(bookListId, it) }
        bookListDao.addBooksToList(crossRefs)
        bookListDao.updateBookCount(bookListId)
    }

    override suspend fun clearBookList(bookListId: Long) {
        bookListDao.clearBookList(bookListId)
        bookListDao.updateBookCount(bookListId)
    }
}
