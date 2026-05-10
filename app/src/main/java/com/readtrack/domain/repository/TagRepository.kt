package com.readtrack.domain.repository

import com.readtrack.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<TagEntity>>
    fun getTagById(tagId: Long): Flow<TagEntity?>
    suspend fun getTagByName(name: String): TagEntity?
    suspend fun createTag(name: String, color: Long? = null): Long
    suspend fun deleteTag(tagId: Long)
    fun getTagsForBook(bookId: Long): Flow<List<TagEntity>>
    suspend fun getTagsForBookOnce(bookId: Long): List<TagEntity>
    suspend fun getCrossRefsForBooks(bookIds: List<Long>): List<com.readtrack.data.local.entity.TagCrossRef>
    fun getBookIdsWithTag(tagId: Long): Flow<List<Long>>
    suspend fun addTagToBook(tagId: Long, bookId: Long)
    suspend fun removeTagFromBook(tagId: Long, bookId: Long)
    suspend fun isBookTagged(tagId: Long, bookId: Long): Boolean
    suspend fun setTagsForBook(bookId: Long, tagIds: List<Long>)
}
