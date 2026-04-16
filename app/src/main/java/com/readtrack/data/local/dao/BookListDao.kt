package com.readtrack.data.local.dao

import androidx.room.*
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书单数据访问对象
 */
@Dao
interface BookListDao {
    // ========== 书单本身 ==========

    @Query("SELECT * FROM book_lists ORDER BY updatedAt DESC")
    fun getAllBookLists(): Flow<List<BookListEntity>>

    @Query("SELECT * FROM book_lists WHERE id = :id")
    fun getBookListById(id: Long): Flow<BookListEntity?>

    @Query("SELECT * FROM book_lists WHERE id = :id")
    suspend fun getBookListByIdOnce(id: Long): BookListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookList(bookList: BookListEntity): Long

    @Update
    suspend fun updateBookList(bookList: BookListEntity)

    @Delete
    suspend fun deleteBookList(bookList: BookListEntity)

    @Query("DELETE FROM book_lists WHERE id = :id")
    suspend fun deleteBookListById(id: Long)

    @Query("DELETE FROM book_lists")
    suspend fun deleteAllBookLists()

    // ========== 书单内书籍 ==========

    /**
     * 获取某书单内的所有书籍（按加入时间倒序）
     */
    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_list_cross_ref ref ON b.id = ref.bookId
        WHERE ref.bookListId = :bookListId
        ORDER BY ref.addedAt DESC
    """)
    fun getBooksInBookList(bookListId: Long): Flow<List<BookEntity>>

    /**
     * 获取某书籍所属的所有书单
     */
    @Query("""
        SELECT bl.* FROM book_lists bl
        INNER JOIN book_list_cross_ref ref ON bl.id = ref.bookListId
        WHERE ref.bookId = :bookId
        ORDER BY bl.updatedAt DESC
    """)
    fun getBookListsForBook(bookId: Long): Flow<List<BookListEntity>>

    /**
     * 某书籍是否已在某书单中
     */
    @Query("SELECT EXISTS(SELECT 1 FROM book_list_cross_ref WHERE bookListId = :bookListId AND bookId = :bookId)")
    suspend fun isBookInBookList(bookListId: Long, bookId: Long): Boolean

    /**
     * 将书籍添加到书单
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToList(crossRef: BookListCrossRef)

    /**
     * 将书籍从书单移除
     */
    @Query("DELETE FROM book_list_cross_ref WHERE bookListId = :bookListId AND bookId = :bookId")
    suspend fun removeBookFromList(bookListId: Long, bookId: Long)

    /**
     * 更新书单内的书籍数量
     */
    @Query("""
        UPDATE book_lists
        SET bookCount = (
            SELECT COUNT(*) FROM book_list_cross_ref WHERE bookListId = :bookListId
        ), updatedAt = :updatedAt
        WHERE id = :bookListId
    """)
    suspend fun updateBookCount(bookListId: Long, updatedAt: Long = System.currentTimeMillis())

    // ========== 批量操作 ==========

    /**
     * 批量添加书籍到书单
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBooksToList(crossRefs: List<BookListCrossRef>)

    /**
     * 清空书单内所有书籍（但保留书单）
     */
    @Query("DELETE FROM book_list_cross_ref WHERE bookListId = :bookListId")
    suspend fun clearBookList(bookListId: Long)
}
