package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.presentation.viewmodel.ProgressType
import kotlinx.serialization.Serializable

/**
 * 数据备份模型 - 用于导入导出
 */
@Serializable
data class DataBackup(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val books: List<BookExport> = emptyList(),
    val readingRecords: List<ReadingRecordExport> = emptyList()
)

/**
 * 书籍导出模型
 */
@Serializable
data class BookExport(
    val id: Long,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val progressType: String,
    val totalPages: Double,
    val currentPage: Double,
    val totalChapters: Int,
    val currentChapter: Int,
    val coverPath: String? = null,
    val description: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReadAt: Long? = null
) {
    companion object {
        fun fromEntity(book: BookEntity): BookExport = BookExport(
            id = book.id,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            progressType = book.progressType.name,
            totalPages = book.totalPages,
            currentPage = book.currentPage,
            totalChapters = book.totalChapters ?: 0,
            currentChapter = book.currentChapter,
            coverPath = book.coverPath,
            description = book.description,
            status = book.status.name,
            createdAt = book.createdAt,
            updatedAt = book.updatedAt,
            lastReadAt = book.lastReadAt
        )
    }

    fun toEntity(): BookEntity = BookEntity(
        id = 0, // 导入时生成新ID
        title = title,
        author = author,
        publisher = publisher,
        progressType = ProgressType.valueOf(progressType),
        totalPages = totalPages,
        currentPage = currentPage,
        totalChapters = if (totalChapters > 0) totalChapters else null,
        currentChapter = currentChapter,
        coverPath = coverPath,
        description = description,
        status = BookStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,    // 保留原始时间，不覆盖
        lastReadAt = lastReadAt    // 保留原始时间
    )
}

/**
 * 阅读记录导出模型
 */
@Serializable
data class ReadingRecordExport(
    val id: Long,
    val bookId: Long,          // 导出时用外部ID引用
    val bookTitle: String,     // 用于匹配导入后的书籍
    val pagesRead: Double,
    val fromPage: Double,
    val toPage: Double,
    val date: Long,
    val note: String? = null
) {
    companion object {
        fun fromEntity(record: ReadingRecordEntity, bookTitle: String): ReadingRecordExport =
            ReadingRecordExport(
                id = record.id,
                bookId = record.bookId,
                bookTitle = bookTitle,
                pagesRead = record.pagesRead,
                fromPage = record.fromPage,
                toPage = record.toPage,
                date = record.date,
                note = record.note
            )
    }
}

/**
 * 导入结果
 */
data class ImportResult(
    val booksImported: Int,
    val recordsImported: Int,
    val errors: List<String> = emptyList()
)
