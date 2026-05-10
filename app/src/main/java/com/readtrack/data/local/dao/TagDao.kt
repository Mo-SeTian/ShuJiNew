package com.readtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.readtrack.data.local.entity.TagCrossRef
import com.readtrack.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // Tag CRUD
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    fun getTagById(tagId: Long): Flow<TagEntity?>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    // Book-Tag relationships
    @Query("SELECT t.* FROM tags t INNER JOIN book_tag_cross_ref ref ON t.id = ref.tagId WHERE ref.bookId = :bookId ORDER BY t.name ASC")
    fun getTagsForBook(bookId: Long): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tags t INNER JOIN book_tag_cross_ref ref ON t.id = ref.tagId WHERE ref.bookId = :bookId ORDER BY t.name ASC")
    suspend fun getTagsForBookOnce(bookId: Long): List<TagEntity>

    @Query("SELECT ref.bookId FROM book_tag_cross_ref ref WHERE ref.tagId = :tagId")
    fun getBookIdsWithTag(tagId: Long): Flow<List<Long>>

    @Query("SELECT * FROM book_tag_cross_ref")
    suspend fun getAllBookTagCrossrefs(): List<TagCrossRef>

    @Query("SELECT * FROM book_tag_cross_ref WHERE bookId IN (:bookIds)")
    suspend fun getCrossRefsForBooks(bookIds: List<Long>): List<TagCrossRef>

    @Query("SELECT EXISTS(SELECT 1 FROM book_tag_cross_ref WHERE tagId = :tagId AND bookId = :bookId)")
    suspend fun isBookTagged(tagId: Long, bookId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToBook(crossRef: TagCrossRef)

    @Query("DELETE FROM book_tag_cross_ref WHERE tagId = :tagId AND bookId = :bookId")
    suspend fun removeTagFromBook(tagId: Long, bookId: Long)

    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId")
    suspend fun removeAllTagsFromBook(bookId: Long)

    @androidx.room.Transaction
    suspend fun setTagsForBook(bookId: Long, tagIds: List<Long>) {
        removeAllTagsFromBook(bookId)
        tagIds.forEach { tagId ->
            addTagToBook(TagCrossRef(tagId = tagId, bookId = bookId))
        }
    }
}
