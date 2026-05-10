package com.readtrack.data.repository

import com.readtrack.data.local.dao.TagDao
import com.readtrack.data.local.entity.TagCrossRef
import com.readtrack.data.local.entity.TagEntity
import com.readtrack.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    override fun getTagById(tagId: Long): Flow<TagEntity?> = tagDao.getTagById(tagId)

    override suspend fun getTagByName(name: String): TagEntity? =
        tagDao.getTagByName(name.trim())

    override suspend fun createTag(name: String, color: Long?): Long {
        val trimmed = name.trim()
        tagDao.getTagByName(trimmed)?.let { return it.id }
        return tagDao.insertTag(TagEntity(name = trimmed, color = color))
    }

    override suspend fun deleteTag(tagId: Long) {
        tagDao.deleteTagById(tagId)
    }

    override fun getTagsForBook(bookId: Long): Flow<List<TagEntity>> =
        tagDao.getTagsForBook(bookId)

    override suspend fun getTagsForBookOnce(bookId: Long): List<TagEntity> =
        tagDao.getTagsForBookOnce(bookId)

    override suspend fun getCrossRefsForBooks(bookIds: List<Long>): List<TagCrossRef> =
        tagDao.getCrossRefsForBooks(bookIds)

    override fun getAllTagCrossRefsFlow(): Flow<List<TagCrossRef>> =
        tagDao.getAllBookTagCrossRefsFlow()

    override fun getBookIdsWithTag(tagId: Long): Flow<List<Long>> =
        tagDao.getBookIdsWithTag(tagId)

    override suspend fun addTagToBook(tagId: Long, bookId: Long) {
        tagDao.addTagToBook(TagCrossRef(tagId = tagId, bookId = bookId))
    }

    override suspend fun removeTagFromBook(tagId: Long, bookId: Long) {
        tagDao.removeTagFromBook(tagId, bookId)
    }

    override suspend fun isBookTagged(tagId: Long, bookId: Long): Boolean =
        tagDao.isBookTagged(tagId, bookId)

    override suspend fun setTagsForBook(bookId: Long, tagIds: List<Long>) {
        tagDao.setTagsForBook(bookId, tagIds)
    }
}
