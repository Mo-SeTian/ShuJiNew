package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.domain.repository.BookListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        refreshCoverIfNeeded(bookListId)
    }

    override suspend fun removeBookFromList(bookListId: Long, bookId: Long) {
        bookListDao.removeBookFromList(bookListId, bookId)
        bookListDao.updateBookCount(bookListId)
        // 如果被移除的是封面来源书籍，重新自动更新封面
        val bookList = bookListDao.getBookListByIdOnce(bookListId)
        if (bookList != null && bookList.coverBookId == bookId) {
            refreshCoverAuto(bookListId)
        }
    }

    override suspend fun isBookInBookList(bookListId: Long, bookId: Long): Boolean =
        bookListDao.isBookInBookList(bookListId, bookId)

    override suspend fun addBooksToList(bookListId: Long, bookIds: List<Long>) {
        val crossRefs = bookIds.map { BookListCrossRef(bookListId, it) }
        bookListDao.addBooksToList(crossRefs)
        bookListDao.updateBookCount(bookListId)
        refreshCoverIfNeeded(bookListId)
    }

    override suspend fun clearBookList(bookListId: Long) {
        bookListDao.clearBookList(bookListId)
        bookListDao.updateBookCount(bookListId)
        // 清空书单后也清除封面
        val bookList = bookListDao.getBookListByIdOnce(bookListId)
        if (bookList != null && bookList.coverBookId != null) {
            bookListDao.updateBookList(
                bookList.copy(coverPath = null, coverBookId = null, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /**
     * 如果书单还没有封面，自动设置为第一本有封面的书的封面
     */
    private suspend fun refreshCoverIfNeeded(bookListId: Long) {
        val bookList = bookListDao.getBookListByIdOnce(bookListId) ?: return
        // 只在用户未自定义封面的情况下自动更新
        if (bookList.coverPath != null) return
        refreshCoverAuto(bookListId)
    }

    /**
     * 自动刷新封面：取书单中第一本有封面的书
     */
    private suspend fun refreshCoverAuto(bookListId: Long) {
        val bookList = bookListDao.getBookListByIdOnce(bookListId) ?: return
        val books = bookListDao.getBooksInBookList(bookListId).first()
        val coverBook = books.firstOrNull { !it.coverPath.isNullOrBlank() }
        if (coverBook != null) {
            bookListDao.updateBookList(
                bookList.copy(
                    coverPath = coverBook.coverPath,
                    coverBookId = coverBook.id,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
