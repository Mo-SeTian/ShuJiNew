package com.readtrack.data.local.dao

import androidx.room.*
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    fun getBooksByStatus(status: BookStatus): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdOnce(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    fun getBookCountByStatus(status: BookStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM books")
    fun getTotalBookCount(): Flow<Int>

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    /** 统计：查询所有有评分的书籍，按评分降序 */
    @Query("SELECT * FROM books WHERE rating IS NOT NULL ORDER BY rating DESC")
    fun getBooksByRating(): Flow<List<BookEntity>>
}
